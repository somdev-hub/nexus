package com.nexus.cms.notification.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;

/**
 * A domain event that could not be projected into a notification.
 * <p>
 * The dead letter topic tells you <em>where</em> a failed event went. It does
 * not tell you how many are failing, which organisations are affected, or
 * which ones to replay — a Kafka topic is not something you can query. This
 * table is the queryable record: it is the answer to "what is broken?".
 * <p>
 * A failure here is not the same as a failed delivery. The notification for a
 * successfully projected event is already committed; this table only records
 * events that never became notifications.
 */
@Entity
@Data
@Table(name = "t_notification_failures", schema = "cms")
public class NotificationFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationFailureId;

    /**
     * Identifier of the event that failed. Unique so that a record which is
     * published to the dead letter topic more than once is not duplicated
     * here.
     */
    @Column(name = "event_id", length = 64, unique = true)
    private String eventId;

    /**
     * The kind of event.
     */
    @Column(name = "event_type", length = 100)
    private String eventType;

    /**
     * The topic the event was consumed from.
     */
    @Column(name = "source_topic", length = 120)
    private String sourceTopic;

    /**
     * The partition the event was consumed from.
     */
    @Column(name = "source_partition")
    private Integer sourcePartition;

    /**
     * The offset the event was consumed from.
     */
    @Column(name = "source_offset")
    private Long sourceOffset;

    /**
     * The original event, verbatim.
     * <p>
     * Plain text rather than JSON: a record reaches the dead letter topic
     * precisely because it could not be parsed, so it cannot be assumed to be
     * valid JSON. Storing it as text means the original bytes are always
     * preserved.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    /**
     * The type of the exception that caused the failure.
     */
    @Column(name = "error_type", length = 200)
    private String errorType;

    /**
     * The message of the exception that caused the failure.
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * How many times the consumer retried before giving up.
     */
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    /**
     * How many times this event has been replayed.
     */
    @Column(name = "replay_count", nullable = false)
    private Integer replayCount;

    /**
     * Where the failure is in its lifecycle.
     * <p>
     * The code, not a Java enum: the catalogue lives in
     * {@code cms.t_notification_failure_status_master}.
     */
    @Column(name = "status", nullable = false, length = 40)
    private String status;

    /**
     * When the failure was recorded.
     */
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    /**
     * When the event was last replayed.
     */
    @Column(name = "replayed_at")
    private Timestamp replayedAt;
}
