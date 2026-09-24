package com.nexus.core.service.impl;

import com.nexus.core.entities.Material;
import com.nexus.core.entities.ProductVariant;
import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ProductVariantDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.ProductVariantRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepo variantRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final MaterialRepo materialRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createVariant(ProductVariantDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(dto.getCatalogId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", dto.getCatalogId()));
        ProductVariant variant = modelMapper.map(dto, ProductVariant.class);
        variant.setVariantId(null);
        variant.setCatalog(catalog);
        if (dto.getBomMaterialId() != null) {
            Material m = materialRepo.findByMaterialIdAndOrg(dto.getBomMaterialId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", dto.getBomMaterialId()));
            variant.setBomMaterial(m);
        }
        ProductVariant saved = variantRepo.save(variant);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getVariant(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductVariant v = variantRepo.findByVariantIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "variantId", id));
        return ResponseEntity.ok(mapToDto(v));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllVariants(Long catalogId, String variantType, Long bomMaterialId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductVariant.VariantType vt = null;
        if (variantType != null && !variantType.isBlank()) {
            try { vt = ProductVariant.VariantType.valueOf(variantType.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid variantType: " + variantType)); }
        }
        Page<ProductVariant> page = variantRepo.findByOrgWithFilters(orgId, catalogId, vt, bomMaterialId, pageable);
        Page<ProductVariantDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateVariant(Long id, ProductVariantDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductVariant v = variantRepo.findByVariantIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "variantId", id));
        if (dto.getVariantType() != null) v.setVariantType(dto.getVariantType());
        if (dto.getVariantValue() != null) v.setVariantValue(dto.getVariantValue());
        if (dto.getSkuSuffix() != null) v.setSkuSuffix(dto.getSkuSuffix());
        if (dto.getPriceAdjustment() != null) v.setPriceAdjustment(dto.getPriceAdjustment());
        if (dto.getQuantityAvailable() != null) v.setQuantityAvailable(dto.getQuantityAvailable());
        if (dto.getBomMaterialId() != null) {
            Material m = materialRepo.findByMaterialIdAndOrg(dto.getBomMaterialId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", dto.getBomMaterialId()));
            v.setBomMaterial(m);
        }
        if (dto.getIsActive() != null) v.setIsActive(dto.getIsActive());
        ProductVariant saved = variantRepo.save(v);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteVariant(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ProductVariant v = variantRepo.findByVariantIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "variantId", id));
        variantRepo.delete(v);
        return ResponseEntity.noContent().build();
    }

    private ProductVariantDto mapToDto(ProductVariant v) {
        ProductVariantDto dto = modelMapper.map(v, ProductVariantDto.class);
        if (v.getCatalog() != null) {
            dto.setCatalogId(v.getCatalog().getCatalogId());
            dto.setCatalogName(v.getCatalog().getName());
        }
        if (v.getBomMaterial() != null) {
            dto.setBomMaterialId(v.getBomMaterial().getMaterialId());
            dto.setBomMaterialName(v.getBomMaterial().getName());
        }
        return dto;
    }
}
