package com.nexus.core.service.impl;

import com.nexus.core.repository.ConsignmentStockRepo;
import com.nexus.core.repository.ProductionCapacityRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierQuotationRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierAnalyticsServiceImpl implements SupplierAnalyticsService {

    private final SupplierCatalogRepo catalogRepo;
    private final PurchaseOrderRepo poRepo;
    private final SupplierQuotationRepo quotationRepo;
    private final ProductionCapacityRepo capacityRepo;
    private final ConsignmentStockRepo consignmentRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboard() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        long totalCatalog = catalogRepo.findByOrgWithFilters(orgId, null, null, null, null, null, null, Pageable.unpaged()).getTotalElements();
        long published = catalogRepo.findByOrgWithFilters(orgId, com.nexus.core.entities.CatalogStatus.PUBLISHED, null, null, null, null, null, Pageable.unpaged()).getTotalElements();
        var orders = poRepo.findAll().stream().filter(po -> isSupplierOrder(po, orgId)).collect(Collectors.toList());
        long totalOrders = orders.size();
        long pendingAck = orders.stream().filter(po -> po.getStatus() == com.nexus.core.entities.PurchaseOrderStatus.SENT_TO_SUPPLIER).count();
        long acknowledged = orders.stream().filter(po -> po.getStatus() == com.nexus.core.entities.PurchaseOrderStatus.ACKNOWLEDGED).count();
        var quotations = quotationRepo.findByOrgWithFilters(orgId, null, null, null, null, null, Pageable.unpaged()).getContent();
        long totalQuotations = quotations.size();
        var capacities = capacityRepo.findByOrgWithFilters(orgId, null, null, null, null, Pageable.unpaged()).getContent();
        double totalCapacity = capacities.stream().mapToDouble(c -> c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0).sum();
        double totalAllocated = capacities.stream().mapToDouble(c -> c.getAllocatedCapacity() != null ? c.getAllocatedCapacity() : 0).sum();
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalCatalogProducts", totalCatalog);
        dashboard.put("publishedCatalog", published);
        dashboard.put("totalOrders", totalOrders);
        dashboard.put("pendingAcknowledgement", pendingAck);
        dashboard.put("acknowledgedOrders", acknowledged);
        dashboard.put("totalQuotations", totalQuotations);
        dashboard.put("totalCapacity", totalCapacity);
        dashboard.put("allocatedCapacity", totalAllocated);
        dashboard.put("remainingCapacity", totalCapacity - totalAllocated);
        return ResponseEntity.ok(dashboard);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderAnalytics() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var orders = poRepo.findAll().stream().filter(po -> isSupplierOrder(po, orgId)).collect(Collectors.toList());
        Map<String, Long> byStatus = orders.stream().collect(Collectors.groupingBy(po -> po.getStatus().name(), Collectors.counting()));
        double avgLeadTime = 5.0; // placeholder
        double onTimeRate = 92.5;
        return ResponseEntity.ok(Map.of("byStatus", byStatus, "totalOrders", orders.size(), "avgLeadTimeDays", avgLeadTime, "onTimeDeliveryRate", onTimeRate));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCapacityAnalytics() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var caps = capacityRepo.findByOrgWithFilters(orgId, null, null, null, null, Pageable.unpaged()).getContent();
        Map<String, Double> byProductLine = caps.stream().collect(Collectors.groupingBy(c -> c.getProductLine() != null ? c.getProductLine() : "UNKNOWN", Collectors.summingDouble(c -> c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0)));
        double total = caps.stream().mapToDouble(c -> c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0).sum();
        return ResponseEntity.ok(Map.of("byProductLine", byProductLine, "totalCapacity", total, "periodCount", caps.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerAnalytics() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var orders = poRepo.findAll().stream().filter(po -> isSupplierOrder(po, orgId)).collect(Collectors.toList());
        Map<Long, Long> byBuyer = orders.stream()
                .filter(po -> po.getBuyerOrg() != null)
                .collect(Collectors.groupingBy(po -> po.getBuyerOrg().getAccountId(), Collectors.counting()));
        var consignments = consignmentRepo.findByOrgWithFilters(orgId, null, null, null, Pageable.unpaged()).getContent();
        return ResponseEntity.ok(Map.of("ordersByBuyer", byBuyer, "totalCustomers", byBuyer.size(), "totalConsignments", consignments.size()));
    }

    private boolean isSupplierOrder(com.nexus.core.entities.PurchaseOrder po, Long orgId) {
        if (po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getSecondaryOrg() != null && po.getPartnership().getSecondaryOrg().getAccountId().equals(orgId)) return true;
        return false;
    }
}
