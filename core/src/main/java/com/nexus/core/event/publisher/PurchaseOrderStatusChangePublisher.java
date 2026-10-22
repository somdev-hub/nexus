package com.nexus.core.event.publisher;

import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.DomainEventType;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.security.OrganizationContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns a purchase order status change into a {@link DomainEvent} and hands
 * it to the publisher.
 * <p>
 * The facts the event carries are the ones the notification policy needs to
 * render a message: the order number, the two organisations involved, who
 * acted, and the dates that matter. Nothing presentation-specific is added
 * here — no wording, no severity, no destination.
 */
@Service
public class PurchaseOrderStatusChangePublisher {

    private final com.nexus.core.event.publisher.DomainEventPublisher publisher;

    public PurchaseOrderStatusChangePublisher(com.nexus.core.event.publisher.DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(PurchaseOrder purchaseOrder, PurchaseOrderStatus previousStatus) {
        if (purchaseOrder == null || purchaseOrder.getPurchaseOrderId() == null) {
            return;
        }
        if (purchaseOrder.getBuyerOrg() == null) {
            // Without a buyer organisation the event cannot be addressed.
            return;
        }

        Long actorOrgId = OrganizationContextHolder.getCurrentOrganizationId();
        String actorOrgType = OrganizationContextHolder.getCurrentOrganizationType();

        Long buyerOrgId = purchaseOrder.getBuyerOrg().getAccountId();
        Long supplierOrgId = resolveSupplierOrgId(purchaseOrder);

        // The counterparty of the actor. This is a fact about who the other
        // party is, resolved with the universal rule the consumer also uses.
        // It is a hint for the consumer's policy, which may override it.
        Long targetOrgId;
        if (actorOrgId != null && actorOrgId.equals(buyerOrgId)) {
            targetOrgId = supplierOrgId;
        } else {
            targetOrgId = buyerOrgId;
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", purchaseOrder.getStatus() != null ? purchaseOrder.getStatus().name() : null);
        if (purchaseOrder.getPoNumber() != null) {
            facts.put("poNumber", purchaseOrder.getPoNumber());
        }
        if (purchaseOrder.getConfirmedDeliveryDate() != null) {
            facts.put("confirmedDeliveryDate", purchaseOrder.getConfirmedDeliveryDate().toString());
        }
        if (purchaseOrder.getExpectedDeliveryDate() != null) {
            facts.put("expectedDeliveryDate", purchaseOrder.getExpectedDeliveryDate().toString());
        }
        if (purchaseOrder.getSupplier() != null && purchaseOrder.getSupplier().getBusinessName() != null) {
            facts.put("supplierName", purchaseOrder.getSupplier().getBusinessName());
        }
        if (purchaseOrder.getSupplierNotes() != null) {
            facts.put("supplierNotes", purchaseOrder.getSupplierNotes());
        }

        com.nexus.core.event.DomainEvent event = new com.nexus.core.event.DomainEvent(
                java.util.UUID.randomUUID().toString(),
                DomainEventType.PURCHASE_ORDER_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                actorOrgId,
                actorOrgType,
                buyerOrgId,
                supplierOrgId,
                targetOrgId,
                "PURCHASE_ORDER",
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getPoNumber(),
                facts);

        publisher.publish(event);
    }

    /**
     * Write a minimal outbox row when the normal assembly failed. The relay
     * still has something to publish and the failure is visible, even though
     * the recipient will only get a generic notification.
     */
    public void publishMinimal(PurchaseOrder purchaseOrder, PurchaseOrderStatus previousStatus) {
        if (purchaseOrder == null || purchaseOrder.getBuyerOrg() == null) {
            return;
        }
        java.util.Map<String, Object> facts = new java.util.HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", purchaseOrder.getStatus() != null ? purchaseOrder.getStatus().name() : null);
        if (purchaseOrder.getPoNumber() != null) {
            facts.put("poNumber", purchaseOrder.getPoNumber());
        }
        com.nexus.core.event.DomainEvent event = new com.nexus.core.event.DomainEvent(
                java.util.UUID.randomUUID().toString(),
                DomainEventType.PURCHASE_ORDER_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                OrganizationContextHolder.getCurrentOrganizationId(),
                OrganizationContextHolder.getCurrentOrganizationType(),
                purchaseOrder.getBuyerOrg().getAccountId(),
                resolveSupplierOrgId(purchaseOrder),
                // counterparty of the (unknown) actor — same rule as above
                purchaseOrder.getBuyerOrg().getAccountId(),
                "PURCHASE_ORDER",
                purchaseOrder.getPurchaseOrderId(),
                purchaseOrder.getPoNumber(),
                facts);
        publisher.publish(event);
    }

    private Long resolveSupplierOrgId(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getSupplierOrg() != null) {
            return purchaseOrder.getSupplierOrg().getAccountId();
        }
        if (purchaseOrder.getSupplier() != null
                && purchaseOrder.getSupplier().getSupplierOrgAccountId() != null) {
            return purchaseOrder.getSupplier().getSupplierOrgAccountId();
        }
        return null;
    }
}
