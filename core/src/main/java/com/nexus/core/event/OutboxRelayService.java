package com.nexus.core.event;

import com.nexus.core.event.outbox.EventOutbox;
import com.nexus.core.event.outbox.EventOutboxRepo;
import com.nexus.core.event.outbox.OutboxStatus;
import com.nexus.core.event.transport.EventTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The outbox relay, with adaptive backoff and a circuit breaker.
 * <p>
 * <b>Why this is not {@code @Scheduled}.</b> A fixed delay would poll the
 * database every second even when the outbox is empty. Instead the relay
 * schedules its own next wake-up from the earliest {@code next_retry_at} of
 * the pending rows, so an idle outbox costs nothing and a busy one is
 * drained as fast as possible.
 * <p>
 * <b>Circuit breaker.</b> Three states:
 * <ul>
 *   <li>{@code CLOSED} — normal. Publishes succeed (or retry with backoff).</li>
 *   <li>{@code OPEN} — Kafka is down. No publish is attempted; every pending
 *       row's {@code next_retry_at} is pushed into the future by the
 *       circuit breaker timeout.</li>
 *   <li>{@code HALF_OPEN} — one probe publish is attempted. Success returns
 *       to {@code CLOSED}; failure re-opens with a doubled timeout.</li>
 * </ul>
 * The transition from {@code OPEN} to {@code HALF_OPEN} is driven by the
 * earliest {@code next_retry_at}: when the oldest pending row becomes due,
 * the relay tries once.
 */
@Service
@Slf4j
public class OutboxRelayService {

    /** Backoff cap: 5 minutes. */
    private static final long MAX_BACKOFF_MS = 5 * 60 * 1000L;
    /** Circuit breaker opens after this many consecutive failures. */
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    /** Initial circuit breaker timeout when it opens. */
    private static final long INITIAL_CIRCUIT_BREAKER_TIMEOUT_MS = 30_000L;
    /** Cap on the circuit breaker timeout. */
    private static final long MAX_CIRCUIT_BREAKER_TIMEOUT_MS = 5 * 60 * 1000L;
    /** When the outbox is empty, sleep this long before checking again. */
    private static final long MAX_IDLE_SLEEP_MS = 30_000L;

