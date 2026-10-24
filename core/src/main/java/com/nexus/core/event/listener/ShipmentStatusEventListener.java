package com.nexus.core.event.listener;

import com.nexus.core.event.publisher.ShipmentStatusChangePublisher;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.ShipmentStatus;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShipmentStatusEventListener {

    private final ShipmentStatusChangePublisher publisher;

    public ShipmentStatusEventListener(@Lazy ShipmentStatusChangePublisher publisher) {
        this.publisher = publisher;
    }

    @PostLoad
    public void onPostLoad(Shipment shipment) {
        shipment.setStatusBeforeChange(shipment.getStatus());
    }

    @PrePersist
    public void onPrePersist(Shipment shipment) {
        shipment.setStatusBeforeChange(null);
    }

    /**
     * Publish an event for a newly created shipment that is already in a
     * non-initial status (e.g. BOOKED via immediate handover to logistics).
     * <p>
     * A shipment created as DRAFT produces no event — there is nothing to
     * notify about until the status changes.
     */
    @PostPersist
    public void onPostPersist(Shipment shipment) {
        ShipmentStatus current = shipment.getStatus();
        if (current == null || current == ShipmentStatus.DRAFT) {
            return;
        }
        try {
            publisher.publish(shipment, null);
        } catch (Exception e) {
            log.error("Failed to publish a shipment creation event for shipment {} — writing a minimal fallback row",
                    shipment.getShipmentId(), e);
            try {
                publisher.publishMinimal(shipment, null);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for shipment {}", shipment.getShipmentId(), fallbackError);
            }
        }
    }

    @PostUpdate
    public void onPostUpdate(Shipment shipment) {
        ShipmentStatus previous = shipment.getStatusBeforeChange();
        ShipmentStatus current = shipment.getStatus();
        if (previous == null || current == null || previous == current) {
            return;
        }
        try {
            publisher.publish(shipment, previous);
        } catch (Exception e) {
            log.error("Failed to publish a shipment status change event for shipment {} — writing a minimal fallback row",
                    shipment.getShipmentId(), e);
            try {
                publisher.publishMinimal(shipment, previous);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for shipment {}", shipment.getShipmentId(), fallbackError);
            }
        }
    }
}