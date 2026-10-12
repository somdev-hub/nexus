package com.nexus.core.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.model.entities.AdvanceShipmentNotice;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.payload.AdvanceShipmentNoticeDto;
import com.nexus.core.repository.AdvanceShipmentNoticeRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/asn")
@RequiredArgsConstructor
public class AsnController {

    private final AdvanceShipmentNoticeRepo asnRepo;
    private final PurchaseOrderRepo poRepo;
    private final ShipmentRepo shipmentRepo;

        @GetMapping("/all")
    @LogActivity("Get All Visible ASNs")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll(@PageableDefault(size = 20) Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        return ResponseEntity.ok(asnRepo.findVisibleToOrg(orgId, pageable).map(this::toDto));
    }

    @GetMapping("/purchase-order/{poId}")
    @LogActivity("Get ASNs by Purchase Order")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getByPurchaseOrder(@PathVariable Long poId) {        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = poRepo.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", poId));
        if (!isCounterparty(po, orgId)) {
            throw new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", poId);
        }
        List<AdvanceShipmentNoticeDto> dtos = asnRepo.findByPurchaseOrderPurchaseOrderId(poId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/purchase-order-number/{poNumber}")
    @LogActivity("Get ASNs by PO Number")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getByPurchaseOrderNumber(@PathVariable String poNumber) {        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<PurchaseOrder> visible = poRepo.findByPoNumber(poNumber).stream()
                .filter(po -> isCounterparty(po, orgId))
                .toList();
        if (visible.isEmpty()) {
            throw new ResourceNotFoundException("PurchaseOrder", "poNumber", poNumber);
        }
        List<AdvanceShipmentNoticeDto> dtos = visible.stream()
                .flatMap(po -> asnRepo.findByPurchaseOrderPurchaseOrderId(po.getPurchaseOrderId()).stream())
                .map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    private boolean isCounterparty(PurchaseOrder po, Long orgId) {
        if (po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId().equals(orgId)) return true;
        if (po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null) {
            if (po.getPartnership().getPrimaryOrg() != null
                    && po.getPartnership().getPrimaryOrg().getAccountId().equals(orgId)) return true;
            if (po.getPartnership().getSecondaryOrg() != null
                    && po.getPartnership().getSecondaryOrg().getAccountId().equals(orgId)) return true;
        }
        return false;
    }

    private AdvanceShipmentNoticeDto toDto(AdvanceShipmentNotice asn) {
        AdvanceShipmentNoticeDto dto = new AdvanceShipmentNoticeDto();
        dto.setAsnId(asn.getAsnId());
        dto.setAsnNumber(asn.getAsnNumber());
        double orderedTotal = 0.0;
        if (asn.getPurchaseOrder() != null) {
            dto.setPurchaseOrderId(asn.getPurchaseOrder().getPurchaseOrderId());
            dto.setPoNumber(asn.getPurchaseOrder().getPoNumber());
            if (asn.getPurchaseOrder().getLineItems() != null) {
                orderedTotal = asn.getPurchaseOrder().getLineItems().stream()
                        .mapToDouble(li -> li.getQuantityOrdered() != null ? li.getQuantityOrdered() : 0)
                        .sum();
            }
        }
        dto.setOrderedQuantity(orderedTotal);
        if (asn.getBuyerOrg() != null) dto.setBuyerOrgId(asn.getBuyerOrg().getAccountId());
        if (asn.getSupplierOrg() != null) dto.setSupplierOrgId(asn.getSupplierOrg().getAccountId());
        dto.setShipmentId(asn.getShipmentId());
        dto.setStatus(asn.getStatus());
        dto.setNotes(asn.getNotes());
        dto.setCreatedAt(asn.getCreatedAt());
        dto.setUpdatedAt(asn.getUpdatedAt());
        // Dispatch snapshot from the linked shipment: what is physically on
        // this truck, how it travels, and when it lands.
        if (asn.getShipmentId() != null) {
            Shipment shipment = shipmentRepo.findById(asn.getShipmentId()).orElse(null);
            if (shipment != null) {
                dto.setShipmentNumber(shipment.getShipmentNumber());
                dto.setShipmentStatus(
                        shipment.getStatus() != null ? shipment.getStatus().name() : null);
                dto.setTrackingNumber(shipment.getTrackingNumber());
                dto.setCarrierName(shipment.getCarrierName());
                if (shipment.getLogisticsOrg() != null) {
                    dto.setLogisticsOrgName(shipment.getLogisticsOrg().getName());
                }
                dto.setPickupLocation(shipment.getPickupLocation());
                dto.setDeliveryLocation(shipment.getDeliveryLocation());
                if (shipment.getPickupDate() != null) {
                    dto.setPickupDate(shipment.getPickupDate().toString());
                }
                if (shipment.getDeliveryDate() != null) {
                    dto.setDeliveryDate(shipment.getDeliveryDate().toString());
                }
                double backordered = shipment.getBackorderedQuantity() != null
                        ? shipment.getBackorderedQuantity() : 0.0;
                dto.setBackorderedQuantity(backordered);
                dto.setShippedQuantity(Math.max(0, orderedTotal - backordered));
                dto.setTotalPackages(shipment.getTotalPackages());
                dto.setTotalWeight(shipment.getTotalWeight());
                dto.setTotalVolume(shipment.getTotalVolume());
                if (shipment.getEstimatedArrival() != null) {
                    dto.setEstimatedArrival(shipment.getEstimatedArrival().toString());
                }
            }
        }
        return dto;
    }
}
