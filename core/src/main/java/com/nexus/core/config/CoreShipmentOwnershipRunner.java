package com.nexus.core.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.model.entities.ConsignmentStock;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.entities.VmiConfig;
import com.nexus.core.repository.ConsignmentStockRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.VmiConfigRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Legacy consignment-fill and VMI-replenishment shipments were created without
 * supplier/retailer ownership, which meant they could not be scoped to the
 * supplier or handed over to a logistics partner. This runner heals those rows
 * from the consignment / VMI they reference (idempotent: only rows with a null
 * supplier org are touched).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoreShipmentOwnershipRunner implements ApplicationRunner {

    private static final List<String> CORE_REFERENCE_TYPES =
            List.of("CONSIGNMENT_FILL", "VMI_REPLENISHMENT");

    private final ShipmentRepo shipmentRepo;
    private final ConsignmentStockRepo consignmentStockRepo;
    private final VmiConfigRepo vmiConfigRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Shipment> legacy =
                shipmentRepo.findBySupplierOrgIsNullAndReferenceTypeIn(CORE_REFERENCE_TYPES);
        int healed = 0;
        for (Shipment shipment : legacy) {
            if (shipment.getReferenceId() == null) {
                continue;
            }
            try {
                if ("CONSIGNMENT_FILL".equals(shipment.getReferenceType())) {
                    ConsignmentStock cs = consignmentStockRepo
                            .findById(shipment.getReferenceId()).orElse(null);
                    if (cs == null) {
                        continue;
                    }
                    shipment.setSupplierOrg(cs.getSupplierOrg());
                    shipment.setRetailerOrg(cs.getRetailerOrg());
                } else {
                    VmiConfig vmi = vmiConfigRepo
                            .findById(shipment.getReferenceId()).orElse(null);
                    if (vmi == null) {
                        continue;
                    }
                    shipment.setSupplierOrg(vmi.getSupplierOrg());
                    shipment.setRetailerOrg(vmi.getRetailerOrg());
                }
                if (shipment.getSupplierOrg() == null) {
                    continue;
                }
                shipmentRepo.save(shipment);
                healed++;
            } catch (Exception e) {
                log.warn("Could not heal ownership for shipment {}: {}",
                        shipment.getShipmentId(), e.getMessage());
            }
        }
        if (healed > 0) {
            log.info("Healed ownership for {} legacy core shipment(s)", healed);
        }
    }
}
