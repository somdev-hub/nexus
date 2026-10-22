package com.nexus.core.event.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * The outbox rows.
 * <p>
 * {@link #findPendingForUpdate(int)} is the query the relay uses. It selects
 * rows in {@code PENDING} status and locks them with
 * {@code SELECT ... FOR UPDATE SKIP LOCKED}, so that two instances of the
 * relay cannot pick up the same row.
 */
public interface EventOutboxRepo extends JpaRepository<EventOutbox, Long> {

    /**
     * The next batch of rows that are due to be published, locked so that
     * two relays cannot claim the same row.
     * <p>
     * A row is due when its {@code next_retry_at} is in the past. Rows that
     * failed and are waiting for their backoff to expire are never returned,
     * so a failed publish costs nothing until the retry time has passed.
     */
    @Query(value = "SELECT * FROM core.t_event_outbox WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now) ORDER BY next_retry_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<EventOutbox> findPendingDueForUpdate(@Param("now") java.sql.Timestamp now,
                                             @Param("limit") int limit);

    /**
     * The earliest instant at which a pending row becomes due. Used by the
     * relay to compute how long it may sleep before the next pass.
     */
    @Query("SELECT MIN(e.nextRetryAt) FROM EventOutbox e WHERE e.status = com.nexus.core.event.outbox.OutboxStatus.PENDING")
    java.sql.Timestamp findEarliestPendingRetryAt();

    /**
     * The rows that gave up and are old enough to be retried.
     */
    @Query(value = "SELECT * FROM core.t_event_outbox WHERE status = 'FAILED' AND failed_at IS NOT NULL AND failed_at < :cutoff",
            nativeQuery = true)
    List<EventOutbox> findFailedOlderThan(@Param("cutoff") java.sql.Timestamp cutoff);

    /**
     * The rows that gave up and are older than the retention window.
     */
    @Query(value = "SELECT * FROM core.t_event_outbox WHERE status = 'FAILED' AND failed_at IS NOT NULL AND failed_at < :cutoff",
            nativeQuery = true)
    List<EventOutbox> findFailedCreatedBefore(@Param("cutoff") java.sql.Timestamp cutoff);

    /**
     * Count of rows waiting to be published.
     */
    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.status = com.nexus.core.event.outbox.OutboxStatus.PENDING")
    long countPending();

    /**
     * Count of rows that gave up.
     */
    @Query("SELECT COUNT(e) FROM EventOutbox e WHERE e.status = com.nexus.core.event.outbox.OutboxStatus.FAILED")
    long countFailed();

    /**
     * Mark a row as published.
     */
    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = com.nexus.core.event.outbox.OutboxStatus.PUBLISHED, e.publishedAt = :publishedAt WHERE e.outboxId = :outboxId")
    int markPublished(@Param("outboxId") Long outboxId, @Param("publishedAt") java.sql.Timestamp publishedAt);

    /**
     * Mark a row as failed, with a backoff.
     */
    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = com.nexus.core.event.outbox.OutboxStatus.FAILED, e.attempts = :attempts, e.lastError = :lastError, e.failedAt = :failedAt, e.nextRetryAt = :nextRetryAt WHERE e.outboxId = :outboxId")
    int markFailed(@Param("outboxId") Long outboxId, @Param("attempts") int attempts,
                  @Param("lastError") String lastError, @Param("failedAt") java.sql.Timestamp failedAt,
                  @Param("nextRetryAt") java.sql.Timestamp nextRetryAt);

    /**
     * Re-arm a failed row for another attempt.
     */
    @Modifying
    @Query("UPDATE EventOutbox e SET e.status = com.nexus.core.event.outbox.OutboxStatus.PENDING, e.lastError = NULL, e.failedAt = NULL, e.nextRetryAt = :nextRetryAt WHERE e.outboxId = :outboxId")
    int rearm(@Param("outboxId") Long outboxId, @Param("nextRetryAt") java.sql.Timestamp nextRetryAt);

}
