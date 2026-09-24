package com.nexus.core.service.impl;

import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.entities.SupplierDigitalAsset;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.SupplierDigitalAssetDto;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierDigitalAssetRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierDigitalAssetService;
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
public class SupplierDigitalAssetServiceImpl implements SupplierDigitalAssetService {

    private final SupplierDigitalAssetRepo assetRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ResponseEntity<?> createAsset(SupplierDigitalAssetDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(dto.getCatalogId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", dto.getCatalogId()));
        SupplierDigitalAsset asset = modelMapper.map(dto, SupplierDigitalAsset.class);
        asset.setAssetId(null);
        asset.setCatalog(catalog);
        if (asset.getVersion() == null) asset.setVersion(1);
        SupplierDigitalAsset saved = assetRepo.save(asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAsset(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierDigitalAsset asset = assetRepo.findByAssetIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierDigitalAsset", "assetId", id));
        return ResponseEntity.ok(mapToDto(asset));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllAssets(Long catalogId, String assetType, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierDigitalAsset.AssetType at = null;
        if (assetType != null && !assetType.isBlank()) {
            try { at = SupplierDigitalAsset.AssetType.valueOf(assetType.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid assetType: " + assetType)); }
        }
        Page<SupplierDigitalAsset> page = assetRepo.findByOrgWithFilters(orgId, catalogId, at, pageable);
        Page<SupplierDigitalAssetDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateAsset(Long id, SupplierDigitalAssetDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierDigitalAsset asset = assetRepo.findByAssetIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierDigitalAsset", "assetId", id));
        if (dto.getAssetType() != null) asset.setAssetType(dto.getAssetType());
        if (dto.getFileName() != null) asset.setFileName(dto.getFileName());
        if (dto.getDmsDocumentId() != null) asset.setDmsDocumentId(dto.getDmsDocumentId());
        if (dto.getDmsDocumentUrl() != null) asset.setDmsDocumentUrl(dto.getDmsDocumentUrl());
        if (dto.getVersion() != null) asset.setVersion(dto.getVersion());
        SupplierDigitalAsset saved = assetRepo.save(asset);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteAsset(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierDigitalAsset asset = assetRepo.findByAssetIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierDigitalAsset", "assetId", id));
        assetRepo.delete(asset);
        return ResponseEntity.noContent().build();
    }

    private SupplierDigitalAssetDto mapToDto(SupplierDigitalAsset a) {
        SupplierDigitalAssetDto dto = modelMapper.map(a, SupplierDigitalAssetDto.class);
        if (a.getCatalog() != null) {
            dto.setCatalogId(a.getCatalog().getCatalogId());
            dto.setCatalogName(a.getCatalog().getName());
        }
        return dto;
    }
}
