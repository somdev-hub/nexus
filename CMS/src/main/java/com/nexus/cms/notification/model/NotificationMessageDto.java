package com.nexus.cms.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * A notification as the browser receives it.
 * <p>
 * The browser turns {@code entityType} and {@code entityId} into a route using
 * its own route table. The backend never hardcodes a frontend path, so a
 * route can change without a backend release.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDto {

    /**
     * Identifier of the stored notification. Stable, and the key the browser
     * uses to deduplicate.
     */
    private Long notificationId;

    /**
     * Identifier of the event this notification was projected from. The
     * browser uses it to drop a message it has already seen.
     */
    private String eventId;

    /**
     * The kind of event.
     */
    private String eventType;

    /**
     * The kind of entity the notification is about, e.g.
     * {@code PURCHASE_ORDER}.
     */
    private String entityType;

    /**
     * The identifier of the entity the notification is about.
     */
    private Long entityId;

    /**
     * A human readable reference for the entity, e.g. {@code PO-2026-0042}.
     */
    private String entityRef;

    /**
     * The headline.
     */
    private String title;

    /**
     * The body.
     */
    private String body;

    /**
     * How prominent the notification is.
     * <p>
     * The code, not a Java enum: the catalogue lives in
     * {@code cms.t_notification_severity_master}.
     */
    private String severity;

    /**
     * The organisation the notification belongs to.
     */
    private Long targetOrgId;

    /**
     * The event facts the notification was rendered from.
     */
    private Map<String, Object> facts;

    /**
     * When the notification was created, as an ISO-8601 instant.
     * <p>
     * A string rather than an {@code Instant} — see
     * {@link DomainEventDto#occurredAt} for why.
     */
    private String createdAt;
}
