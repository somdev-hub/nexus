package com.nexus.core.event.publisher;

import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.DomainEventType;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.security.OrganizationContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a shipment status change into one or more {@link DomainEvent}s and
 * hands them to the publisher.
 * <p>
 * Multiple parties may need to be notified for a single status change:
 * - Handover (supplier → logistics): logistics is notified.
 * - Pickup / delivery / exception: both supplier and retailer are notified.
 * - The actor (who performed the action) is never notified about their own
 *   action.
 * <p>
 * Each target party gets its own event + outbox row, so the at-least-once
 * delivery and idempotency guarantees apply per recipient.
 */
@Service
public class ShipmentStatusChangePublisher {

    private final DomainEventPublisher publisher;

    public ShipmentStatusChangePublisher(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Shipment shipment, ShipmentStatus previousStatus) {
        if (shipment == null || shipment.getShipmentId() == null) {
            return;
        }

        Long actorOrgId = OrganizationContextHolder.getCurrentOrganizationId();
        String actorOrgType = OrganizationContextHolder.getCurrentOrganizationType();

        Long supplierOrgId = shipment.getSupplierOrg() != null ? shipment.getSupplierOrg().getAccountId() : null;
        Long retailerOrgId = shipment.getRetailerOrg() != null ? shipment.getRetailerOrg().getAccountId() : null;
        Long logisticsOrgId = shipment.getLogisticsOrg() != null ? shipment.getLogisticsOrg().getAccountId() : null;

        // Determine which parties should be notified (everyone except the actor).
        List<Long> targets = new ArrayList<>();
        if (logisticsOrgId != null && !logisticsOrgId.equals(actorOrgId)) {
            targets.add(logisticsOrgId);
        }
        if (supplierOrgId != null && !supplierOrgId.equals(actorOrgId) && !targets.contains(supplierOrgId)) {
            targets.add(supplierOrgId);
        }
        if (retailerOrgId != null && !retailerOrgId.equals(actorOrgId) && !targets.contains(retailerOrgId)) {
            targets.add(retailerOrgId);
        }

        // Fallback: if no specific targets resolved, notify the counterparty
        // of the actor (existing behaviour).
        if (targets.isEmpty()) {
            Long targetOrgId;
            if (actorOrgId != null && actorOrgId.equals(supplierOrgId)) {
                targetOrgId = retailerOrgId;
            } else {
                targetOrgId = supplierOrgId;
            }
            if (targetOrgId != null) {
                targets.add(targetOrgId);
            }
        }

        for (Long targetOrgId : targets) {
            publishOne(shipment, previousStatus, actorOrgId, actorOrgType,
                    supplierOrgId, retailerOrgId, logisticsOrgId, targetOrgId);
        }
    }

    private void publishOne(Shipment shipment, ShipmentStatus previousStatus,
                            Long actorOrgId, String actorOrgType,
                            Long supplierOrgId, Long retailerOrgId, Long logisticsOrgId,
                            Long targetOrgId) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", shipment.getStatus() != null ? shipment.getStatus().name() : null);
        if (shipment.getShipmentNumber() != null) {
            facts.put("shipmentNumber", shipment.getShipmentNumber());
        }
        if (shipment.getTrackingNumber() != null) {
            facts.put("trackingNumber", shipment.getTrackingNumber());
        }
        if (shipment.getCarrierName() != null) {
            facts.put("carrierName", shipment.getCarrierName());
        }
        if (logisticsOrgId != null) {
            facts.put("logisticsOrgId", logisticsOrgId);
        }

        DomainEvent event = new DomainEvent(
                java.util.UUID.randomUUID().toString(),
                DomainEventType.SHIPMENT_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                actorOrgId,
                actorOrgType,
                supplierOrgId,
                retailerOrgId,
                targetOrgId,
                "SHIPMENT",
                shipment.getShipmentId(),
                shipment.getShipmentNumber(),
                facts);

        publisher.publish(event);
    }

    public void publishMinimal(Shipment shipment, ShipmentStatus previousStatus) {
        if (shipment == null) return;
        Long supplierOrgId = shipment.getSupplierOrg() != null ? shipment.getSupplierOrg().getAccountId() : null;
        Long retailerOrgId = shipment.getRetailerOrg() != null ? shipment.getRetailerOrg().getAccountId() : null;
        Long logisticsOrgId = shipment.getLogisticsOrg() != null ? shipment.getLogisticsOrg().getAccountId() : null;

        Long targetOrgId = logisticsOrgId != null ? logisticsOrgId
                : (supplierOrgId != null ? supplierOrgId : retailerOrgId);

        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", shipment.getStatus() != null ? shipment.getStatus().name() : null);
        if (shipment.getShipmentNumber() != null) {
            facts.put("shipmentNumber", shipment.getShipmentNumber());
        }
        if (logisticsOrgId != null) {
            facts.put("logisticsOrgId", logisticsOrgId);
        }
        DomainEvent event = new DomainEvent(
                java.util.UUID.randomUUID().toString(),
                DomainEventType.SHIPMENT_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                OrganizationContextHolder.getCurrentOrganizationId(),
                OrganizationContextHolder.getCurrentOrganizationType(),
                supplierOrgId,
                retailerOrgId,
                targetOrgId,
                "SHIPMENT",
                shipment.getShipmentId(),
                shipment.getShipmentNumber(),
                facts);
        publisher.publish(event);
    }
}