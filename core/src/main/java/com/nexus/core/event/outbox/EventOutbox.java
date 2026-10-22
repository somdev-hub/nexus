package com.nexus.core.event.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * An event that is waiting to be published.
 * <p>
 * The row is written in the <b>same transaction</b> as the business change
 * it describes. The relay then publishes it outside the transaction. This is
 * what makes the event undroppable: if the process dies after the business
 * write but before the publish, the event is still on disk and drains on the
 * next restart.
 * <p>
 * <b>Retry scheduling.</b> {@code nextRetryAt} holds the earliest instant at
 * which the relay should try to publish the row again. It is set to
 * {@code now()} when the row is created, so it is eligible immediately, and
 * is pushed into the future with exponential backoff (1s, 2s, 4s, 8s, ...,
 * capped at 5 minutes) every time a publish fails. The relay only ever picks
 * up rows whose {@code nextRetryAt} has passed, so an idle outbox costs
 * nothing: the relay sleeps until the earliest due time.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_event_outbox", schema = "core")
public class EventOutbox extends com.nexus.core.model.entities.BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    /**
     * The idempotency key of the event.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    /**
     * The kind of event.
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * The serialised event.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    /**
     * Where the row is in its lifecycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    /**
     * How many times the relay has tried to publish it.
     */
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    /**
     * The last failure, if any.
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * When the row was created.
     */
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    /**
     * When the row was published.
     */
    @Column(name = "published_at")
    private Timestamp publishedAt;

    /**
     * When the row gave up.
     */
    @Column(name = "failed_at")
    private Timestamp failedAt;

    /**
     * The earliest instant at which the relay should try to publish the row
     * again. Set to {@code now()} when the row is created, so it is eligible
     * immediately, and pushed into the future with exponential backoff every
     * time a publish fails. Null for published rows.
     */
    @Column(name = "next_retry_at")
    private Timestamp nextRetryAt;

    /**
     * Convenience factory for a new pending row.
     */
    public static EventOutbox pending(String eventType, String payload) {
        EventOutbox row = new EventOutbox();
        row.setEventId(UUID.randomUUID().toString());
        row.setEventType(eventType);
        row.setPayload(payload);
        row.setStatus(OutboxStatus.PENDING);
        row.setAttempts(0);
        Timestamp now = Timestamp.from(java.time.Instant.now());
        row.setCreatedAt(now);
        row.setNextRetryAt(now);
        return row;
    }
}
