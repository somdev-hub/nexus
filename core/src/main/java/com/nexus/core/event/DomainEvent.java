package com.nexus.core.event;

import com.nexus.core.model.enums.PurchaseOrderStatus;

import java.time.Instant;
import java.util.Map;

/**
 * A domain event published by {@code nexus/core}.
 * <p>
 * The event carries <b>facts only</b>: what changed, which two organisations
 * are involved, and who acted. It carries no wording, no severity and no
 * presentation concerns — deciding those is the job of the notification
 * policy registry, which lives in {@code nexus/CMS}.
 * <p>
 * The shape is the wire contract with the consumer. It is deliberately flat
 * and versioned, and it is not the {@code KafkaMessageDto} envelope used
 * elsewhere in the platform, whose {@code message} field holds a JSON
 * document nested inside a JSON document and which has no room for an
 * organisation id.
 *
 * @param eventId     unique identifier of the event — the idempotency key
 * @param eventType   the kind of event
 * @param schemaVersion version of the event contract
 * @param occurredAt  when the event happened, as an ISO-8601 instant
 * @param actorOrgId  the organisation that performed the action, null for a
 *                    system job
 * @param actorOrgType the type of the organisation that performed the action
 * @param buyerOrgId  the organisation that raised the purchase order
 * @param supplierOrgId the organisation that fulfils the purchase order, may
 *                     be null for legacy orders
 * @param targetOrgId the organisation the event is addressed to — the
 *                    counterparty of the actor. Resolved by the producer
 *                    with the universal counterparty rule, so that the
 *                    recipient can be determined without a round trip. The
 *                    consumer's policy can override it
 * @param entityType  the kind of entity the event is about
 * @param entityId    the identifier of the entity the event is about
 * @param entityRef   a human readable reference for the entity
 * @param facts       event specific facts
 */
public record DomainEvent(
        String eventId,
        DomainEventType eventType,
        int schemaVersion,
        String occurredAt,
        Long actorOrgId,
        String actorOrgType,
        Long buyerOrgId,
        Long supplierOrgId,
        Long targetOrgId,
        String entityType,
        Long entityId,
        String entityRef,
        Map<String, Object> facts
) {
}
