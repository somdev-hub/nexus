package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.Material;
import com.nexus.core.entities.VmiConfig;
import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.VmiConfigDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.VmiConfigRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.VmiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VmiServiceImpl implements VmiService {

    private final VmiConfigRepo vmiRepo;
    private final AccountRepository accountRepo;
    private final WarehouseRepo warehouseRepo;
    private final MaterialRepo materialRepo;
    private final StockRepo stockRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createVmi(VmiConfigDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account retailerOrg = accountRepo.findById(dto.getRetailerOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getRetailerOrgId()));
        Material material = materialRepo.findByMaterialIdAndOrg(dto.getMaterialId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", dto.getMaterialId()));
        Warehouse warehouse = null;
        if (dto.getWarehouseId() != null) {
            warehouse = warehouseRepo.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", dto.getWarehouseId()));
        }
        VmiConfig vmi = new VmiConfig();
        vmi.setSupplierOrg(supplierOrg);
        vmi.setRetailerOrg(retailerOrg);
        vmi.setWarehouse(warehouse);
        vmi.setMaterial(material);
        vmi.setMinLevel(dto.getMinLevel());
        vmi.setMaxLevel(dto.getMaxLevel());
        vmi.setReorderPoint(dto.getReorderPoint());
        vmi.setReorderQuantity(dto.getReorderQuantity());
        vmi.setAutoReplenish(dto.getAutoReplenish() != null ? dto.getAutoReplenish() : false);
        VmiConfig saved = vmiRepo.save(vmi);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getVmi(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        VmiConfig vmi = vmiRepo.findByVmiIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("VmiConfig", "vmiId", id));
        return ResponseEntity.ok(mapToDto(vmi));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllVmis(Long retailerOrgId, Long warehouseId, Long materialId, Boolean autoReplenish, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Page<VmiConfig> page = vmiRepo.findByOrgWithFilters(orgId, retailerOrgId, warehouseId, materialId, autoReplenish, pageable);
        Page<VmiConfigDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateVmi(Long id, VmiConfigDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        VmiConfig vmi = vmiRepo.findByVmiIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("VmiConfig", "vmiId", id));
        if (dto.getMinLevel() != null) vmi.setMinLevel(dto.getMinLevel());
        if (dto.getMaxLevel() != null) vmi.setMaxLevel(dto.getMaxLevel());
        if (dto.getReorderPoint() != null) vmi.setReorderPoint(dto.getReorderPoint());
        if (dto.getReorderQuantity() != null) vmi.setReorderQuantity(dto.getReorderQuantity());
        if (dto.getAutoReplenish() != null) vmi.setAutoReplenish(dto.getAutoReplenish());
        if (dto.getIsActive() != null) vmi.setIsActive(dto.getIsActive());
        VmiConfig saved = vmiRepo.save(vmi);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteVmi(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        VmiConfig vmi = vmiRepo.findByVmiIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("VmiConfig", "vmiId", id));
        vmiRepo.delete(vmi);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> triggerReplenishment(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        VmiConfig vmi = vmiRepo.findByVmiIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("VmiConfig", "vmiId", id));
        vmi.setLastReplenishedAt(Timestamp.valueOf(LocalDateTime.now()));
        VmiConfig saved = vmiRepo.save(vmi);
        return ResponseEntity.ok(Map.of("vmiId", saved.getVmiId(), "lastReplenishedAt", saved.getLastReplenishedAt(), "message", "Replenishment triggered"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getReplenishmentSuggestions() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var vmis = vmiRepo.findByOrgWithFilters(orgId, null, null, null, null, Pageable.unpaged()).getContent();
        List<Map<String, Object>> suggestions = vmis.stream().map(vmi -> {
            // find stock for material+warehouse
            var stockOpt = stockRepo.findAll().stream()
                    .filter(s -> s.getMaterial() != null && s.getMaterial().getMaterialId().equals(vmi.getMaterial().getMaterialId()))
                    .filter(s -> vmi.getWarehouse() == null || (s.getWarehouse() != null && s.getWarehouse().getWarehouseId().equals(vmi.getWarehouse().getWarehouseId())))
                    .findFirst();
            double available = stockOpt.map(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0.0).orElse(0.0);
            boolean needsReplenish = vmi.getReorderPoint() != null && available <= vmi.getReorderPoint();
            double suggestedQty = vmi.getReorderQuantity() != null ? vmi.getReorderQuantity() : (vmi.getMaxLevel() != null ? vmi.getMaxLevel() - available : 0);
            return Map.<String, Object>of(
                    "vmiId", vmi.getVmiId(),
                    "materialId", vmi.getMaterial().getMaterialId(),
                    "available", available,
                    "reorderPoint", vmi.getReorderPoint() != null ? vmi.getReorderPoint() : 0,
                    "needsReplenish", needsReplenish,
                    "suggestedQuantity", Math.max(0, suggestedQty)
            );
        }).filter(m -> (Boolean) m.get("needsReplenish")).collect(Collectors.toList());
        return ResponseEntity.ok(suggestions);
    }

    private VmiConfigDto mapToDto(VmiConfig vmi) {
        VmiConfigDto dto = modelMapper.map(vmi, VmiConfigDto.class);
        if (vmi.getSupplierOrg() != null) dto.setSupplierOrgId(vmi.getSupplierOrg().getAccountId());
        if (vmi.getRetailerOrg() != null) {
            dto.setRetailerOrgId(vmi.getRetailerOrg().getAccountId());
            dto.setRetailerOrgName(vmi.getRetailerOrg().getName());
        }
        if (vmi.getWarehouse() != null) {
            dto.setWarehouseId(vmi.getWarehouse().getWarehouseId());
            dto.setWarehouseCode(vmi.getWarehouse().getCode());
        }
        if (vmi.getMaterial() != null) {
            dto.setMaterialId(vmi.getMaterial().getMaterialId());
            dto.setMaterialName(vmi.getMaterial().getName());
        }
        return dto;
    }
}
