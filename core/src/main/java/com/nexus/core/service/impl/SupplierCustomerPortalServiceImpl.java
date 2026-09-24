package com.nexus.core.service.impl;

import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierCustomerPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierCustomerPortalServiceImpl implements SupplierCustomerPortalService {

    private final PurchaseOrderRepo poRepo;
    private final InvoiceRepo invoiceRepo;
    private final ShipmentRepo shipmentRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerOrders(Long buyerOrgId, String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<com.nexus.core.entities.PurchaseOrder> filtered = poRepo.findAll().stream()
                .filter(po -> isSupplierOrder(po, orgId))
                .filter(po -> buyerOrgId == null || (po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId().equals(buyerOrgId)))
                .filter(po -> status == null || po.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        return paginate(filtered.stream().map(po -> modelMapper.map(po, com.nexus.core.payload.PurchaseOrderDto.class)).collect(Collectors.toList()), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerInvoices(Long buyerOrgId, String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        // invoices linked via purchase order
        var pos = poRepo.findAll().stream().filter(po -> isSupplierOrder(po, orgId)).collect(Collectors.toList());
        var poIds = pos.stream().map(com.nexus.core.entities.PurchaseOrder::getPurchaseOrderId).collect(Collectors.toSet());
        List<com.nexus.core.entities.Invoice> invoices = invoiceRepo.findAll().stream()
                .filter(inv -> inv.getPurchaseOrder() != null && poIds.contains(inv.getPurchaseOrder().getPurchaseOrderId()))
                .filter(inv -> buyerOrgId == null || (inv.getPurchaseOrder().getBuyerOrg() != null && inv.getPurchaseOrder().getBuyerOrg().getAccountId().equals(buyerOrgId)))
                .filter(inv -> status == null || inv.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        return paginate(invoices.stream().map(inv -> modelMapper.map(inv, com.nexus.core.payload.InvoiceDto.class)).collect(Collectors.toList()), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerShipments(Long buyerOrgId, String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<com.nexus.core.entities.Shipment> shipments = shipmentRepo.findAll().stream()
                .filter(s -> s.getSupplierOrg() != null && s.getSupplierOrg().getAccountId().equals(orgId))
                .filter(s -> buyerOrgId == null || (s.getRetailerOrg() != null && s.getRetailerOrg().getAccountId().equals(buyerOrgId)))
                .filter(s -> status == null || s.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        return paginate(shipments, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerReturns(Pageable pageable) {
        // For MVP, returns not yet implemented; return empty page
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerSummary(Long buyerOrgId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var orders = poRepo.findAll().stream().filter(po -> isSupplierOrder(po, orgId))
                .filter(po -> buyerOrgId == null || (po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId().equals(buyerOrgId)))
                .collect(Collectors.toList());
        long totalOrders = orders.size();
        double totalValue = orders.stream().mapToDouble(po -> po.getTotalAmount() != null ? po.getTotalAmount() : 0).sum();
        long openOrders = orders.stream().filter(po -> po.getStatus() != com.nexus.core.entities.PurchaseOrderStatus.CANCELLED && po.getStatus() != com.nexus.core.entities.PurchaseOrderStatus.CLOSED && po.getStatus() != com.nexus.core.entities.PurchaseOrderStatus.PAID).count();
        var invoices = invoiceRepo.findAll().stream()
                .filter(inv -> inv.getPurchaseOrder() != null && orders.stream().anyMatch(po -> po.getPurchaseOrderId().equals(inv.getPurchaseOrder().getPurchaseOrderId())))
                .collect(Collectors.toList());
        long totalInvoices = invoices.size();
        var shipments = shipmentRepo.findAll().stream().filter(s -> s.getSupplierOrg() != null && s.getSupplierOrg().getAccountId().equals(orgId)).collect(Collectors.toList());
        long totalShipments = shipments.size();
        return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "totalOrderValue", totalValue,
                "openOrders", openOrders,
                "totalInvoices", totalInvoices,
                "totalShipments", totalShipments,
                "buyerOrgId", buyerOrgId != null ? buyerOrgId : "ALL"
        ));
    }

    private boolean isSupplierOrder(com.nexus.core.entities.PurchaseOrder po, Long orgId) {
        if (po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getSecondaryOrg() != null && po.getPartnership().getSecondaryOrg().getAccountId().equals(orgId)) return true;
        return false;
    }

    private <T> ResponseEntity<?> paginate(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<T> sub = list.subList(Math.min(start, list.size()), end);
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(sub, pageable, list.size()));
    }
}
