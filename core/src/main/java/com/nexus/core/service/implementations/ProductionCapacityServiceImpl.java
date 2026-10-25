package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.ProductVariant;
import com.nexus.core.model.entities.ProductionCapacityCalendar;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.ProductionCapacityDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.ProductVariantRepo;
import com.nexus.core.repository.ProductionCapacityRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.ProductionCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionCapacityServiceImpl implements ProductionCapacityService {

    private static final double EPSILON = 0.000001;

    private final ProductionCapacityRepo capacityRepo;
    private final AccountRepository accountRepo;
    private final SupplierCatalogRepo supplierCatalogRepo;
    private final ProductVariantRepo productVariantRepo;
    private final com.nexus.core.service.interfaces.CapacityAvailabilityService capacityAvailability;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createCapacity(ProductionCapacityDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        if (dto.getCatalogId() == null) {
            throw new ValidationException("Catalog item is required for a capacity period");
        }
        if (dto.getPeriodStart() != null && dto.getPeriodStart().toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new ValidationException("Period start cannot be before today");
        }
        if (dto.getPeriodEnd() != null && dto.getPeriodEnd().toLocalDate().isBefore(java.time.LocalDate.now())) {
            throw new ValidationException("Period end cannot be before today");
        }
        SupplierCatalog catalog = resolveCatalog(dto.getCatalogId(), orgId);
        Long variantId = resolveVariant(dto.getVariantId(), catalog);
        ProductionCapacityCalendar cal = modelMapper.map(dto, ProductionCapacityCalendar.class);
        cal.setCapacityId(null);
        cal.setSupplierOrg(org);
        cal.setCatalog(catalog);
        cal.setVariantId(variantId);
        cal.setProductLine(null); // superseded by the catalog reference
        // The unit is always the catalog item's unit of measure.
        cal.setUnit(catalog.getUnitOfMeasure() != null && !catalog.getUnitOfMeasure().isBlank()
                ? catalog.getUnitOfMeasure()
                : "UNITS");
        if (cal.getAvailableCapacity() == null) cal.setAvailableCapacity(0.0);
        if (cal.getAllocatedCapacity() == null) cal.setAllocatedCapacity(0.0);
        ProductionCapacityCalendar saved = capacityRepo.save(cal);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCapacity(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductionCapacityCalendar cal = capacityRepo.findByCapacityIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionCapacity", "capacityId", id));
        return ResponseEntity.ok(mapToDto(cal));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCapacities(String productLine, String shift, java.sql.Date periodStart, java.sql.Date periodEnd, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Page<ProductionCapacityCalendar> page = capacityRepo.findByOrgWithFilters(orgId, productLine, shift, periodStart, periodEnd, pageable);
        Page<ProductionCapacityDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateCapacity(Long id, ProductionCapacityDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductionCapacityCalendar cal = capacityRepo.findByCapacityIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionCapacity", "capacityId", id));
        if (dto.getCatalogId() != null) {
            SupplierCatalog catalog = resolveCatalog(dto.getCatalogId(), orgId);
            cal.setCatalog(catalog);
            cal.setVariantId(resolveVariant(dto.getVariantId(), catalog));
            // Keep the unit in sync with the catalog item's unit of measure.
            cal.setUnit(catalog.getUnitOfMeasure() != null && !catalog.getUnitOfMeasure().isBlank()
                    ? catalog.getUnitOfMeasure()
                    : cal.getUnit());
        } else if (dto.getVariantId() != null) {
            // variant without catalog is not meaningful
            throw new ValidationException("A catalog item is required when pinning a variant");
        }
        if (dto.getProductLine() != null) cal.setProductLine(dto.getProductLine());
        if (dto.getPeriodStart() != null) cal.setPeriodStart(dto.getPeriodStart());
        if (dto.getPeriodEnd() != null) cal.setPeriodEnd(dto.getPeriodEnd());
        if (dto.getShift() != null) cal.setShift(dto.getShift());
        if (dto.getAvailableCapacity() != null) cal.setAvailableCapacity(dto.getAvailableCapacity());
        if (dto.getAllocatedCapacity() != null) cal.setAllocatedCapacity(dto.getAllocatedCapacity());
        if (dto.getUnit() != null) cal.setUnit(dto.getUnit());
        if (dto.getNotes() != null) cal.setNotes(dto.getNotes());
        ProductionCapacityCalendar saved = capacityRepo.save(cal);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteCapacity(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductionCapacityCalendar cal = capacityRepo.findByCapacityIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionCapacity", "capacityId", id));
        capacityRepo.delete(cal);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCapacitySummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var all = capacityRepo.findByOrgWithFilters(orgId, null, null, null, null, Pageable.unpaged()).getContent();
        double totalAvail = all.stream().mapToDouble(c -> c.getAvailableCapacity() != null ? c.getAvailableCapacity() : 0).sum();
        double totalAlloc = all.stream().mapToDouble(c -> c.getAllocatedCapacity() != null ? c.getAllocatedCapacity() : 0).sum();
        double totalRemaining = totalAvail - totalAlloc;
        return ResponseEntity.ok(Map.of("totalAvailable", totalAvail, "totalAllocated", totalAlloc, "totalRemaining", totalRemaining, "periodCount", all.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> checkAvailability(Long catalogId, Long variantId, Double quantity,
            java.sql.Date periodStart, java.sql.Date periodEnd) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = resolveCatalog(catalogId, orgId);
        Long vid = resolveVariant(variantId, catalog);
        double available = capacityAvailability.remainingCapacity(orgId, catalogId, vid, periodStart, periodEnd);
        boolean sufficient = quantity == null || available + EPSILON >= quantity;
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("catalogId", catalogId);
        body.put("variantId", vid);
        body.put("catalogName", catalog.getName());
        body.put("periodStart", periodStart != null ? periodStart.toString() : null);
        body.put("periodEnd", periodEnd != null ? periodEnd.toString() : null);
        body.put("requested", quantity != null ? quantity : 0.0);
        body.put("available", available);
        body.put("sufficient", sufficient);
        return ResponseEntity.ok(body);
    }

    private ProductionCapacityDto mapToDto(ProductionCapacityCalendar c) {
        ProductionCapacityDto dto = modelMapper.map(c, ProductionCapacityDto.class);
        if (c.getSupplierOrg() != null) dto.setSupplierOrgId(c.getSupplierOrg().getAccountId());
        if (c.getCatalog() != null) {
            dto.setCatalogId(c.getCatalog().getCatalogId());
            dto.setCatalogName(c.getCatalog().getName());
        }
        if (c.getVariantId() != null) {
            dto.setVariantId(c.getVariantId());
            productVariantRepo.findById(c.getVariantId()).ifPresent(v -> dto.setVariantLabel(
                    v.getVariantValue() + (v.getVariantType() != null ? " (" + v.getVariantType() + ")" : "")));
        }
        if (c.getAvailableCapacity() != null && c.getAllocatedCapacity() != null) dto.setRemainingCapacity(c.getAvailableCapacity() - c.getAllocatedCapacity());
        return dto;
    }

    /** Loads a catalog item and asserts it belongs to the caller's org. */
    private SupplierCatalog resolveCatalog(Long catalogId, Long orgId) {
        return supplierCatalogRepo.findByCatalogIdAndSupplierOrgAccountId(catalogId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", catalogId));
    }

    /** Validates an optional variant belongs to the given catalog. */
    private Long resolveVariant(Long variantId, SupplierCatalog catalog) {
        if (variantId == null) {
            return null;
        }
        ProductVariant variant = productVariantRepo.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "variantId", variantId));
        if (variant.getCatalog() == null || !variant.getCatalog().getCatalogId().equals(catalog.getCatalogId())) {
            throw new ValidationException("Variant " + variantId + " does not belong to catalog item "
                    + catalog.getCatalogId());
        }
        return variantId;
    }
}
