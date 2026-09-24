package com.nexus.core.service.impl;

import com.nexus.core.entities.Invoice;
import com.nexus.core.entities.InvoiceStatus;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierAccountHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierAccountHealthServiceImpl implements SupplierAccountHealthService {

    private final PurchaseOrderRepo poRepo;
    private final InvoiceRepo invoiceRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAccountHealth(Long buyerOrgId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<PurchaseOrder> orders = poRepo.findAll().stream()
                .filter(po -> po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId))
                .filter(po -> po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId().equals(buyerOrgId))
                .collect(Collectors.toList());
        List<Invoice> invoices = invoiceRepo.findAll().stream()
                .filter(inv -> inv.getPurchaseOrder() != null && orders.stream().anyMatch(po -> po.getPurchaseOrderId().equals(inv.getPurchaseOrder().getPurchaseOrderId())))
                .collect(Collectors.toList());

        long totalOrders = orders.size();
        double totalOrderValue = orders.stream().mapToDouble(po -> po.getTotalAmount() != null ? po.getTotalAmount() : 0).sum();
        double avgOrderValue = totalOrders > 0 ? totalOrderValue / totalOrders : 0;

        long paidInvoices = invoices.stream().filter(inv -> inv.getStatus() == InvoiceStatus.PAID).count();
        long pendingInvoices = invoices.stream().filter(inv -> inv.getStatus() == InvoiceStatus.PENDING_APPROVAL || inv.getStatus() == InvoiceStatus.APPROVED).count();
        long overdueInvoices = invoices.stream().filter(inv -> inv.getStatus() == InvoiceStatus.APPROVED).count(); // simplified

        double paymentBehaviorScore = invoices.isEmpty() ? 100.0 : (paidInvoices * 100.0 / invoices.size());
        String creditUtilization = totalOrderValue > 100000 ? "HIGH" : totalOrderValue > 50000 ? "MEDIUM" : "LOW";

        // order pattern: orders per month approximation
        var recentOrders = orders.stream().filter(po -> po.getCreatedAt() != null && po.getCreatedAt().after(new java.sql.Timestamp(System.currentTimeMillis() - 30L*24*60*60*1000))).count();

        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("buyerOrgId", buyerOrgId);
        map.put("totalOrders", totalOrders);
        map.put("totalOrderValue", totalOrderValue);
        map.put("avgOrderValue", avgOrderValue);
        map.put("recentOrdersLast30Days", recentOrders);
        map.put("totalInvoices", invoices.size());
        map.put("paidInvoices", paidInvoices);
        map.put("pendingInvoices", pendingInvoices);
        map.put("overdueInvoices", overdueInvoices);
        map.put("paymentBehaviorScore", paymentBehaviorScore);
        map.put("creditUtilization", creditUtilization);
        return ResponseEntity.ok(map);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllAccountHealths() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var buyerIds = poRepo.findAll().stream()
                .filter(po -> po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId))
                .filter(po -> po.getBuyerOrg() != null)
                .map(po -> po.getBuyerOrg().getAccountId())
                .distinct()
                .collect(Collectors.toList());
        List<Map<String, Object>> healths = buyerIds.stream().map(id -> {
            var resp = getAccountHealth(id);
            return (Map<String, Object>) resp.getBody();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(healths);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getHealthSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var allHealth = (List<Map<String, Object>>) getAllAccountHealths().getBody();
        if (allHealth == null || allHealth.isEmpty()) {
            return ResponseEntity.ok(Map.of("totalCustomers", 0, "avgPaymentScore", 0));
        }
        double avgScore = allHealth.stream().mapToDouble(m -> ((Number)m.get("paymentBehaviorScore")).doubleValue()).average().orElse(0);
        return ResponseEntity.ok(Map.of("totalCustomers", allHealth.size(), "avgPaymentScore", avgScore));
    }
}
