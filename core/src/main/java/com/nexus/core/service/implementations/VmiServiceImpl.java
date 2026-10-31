package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.Material;
import com.nexus.core.model.entities.ProductVariant;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.model.entities.SupplierContract;
import com.nexus.core.model.entities.VmiConfig;
import com.nexus.core.model.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.VmiConfigDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.ProductVariantRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierContractRepo;
import com.nexus.core.repository.SupplierPriceTierRepo;
import com.nexus.core.repository.VmiConfigRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.VmiService;
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
    private final SupplierCatalogRepo supplierCatalogRepo;
    private final ProductVariantRepo productVariantRepo;
    private final SupplierPriceTierRepo supplierPriceTierRepo;
    private final SupplierContractRepo supplierContractRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createVmi(VmiConfigDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account retailerOrg = accountRepo.findById(dto.getRetailerOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getRetailerOrgId()));

        boolean hasVmiContract = supplierContractRepo
                .findByAccountAccountIdAndContractType(retailerOrg.getAccountId(), SupplierContract.ContractType.VMI, Pageable.unpaged())
                .getContent().stream().anyMatch(c -> c.getStatus() == SupplierContract.ContractStatus.ACTIVE
                        && c.getSupplier() != null
                        && orgId.equals(c.getSupplier().getSupplierOrgAccountId()));
        if (!hasVmiContract) {
            throw new ValidationException("Active VMI contract required between supplier and retailer");
        }
        Material material = null;
        if (dto.getCatalogId() != null) {
            SupplierCatalog catalog = supplierCatalogRepo.findById(dto.getCatalogId()).orElse(null);
            if (catalog != null) {
                material = materialRepo.findFirstByOrgAndCatalogId(orgId, catalog.getCatalogId()).orElse(null);
                if (material == null) {
                    material = new Material();
                    material.setOrg(orgId);
                    material.setCatalogId(catalog.getCatalogId());
                    material.setName(catalog.getName());
                    material.setCode(catalog.getCode());
                    material.setCategory(catalog.getCategory() != null ? catalog.getCategory() : catalog.getFamily());
                    material.setUnit(catalog.getUnitOfMeasure());
                    material.setCurrency(catalog.getCurrency() != null ? catalog.getCurrency() : "USD");
                    material.setPricePerUnit(catalog.getBasePrice());
                    material.setSupplierOrgId(catalog.getSupplierOrg() != null ? catalog.getSupplierOrg().getAccountId() : null);
                    material = materialRepo.save(material);
                }
            }
        }
        if (material == null && dto.getMaterialId() != null) {
            material = materialRepo.findByMaterialIdAndOrg(dto.getMaterialId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", dto.getMaterialId()));
        }
        if (material == null) {
            throw new ValidationException("Catalog item is required");
        }
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
        vmi.setCatalogId(dto.getCatalogId());
        vmi.setVariantId(dto.getVariantId());
        vmi.setPriceTierId(dto.getPriceTierId());
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
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRetailerVmis(Long warehouseId, Long materialId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Page<VmiConfig> page = vmiRepo.findByRetailerOrgWithFilters(orgId, warehouseId, materialId, pageable);
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
            // Aggregate available stock across warehouses for this material
            // (VMI warehouse filter is applied when set).
            double available = stockRepo.findAll().stream()
                    .filter(s -> s.getMaterial() != null && (
                            s.getMaterial().getMaterialId().equals(vmi.getMaterial().getMaterialId())
                                    || (vmi.getCatalogId() != null && vmi.getCatalogId().equals(s.getMaterial().getCatalogId()))
                    ))
                    .filter(s -> vmi.getWarehouse() == null || (s.getWarehouse() != null && s.getWarehouse().getWarehouseId().equals(vmi.getWarehouse().getWarehouseId())))
                    .mapToDouble(s -> s.getQuantityAvailable() != null ? s.getQuantityAvailable() : 0.0)
                    .sum();
            boolean needsReplenish = vmi.getReorderPoint() != null && available <= vmi.getReorderPoint();
            double suggestedQty = vmi.getReorderQuantity() != null ? vmi.getReorderQuantity() : (vmi.getMaxLevel() != null ? vmi.getMaxLevel() - available : 0);
            return Map.<String, Object>of(
                    "vmiId", vmi.getVmiId(),
                    "materialId", vmi.getMaterial().getMaterialId(),
                    "materialName", vmi.getMaterial().getName(),
                    "catalogName", vmi.getCatalogId() != null ? supplierCatalogRepo.findById(vmi.getCatalogId()).map(SupplierCatalog::getName).orElse(null) : null,
                    "available", available,
                    "reorderPoint", vmi.getReorderPoint() != null ? vmi.getReorderPoint() : 0,
                    "needsReplenish", needsReplenish,
                    "suggestedQuantity", Math.max(0, suggestedQty)
            );
        }).filter(m -> (Boolean) m.get("needsReplenish")).collect(Collectors.toList());
        return ResponseEntity.ok(suggestions);
    }

    private VmiConfigDto mapToDto(VmiConfig vmi) {
        VmiConfigDto dto = new VmiConfigDto();
        dto.setVmiId(vmi.getVmiId());
        dto.setMinLevel(vmi.getMinLevel());
        dto.setMaxLevel(vmi.getMaxLevel());
        dto.setReorderPoint(vmi.getReorderPoint());
        dto.setReorderQuantity(vmi.getReorderQuantity());
        dto.setAutoReplenish(vmi.getAutoReplenish());
        dto.setLastReplenishedAt(vmi.getLastReplenishedAt());
        dto.setIsActive(vmi.getIsActive());
        dto.setCreatedAt(vmi.getCreatedAt());
        dto.setUpdatedAt(vmi.getUpdatedAt());
        dto.setCatalogId(vmi.getCatalogId());
        dto.setVariantId(vmi.getVariantId());
        dto.setPriceTierId(vmi.getPriceTierId());
        if (vmi.getSupplierOrg() != null) {
            dto.setSupplierOrgId(vmi.getSupplierOrg().getAccountId());
            dto.setSupplierOrgName(vmi.getSupplierOrg().getName());
        }
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
        if (vmi.getCatalogId() != null) {
            SupplierCatalog cat = supplierCatalogRepo.findById(vmi.getCatalogId()).orElse(null);
            if (cat != null) dto.setCatalogName(cat.getName());
        }
        if (vmi.getVariantId() != null) {
            ProductVariant v = productVariantRepo.findById(vmi.getVariantId()).orElse(null);
            if (v != null) dto.setVariantValue(v.getVariantValue());
        }
        if (vmi.getPriceTierId() != null) {
            com.nexus.core.model.entities.SupplierPriceTier t = supplierPriceTierRepo.findById(vmi.getPriceTierId()).orElse(null);
            if (t != null) dto.setPriceTierName(t.getTierName());
        }
        return dto;
    }
}