    private final EventOutboxRepo outboxRepo;
    private final com.nexus.core.event.transport.EventTransport eventTransport;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private final boolean relayEnabled;
    private final int maxAttempts;
    private final int batchSize;
    private final int failedRetentionDays;
    private final boolean replayEnabled;
    private final int maxReplays;
    private final long cooldownSeconds;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "outbox-relay");
        t.setDaemon(true);
        return t;
    });

    private volatile ScheduledFuture<?> nextRun;
    private final AtomicReference<CircuitBreakerState> circuitState = new AtomicReference<>(CircuitBreakerState.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long circuitBreakerTimeoutMs = INITIAL_CIRCUIT_BREAKER_TIMEOUT_MS;
    private volatile long circuitOpenedAt = 0L;

    public OutboxRelayService(EventOutboxRepo outboxRepo,
                              com.nexus.core.event.transport.EventTransport eventTransport,
                              org.springframework.transaction.support.TransactionTemplate transactionTemplate,
                              @org.springframework.beans.factory.annotation.Value("${outbox.relay.enabled:true}") boolean relayEnabled,
                              @org.springframework.beans.factory.annotation.Value("${outbox.relay.max-attempts:10}") int maxAttempts,
                              @org.springframework.beans.factory.annotation.Value("${outbox.relay.batch-size:100}") int batchSize,
                              @org.springframework.beans.factory.annotation.Value("${outbox.failed-retention.days:30}") int failedRetentionDays,
                              @org.springframework.beans.factory.annotation.Value("${outbox.replay.enabled:true}") boolean replayEnabled,
                              @org.springframework.beans.factory.annotation.Value("${outbox.replay.max-replays:3}") int maxReplays,
                              @org.springframework.beans.factory.annotation.Value("${outbox.replay.cooldown-seconds:60}") long cooldownSeconds) {
        this.outboxRepo = outboxRepo;
        this.eventTransport = eventTransport;
        this.transactionTemplate = transactionTemplate;
        this.relayEnabled = relayEnabled;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
        this.failedRetentionDays = failedRetentionDays;
        this.replayEnabled = replayEnabled;
        this.maxReplays = maxReplays;
        this.cooldownSeconds = cooldownSeconds;
    }

    @PostConstruct
    public void start() {
        if (!relayEnabled) {
            log.info("Outbox relay is DISABLED via outbox.relay.enabled=false — events will be written to the outbox but not published automatically");
            return;
        }
        scheduleNext(0);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private void scheduleNext(long delayMs) {
        long safeDelay = Math.max(0, Math.min(delayMs, MAX_IDLE_SLEEP_MS));
        nextRun = scheduler.schedule(this::runPass, safeDelay, TimeUnit.MILLISECONDS);
    }

    private void runPass() {
        long nextWakeUpMs;
        try {
            nextWakeUpMs = relayPass();
        } catch (Exception e) {
            // Should not happen — relayPass catches its own exceptions — but be
            // safe so the relay never dies silently.
            log.error("Relay pass failed unexpectedly", e);
            nextWakeUpMs = 30_000;
        }
        scheduleNext(nextWakeUpMs);
    }

    /**
     * One pass of the relay. Returns the number of milliseconds until the
     * next pass should run.
     */
    public long relayPass() {
        return transactionTemplate.execute(status -> {
            // 1. Circuit breaker check.
            CircuitBreakerState state = circuitState.get();
        if (state == CircuitBreakerState.OPEN) {
            long elapsed = System.currentTimeMillis() - circuitOpenedAt;
            if (elapsed < circuitBreakerTimeoutMs) {
                // Still open. Push all pending rows' retry times into the
                // future and sleep until the circuit breaker timeout.
                pushPendingRetryTimes(circuitBreakerTimeoutMs - elapsed);
                return circuitBreakerTimeoutMs - elapsed;
            }
            // Timeout expired — try one probe.
            circuitState.set(CircuitBreakerState.HALF_OPEN);
            log.info("Circuit breaker transitioning from OPEN to HALF_OPEN — probing Kafka");
        }

        // 2. Publish due rows.
        java.sql.Timestamp now = java.sql.Timestamp.from(Instant.now());
        List<EventOutbox> due = outboxRepo.findPendingDueForUpdate(now, batchSize);

        boolean anyPublished = false;
        boolean anyFailed = false;
        for (EventOutbox row : due) {
            try {
                eventTransport.publish(rowToEvent(row));
                Timestamp publishedAt = Timestamp.from(Instant.now());
                outboxRepo.markPublished(row.getOutboxId(), publishedAt);
                anyPublished = true;
                log.debug("Published outbox row {} ({})", row.getOutboxId(), row.getEventType());
            } catch (Exception e) {
                int attempts = (row.getAttempts() == null ? 0 : row.getAttempts()) + 1;
                String error = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                long backoffMs = backoffMs(attempts);
                Timestamp nextRetryAt = Timestamp.from(Instant.now().plusMillis(backoffMs));
                if (attempts >= maxAttempts) {
                    Timestamp failedAt = Timestamp.from(Instant.now());
                    outboxRepo.markFailed(row.getOutboxId(), attempts, error, failedAt, nextRetryAt);
                    log.error("Outbox row {} ({}) gave up after {} attempts — it is now FAILED",
                            row.getOutboxId(), row.getEventType(), attempts, e);
                } else {
                    outboxRepo.markFailed(row.getOutboxId(), attempts, error, null, nextRetryAt);
                    log.warn("Outbox row {} ({}) failed on attempt {} — next retry in {}ms",
                            row.getOutboxId(), row.getEventType(), attempts, backoffMs, e);
                }
                anyFailed = true;
                recordFailure(row, e);
            }
        }

        // 3. Update circuit breaker state.
        if (anyFailed && !anyPublished && circuitState.get() == CircuitBreakerState.HALF_OPEN) {
            // Probe failed — re-open with a doubled timeout.
            circuitBreakerTimeoutMs = Math.min(circuitBreakerTimeoutMs * 2, MAX_CIRCUIT_BREAKER_TIMEOUT_MS);
            circuitState.set(CircuitBreakerState.OPEN);
            circuitOpenedAt = System.currentTimeMillis();
            consecutiveFailures.set(0);
            log.warn("Circuit breaker re-opened — Kafka is still down. Next probe in {}ms",
                    circuitBreakerTimeoutMs);
        } else if (anyFailed) {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= CIRCUIT_BREAKER_THRESHOLD && circuitState.get() == CircuitBreakerState.CLOSED) {
                circuitState.set(CircuitBreakerState.OPEN);
                circuitOpenedAt = System.currentTimeMillis();
                log.warn("Circuit breaker tripped after {} consecutive failures — Kafka is assumed down. "
                        + "Next probe in {}ms", failures, circuitBreakerTimeoutMs);
            }
        } else if (anyPublished) {
            consecutiveFailures.set(0);
            if (circuitState.get() == CircuitBreakerState.HALF_OPEN) {
                circuitState.set(CircuitBreakerState.CLOSED);
                circuitBreakerTimeoutMs = INITIAL_CIRCUIT_BREAKER_TIMEOUT_MS;
                log.info("Circuit breaker closed — Kafka is back");
            }
        }

        // 4. Re-arm failed rows that are old enough.
        if (replayEnabled) {
            rearmFailed();
        }

        // 5. Purge expired failed rows.
        purgeFailed();

        // 6. Compute next wake-up time.
        if (anyPublished || anyFailed) {
            return 1000L; // There was work; check again soon.
        }
        // Idle: find the earliest due time.
        Timestamp earliest = outboxRepo.findEarliestPendingRetryAt();
        if (earliest == null) {
            return (long) MAX_IDLE_SLEEP_MS; // Nothing pending — sleep for a while.
        }
        long delayMs = earliest.getTime() - System.currentTimeMillis();
        if (delayMs < 0) {
            delayMs = 0;
        }
        return Math.min(delayMs, MAX_IDLE_SLEEP_MS);
        });
    }

    /**
     * Push all pending rows' retry times into the future so that the circuit
     * breaker's timeout is respected.
     */
    private void pushPendingRetryTimes(long delayMs) {
        try {
            // Find pending rows whose next_retry_at is in the past and push
            // them forward. This is a bulk operation.
            Timestamp newRetryAt = Timestamp.from(Instant.now().plusMillis(delayMs));
            // Re-arm them with the new retry time.
            // We use findPendingDueForUpdate to find them, but we don't
            // publish — just update their next_retry_at.
            List<EventOutbox> due = outboxRepo.findPendingDueForUpdate(Timestamp.from(Instant.now()), batchSize);
            for (EventOutbox row : due) {
                outboxRepo.rearm(row.getOutboxId(), newRetryAt);
            }
        } catch (Exception e) {
            log.warn("Could not push pending retry times forward", e);
        }
    }

    private long backoffMs(int attempts) {
        // 1s, 2s, 4s, 8s, 16s, 32s, 60s, ... capped at 5 minutes.
        long backoff = (long) Math.pow(2, attempts - 1) * 1000L;
        return Math.min(backoff, MAX_BACKOFF_MS);
    }

    private void rearmFailed() {
        Instant cooldownBefore = Instant.now().minusSeconds(cooldownSeconds);
        Timestamp cutoff = Timestamp.from(cooldownBefore);
        List<EventOutbox> failed = outboxRepo.findFailedOlderThan(cutoff);
        for (EventOutbox row : failed) {
            if (row.getAttempts() != null && row.getAttempts() >= maxAttempts) {
                continue;
            }
            outboxRepo.rearm(row.getOutboxId(), Timestamp.from(Instant.now()));
            log.info("Re-armed outbox row {} ({}) for another attempt", row.getOutboxId(), row.getEventType());
        }
    }

    private void purgeFailed() {
        if (failedRetentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(failedRetentionDays, ChronoUnit.DAYS);
        List<EventOutbox> expired = outboxRepo.findFailedCreatedBefore(Timestamp.from(cutoff));
        if (expired.isEmpty()) {
            return;
        }
        for (EventOutbox row : expired) {
            outboxRepo.delete(row);
        }
        log.info("Purged {} expired FAILED outbox rows", expired.size());
    }

    /**
     * Turn an outbox row back into the event it describes.
     * <p>
     * The row stores the serialised event, so the relay deserialises it.
     */
    private com.nexus.core.event.DomainEvent rowToEvent(EventOutbox row) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(row.getPayload(), com.nexus.core.event.DomainEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialise outbox row " + row.getOutboxId(), e);
        }
    }

    /**
     * Record a failure for the relay's own purposes. The relay does not
     * write to the CMS failure table — that is the consumer's job. The relay
     * just records the error on the row.
     */
    private void recordFailure(EventOutbox row, Exception e) {
        // Already recorded via markFailed. Nothing more to do here.
    }

    private enum CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
}
