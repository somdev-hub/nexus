package com.nexus.core.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-time-style healing: shipments that reached a delivered state before
 * PO-follow logic existed left their purchase orders stuck at
 * {@code PICKED_UP}. On every boot this moves those POs forward (idempotent:
 * only forward moves, never regressions or terminal-state touches).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveredShipmentPoSyncRunner implements ApplicationRunner {

    private final ShipmentRepo shipmentRepo;
    private final PurchaseOrderRepo purchaseOrderRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Shipment> shipments = shipmentRepo.findAll();
        int healed = 0;
        for (Shipment shipment : shipments) {
            ShipmentStatus status = shipment.getStatus();
            if (status != ShipmentStatus.DELIVERED && status != ShipmentStatus.PARTIALLY_DELIVERED) {
                continue;
            }
            if (shipment.getPurchaseOrder() == null
                    || shipment.getPurchaseOrder().getPurchaseOrderId() == null) {
                continue;
            }
            PurchaseOrder po = purchaseOrderRepo
                    .findById(shipment.getPurchaseOrder().getPurchaseOrderId()).orElse(null);
            if (po == null) continue;
            PurchaseOrderStatus current = po.getStatus();
            if (current == PurchaseOrderStatus.RECEIVED || current == PurchaseOrderStatus.INVOICED
                    || current == PurchaseOrderStatus.PAID || current == PurchaseOrderStatus.CANCELLED
                    || current == PurchaseOrderStatus.CLOSED) {
                continue;
            }
            if (status == ShipmentStatus.DELIVERED) {
                po.setStatus(PurchaseOrderStatus.RECEIVED);
                purchaseOrderRepo.save(po);
                healed++;
            } else if (current != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
                purchaseOrderRepo.save(po);
                healed++;
            }
        }
        if (healed > 0) {
            log.info("Healed {} purchase order(s) stuck behind delivered shipments", healed);
        } else {
            log.info("No stuck purchase orders behind delivered shipments");
        }
    }
}
