package com.nexus.core.service.implementations;

import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierCustomerPortalService;
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
        com.nexus.core.model.enums.PurchaseOrderStatus st = parsePoStatus(status);
        if (status != null && !status.isBlank() && st == null) {
            // Preserve legacy behavior: unknown status matches nothing -> empty page (200).
            return paginate(List.of(), pageable, 0);
        }
        var page = poRepo.findSupplierVisibleOrders(orgId, st, null, buyerOrgId, pageable);
        return paginate(page.getContent().stream().map(po -> modelMapper.map(po, com.nexus.core.payload.PurchaseOrderDto.class)).collect(Collectors.toList()), pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerInvoices(Long buyerOrgId, String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        com.nexus.core.model.enums.InvoiceStatus st = parseInvoiceStatus(status);
        if (status != null && !status.isBlank() && st == null) {
            // Preserve legacy behavior: unknown status matches nothing -> empty page (200).
            return paginate(List.of(), pageable, 0);
        }
        var page = invoiceRepo.findSupplierVisibleInvoices(orgId, buyerOrgId, st, pageable);
        return paginate(page.getContent().stream().map(inv -> modelMapper.map(inv, com.nexus.core.payload.InvoiceDto.class)).collect(Collectors.toList()), pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerShipments(Long buyerOrgId, String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        com.nexus.core.model.enums.ShipmentStatus st = parseShipmentStatus(status);
        if (status != null && !status.isBlank() && st == null) {
            // Preserve legacy behavior: unknown status matches nothing -> empty page (200).
            return paginate(List.of(), pageable, 0);
        }
        var page = shipmentRepo.findSupplierVisibleShipments(orgId, buyerOrgId, st, pageable);
        return paginate(page.getContent(), pageable, page.getTotalElements());
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
        var orders = poRepo.findSupplierVisibleOrdersList(orgId).stream()
                .filter(po -> buyerOrgId == null || (po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId().equals(buyerOrgId)))
                .collect(Collectors.toList());
        long totalOrders = orders.size();
        double totalValue = orders.stream().mapToDouble(po -> po.getTotalAmount() != null ? po.getTotalAmount() : 0).sum();
        long openOrders = orders.stream().filter(po -> po.getStatus() != com.nexus.core.model.enums.PurchaseOrderStatus.CANCELLED && po.getStatus() != com.nexus.core.model.enums.PurchaseOrderStatus.CLOSED && po.getStatus() != com.nexus.core.model.enums.PurchaseOrderStatus.PAID).count();
        // scoped invoice count via the same supplier-visibility JPQL (unfiltered, then narrowed to these orders)
        var visibleInvoices = invoiceRepo.findSupplierVisibleInvoices(orgId, buyerOrgId, null, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long totalInvoices = visibleInvoices.size();
        long totalShipments = shipmentRepo.findSupplierShipmentsList(orgId).stream()
                .filter(s -> buyerOrgId == null || (s.getRetailerOrg() != null && s.getRetailerOrg().getAccountId().equals(buyerOrgId)))
                .count();
        return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "totalOrderValue", totalValue,
                "openOrders", openOrders,
                "totalInvoices", totalInvoices,
                "totalShipments", totalShipments,
                "buyerOrgId", buyerOrgId != null ? buyerOrgId : "ALL"
        ));
    }

    // Kept for reference / fallback parity checks; live reads use the scoped
    // PurchaseOrderRepo.findSupplierVisible* JPQL above expressing the same predicate.
    private boolean isSupplierOrder(com.nexus.core.model.entities.PurchaseOrder po, Long orgId) {
        if (po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getSecondaryOrg() != null && po.getPartnership().getSecondaryOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getPrimaryOrg() != null && po.getPartnership().getPrimaryOrg().getAccountId().equals(orgId)) return true;
        // fallback: if no supplierOrg mapping, allow supplier to see SENT_TO_SUPPLIER orders where buyer != supplier
        // For MVP, supplier can see orders where status is SENT_TO_SUPPLIER and not its own buyer org
        return po.getSupplierOrg() == null && po.getStatus() == com.nexus.core.model.enums.PurchaseOrderStatus.SENT_TO_SUPPLIER;
    }

    private <T> ResponseEntity<?> paginate(List<T> list, Pageable pageable, long total) {
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(list, pageable, total));
    }

    private <T> ResponseEntity<?> paginate(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<T> sub = list.subList(Math.min(start, list.size()), end);
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(sub, pageable, list.size()));
    }

    private static com.nexus.core.model.enums.PurchaseOrderStatus parsePoStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return com.nexus.core.model.enums.PurchaseOrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static com.nexus.core.model.enums.InvoiceStatus parseInvoiceStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return com.nexus.core.model.enums.InvoiceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static com.nexus.core.model.enums.ShipmentStatus parseShipmentStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return com.nexus.core.model.enums.ShipmentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
