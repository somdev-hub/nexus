package com.nexus.core.controller;

import java.util.List;

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
import com.nexus.core.payload.AdvanceShipmentNoticeDto;
import com.nexus.core.repository.AdvanceShipmentNoticeRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.security.OrganizationContextHolder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/asn")
@RequiredArgsConstructor
public class AsnController {

    private final AdvanceShipmentNoticeRepo asnRepo;
    private final PurchaseOrderRepo poRepo;

    @GetMapping("/purchase-order/{poId}")
    @LogActivity("Get ASNs by Purchase Order")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getByPurchaseOrder(@PathVariable Long poId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = poRepo.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", poId));
        if (!isCounterparty(po, orgId)) {
            throw new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", poId);
        }
        List<AdvanceShipmentNoticeDto> dtos = asnRepo.findByPurchaseOrderPurchaseOrderId(poId)
                .stream().map(this::toDto).toList();
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
        if (asn.getPurchaseOrder() != null) {
            dto.setPurchaseOrderId(asn.getPurchaseOrder().getPurchaseOrderId());
            dto.setPoNumber(asn.getPurchaseOrder().getPoNumber());
        }
        if (asn.getBuyerOrg() != null) dto.setBuyerOrgId(asn.getBuyerOrg().getAccountId());
        if (asn.getSupplierOrg() != null) dto.setSupplierOrgId(asn.getSupplierOrg().getAccountId());
        dto.setShipmentId(asn.getShipmentId());
        dto.setStatus(asn.getStatus());
        dto.setNotes(asn.getNotes());
        dto.setCreatedAt(asn.getCreatedAt());
        dto.setUpdatedAt(asn.getUpdatedAt());
        return dto;
    }
}
