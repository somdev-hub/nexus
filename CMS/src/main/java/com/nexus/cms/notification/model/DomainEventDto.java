package com.nexus.cms.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * A domain event published by {@code nexus/core} and consumed by the
 * notification subsystem.
 * <p>
 * The event carries <b>facts only</b>: what changed, which two organisations
 * are involved, and who acted. It carries no wording, no severity and no
 * presentation concerns — deciding those is the job of the notification
 * policy registry, which lives in this service.
 * <p>
 * The shape is deliberately flat and versioned. It is not the
 * {@code KafkaMessageDto} envelope used elsewhere in the platform, whose
 * {@code message} field holds a JSON document nested inside a JSON document
 * and which has no room for an organisation id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEventDto {

    /**
     * Unique identifier of the event. Used as the idempotency key: the
     * notification table has a unique constraint on it, so an event that is
     * delivered twice produces exactly one notification.
     */
    private String eventId;

    /**
     * The kind of event.
     * <p>
     * The code, not a Java enum: the catalogue lives in
     * {@code cms.t_notification_event_type_master}, so a new event type is an
     * {@code INSERT} rather than a release. It is part of the wire contract
     * with the producer.
     */
    private String eventType;

    /**
     * Version of the event contract. Incremented when a breaking change is
     * made to this shape.
     */
    private Integer schemaVersion;

    /**
     * When the event happened, as an ISO-8601 instant, e.g.
     * {@code 2026-10-22T10:00:00Z}.
     * <p>
     * A string rather than an {@code Instant}, deliberately. The application
     * wide {@code ObjectMapper} is a bare {@code new ObjectMapper()} that has
     * no Java-time module, so an {@code Instant} on the wire cannot be read
     * or written. Keeping the wire format as an explicit ISO-8601 string
     * avoids that entirely, and is what the wire contract should say anyway.
     */
    private String occurredAt;

    /**
     * The organisation that performed the action.
     */
    private Long actorOrgId;

    /**
     * The type of the organisation that performed the action, e.g.
     * {@code SUPPLIER}. Null when the action was performed by a system job.
     */
    private String actorOrgType;

    /**
     * The organisation that raised the purchase order — the retailer.
     */
    private Long buyerOrgId;

    /**
     * The organisation that fulfils the purchase order — the supplier. May be
     * null for legacy orders that predate the supplier organisation link, in
     * which case the event cannot be addressed and is skipped.
     */
    private Long supplierOrgId;

    /**
     * The kind of entity the event is about, e.g. {@code PURCHASE_ORDER}.
     */
    private String entityType;

    /**
     * The identifier of the entity the event is about.
     */
    private Long entityId;

    /**
     * A human readable reference for the entity, e.g. {@code PO-2026-0042}.
     */
    private String entityRef;

    /**
     * Event specific facts. For a purchase order status change this holds
     * {@code previousStatus} and {@code newStatus}.
     */
    private Map<String, Object> facts;
}
