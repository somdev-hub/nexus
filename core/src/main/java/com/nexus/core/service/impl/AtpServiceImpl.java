package com.nexus.core.service.impl;

import com.nexus.core.entities.ProductionCapacityCalendar;
import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.repository.ProductionCapacityRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.AtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtpServiceImpl implements AtpService {

    private final SupplierCatalogRepo catalogRepo;
    private final ProductionCapacityRepo capacityRepo;
    private final StockRepo stockRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAvailableToPromise(Long catalogId, Double requestedQuantity) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(catalogId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", catalogId));

        // inventory available: sum of stock available for supplier org warehouses
        double inventoryAvailable = stockRepo.findAll().stream()
                .filter(s -> s.getWarehouse() != null && s.getWarehouse().getOrg() != null && s.getWarehouse().getOrg().equals(orgId))
                .mapToDouble(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0)
                .sum();

        // capacity available for product line matching catalog category/family
        double capacityAvailable = capacityRepo.findByOrgWithFilters(orgId, catalog.getCategory(), null, null, null, Pageable.unpaged()).getContent().stream()
                .mapToDouble(c -> {
                    double avail = c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0;
                    double alloc = c.getAllocatedCapacity() != null ? c.getAllocatedCapacity() : 0;
                    return Math.max(0, avail - alloc);
                }).sum();

        // fallback if no productLine capacity, sum all
        if (capacityAvailable == 0) {
            capacityAvailable = capacityRepo.findByOrgWithFilters(orgId, null, null, null, null, Pageable.unpaged()).getContent().stream()
                    .mapToDouble(c -> Math.max(0, (c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0) - (c.getAllocatedCapacity() != null ? c.getAllocatedCapacity() : 0)))
                    .sum();
        }

        double atp = inventoryAvailable + capacityAvailable;
        boolean canFulfill = requestedQuantity == null || atp >= requestedQuantity;
        double shortage = requestedQuantity != null ? Math.max(0, requestedQuantity - atp) : 0;

        return ResponseEntity.ok(Map.of(
                "catalogId", catalogId,
                "catalogName", catalog.getName(),
                "inventoryAvailable", inventoryAvailable,
                "capacityAvailable", capacityAvailable,
                "atpQuantity", atp,
                "requestedQuantity", requestedQuantity != null ? requestedQuantity : 0,
                "canFulfill", canFulfill,
                "shortage", shortage
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAtpForProductLine(String productLine, Date date) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var capacities = capacityRepo.findByOrgWithFilters(orgId, productLine, null, date, date, Pageable.unpaged()).getContent();
        if (capacities.isEmpty()) {
            capacities = capacityRepo.findByOrgWithFilters(orgId, productLine, null, null, null, Pageable.unpaged()).getContent();
        }
        double totalAtp = capacities.stream()
                .mapToDouble(c -> Math.max(0, (c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0) - (c.getAllocatedCapacity() != null ? c.getAllocatedCapacity() : 0)))
                .sum();
        double inventoryAvailable = stockRepo.findAll().stream()
                .filter(s -> s.getWarehouse() != null && s.getWarehouse().getOrg() != null && s.getWarehouse().getOrg().equals(orgId))
                .mapToDouble(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0)
                .sum();
        return ResponseEntity.ok(Map.of(
                "productLine", productLine != null ? productLine : "ALL",
                "date", date != null ? date.toString() : "N/A",
                "capacityAtp", totalAtp,
                "inventoryAvailable", inventoryAvailable,
                "totalAtp", totalAtp + inventoryAvailable
        ));
    }
}
