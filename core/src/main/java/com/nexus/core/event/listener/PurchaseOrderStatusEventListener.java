package com.nexus.core.event.listener;

import com.nexus.core.event.publisher.PurchaseOrderStatusChangePublisher;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Captures every change to a purchase order's status.
 * <p>
 * The status is snapshotted when the entity is loaded, and compared with
 * the new value when it is updated. A change produces a domain event, which
 * is written to the outbox in the same transaction as the business change.
 * <p>
 * Because the capture happens at the entity level, it catches <em>every</em>
 * status change in the system — the acknowledge flow, the fulfillment flow,
 * the goods receipt flow, the invoice flow, and the scheduled runner that
 * closes delivered shipments. None of those code paths have to be modified
 * individually, so none of them can silently forget to emit the event.
 * <p>
 * <b>Circular dependency.</b> The entity listener is a Spring bean that
 * depends on the publisher, which depends on the outbox repository, which
 * needs the entity manager factory, which needs this listener class. The
 * dependency is broken with {@code @Lazy} on the constructor parameter: the
 * publisher is injected lazily, so the listener can be created before the
 * publisher exists.
 */
@Component
@Slf4j
public class PurchaseOrderStatusEventListener {

    private final PurchaseOrderStatusChangePublisher publisher;

    public PurchaseOrderStatusEventListener(@Lazy PurchaseOrderStatusChangePublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Snapshot the status when the entity is loaded.
     */
    @PostLoad
    public void onPostLoad(PurchaseOrder purchaseOrder) {
        purchaseOrder.setStatusBeforeChange(purchaseOrder.getStatus());
    }

    /**
     * Mark a newly created entity as having no previous status.
     */
    @PrePersist
    public void onPrePersist(PurchaseOrder purchaseOrder) {
        purchaseOrder.setStatusBeforeChange(null);
    }

    /**
     * Publish the event if the status changed.
     */
    @PostUpdate
    public void onPostUpdate(PurchaseOrder purchaseOrder) {
        PurchaseOrderStatus previous = purchaseOrder.getStatusBeforeChange();
        PurchaseOrderStatus current = purchaseOrder.getStatus();
        if (previous == null || current == null || previous == current) {
            return;
        }
        try {
            publisher.publish(purchaseOrder, previous);
        } catch (Exception e) {
            // The business transaction must never fail because of a
            // notification. Log loudly and write a minimal fallback row so
            // that the failure is visible and the event is not lost.
            log.error("Failed to publish a purchase order status change event for PO {} — writing a minimal fallback row",
                    purchaseOrder.getPurchaseOrderId(), e);
            try {
                publisher.publishMinimal(purchaseOrder, previous);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for PO {}",
                        purchaseOrder.getPurchaseOrderId(), fallbackError);
            }
        }
    }
}
