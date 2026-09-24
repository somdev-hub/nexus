package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.ProductionCapacityCalendar;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ProductionCapacityDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.ProductionCapacityRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.ProductionCapacityService;
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

    private final ProductionCapacityRepo capacityRepo;
    private final AccountRepository accountRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createCapacity(ProductionCapacityDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        ProductionCapacityCalendar cal = modelMapper.map(dto, ProductionCapacityCalendar.class);
        cal.setCapacityId(null);
        cal.setSupplierOrg(org);
        if (cal.getUnit() == null) cal.setUnit("UNITS");
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

    private ProductionCapacityDto mapToDto(ProductionCapacityCalendar c) {
        ProductionCapacityDto dto = modelMapper.map(c, ProductionCapacityDto.class);
        if (c.getSupplierOrg() != null) dto.setSupplierOrgId(c.getSupplierOrg().getAccountId());
        if (c.getAvailableCapacity() != null && c.getAllocatedCapacity() != null) dto.setRemainingCapacity(c.getAvailableCapacity() - c.getAllocatedCapacity());
        return dto;
    }
}
