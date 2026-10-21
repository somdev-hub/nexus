package com.nexus.cms.notification.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.Map;

/**
 * A notification in an organisation's inbox.
 * <p>
 * One row is created per domain event that the notification policy decides is
 * worth telling somebody about. The row is the durable record: the realtime
 * push is best effort, but the inbox is not, so a user who was offline still
 * finds the notification when they come back.
 */
@Entity
@Data
@Table(name = "t_notifications", schema = "cms")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    /**
     * Identifier of the domain event this notification was projected from.
     * <p>
     * Unique: the event is projected at most once. The producer publishes
     * at-least-once, so the same event can arrive more than once, and this
     * constraint is what turns that into a single notification rather than a
     * duplicate.
     */
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    /**
     * The kind of event, e.g. {@code PURCHASE_ORDER_STATUS_CHANGED}.
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * The organisation the notification belongs to. Every member of this
     * organisation sees it.
     */
    @Column(name = "target_org_id", nullable = false)
    private Long targetOrgId;

    /**
     * The kind of entity the notification is about, e.g.
     * {@code PURCHASE_ORDER}.
     */
    @Column(name = "entity_type", length = 60)
    private String entityType;

    /**
     * The identifier of the entity the notification is about.
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * A human readable reference for the entity, e.g. {@code PO-2026-0042}.
     */
    @Column(name = "entity_ref", length = 80)
    private String entityRef;

    /**
     * The rendered headline.
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * The rendered body.
     */
    @Column(name = "body", length = 2000)
    private String body;

    /**
     * How prominent the notification is.
     * <p>
     * The code, not a Java enum: the catalogue lives in
     * {@code cms.t_notification_severity_master}, so a new severity is an
     * {@code INSERT} rather than a release.
     */
    @Column(name = "severity", nullable = false, length = 40)
    private String severity;

    /**
     * How the notification is addressed.
     * <p>
     * The code, not a Java enum: the catalogue lives in
     * {@code cms.t_notification_addressing_type_master}. It is what selects
     * the delivery router, and it is the seam that user-level and
     * authority-level addressing will use.
     */
    @Column(name = "addressing", nullable = false, length = 40)
    private String addressing;

    /**
     * The event facts the notification was rendered from.
     * <p>
     * A JSON column. {@code @JdbcTypeCode(SqlTypes.JSON)} is what tells
     * Hibernate to bind the map as JSON — without it, the map is treated as
     * an opaque object and the insert fails with
     * "Unsupported Types value: 2,000".
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * When the notification was created.
     */
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;
}
