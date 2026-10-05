package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.model.entities.SupplierDigitalAsset;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ServiceLevelException;
import com.nexus.core.payload.SupplierDigitalAssetDto;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierDigitalAssetRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierDigitalAssetService;
import com.nexus.core.utils.RestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierDigitalAssetServiceImpl implements SupplierDigitalAssetService {

    private final SupplierDigitalAssetRepo assetRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final ModelMapper modelMapper;
    private final RestService restService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "application/pdf");

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
    public ResponseEntity<?> uploadAssetFile(Long id, org.springframework.web.multipart.MultipartFile file,
            String authToken) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierDigitalAsset asset = assetRepo.findByAssetIdAndCatalogSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierDigitalAsset", "assetId", id));
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "File is required"));
        }
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        // Allow jpg/png/pdf only (extension fallback when browsers omit the type).
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean allowed = ALLOWED_CONTENT_TYPES.contains(contentType)
                || filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".pdf");
        if (!allowed) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Only JPG, PNG and PDF files are allowed"));
        }
        // A digital asset is meaningless without its file: if this upload
        // fails and the asset has no DMS url yet, the row is removed so a
        // file-less entry never persists. A re-upload over an existing file
        // keeps the old row (and its url) on failure.
        boolean hadUrl = asset.getDmsDocumentUrl() != null && !asset.getDmsDocumentUrl().isBlank();
        ResponseEntity<String> response;
        try {
            response = restService.uploadToDmsOrg(
                    file,
                    file.getOriginalFilename(),
                    orgId,
                    "Supplier digital asset for catalog: "
                            + (asset.getCatalog() != null ? asset.getCatalog().getCatalogId() : null),
                    "SUPPLIER_ASSET",
                    "SUPPLIER",
                    authToken,
                    orgId);
        } catch (Exception e) {
            log.error("Error uploading asset file to DMS", e);
            discardUrlLessAsset(asset.getAssetId(), hadUrl);
            throw new ServiceLevelException("DMS", "DMS upload failed: " + e.getMessage(), "uploadAssetFile",
                    e.getClass().getSimpleName(), "Supplier digital asset file upload");
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            discardUrlLessAsset(asset.getAssetId(), hadUrl);
            throw new ServiceLevelException("DMS",
                    "DMS upload failed: " + (response.getBody() != null ? response.getBody() : response.getStatusCode()),
                    "uploadAssetFile", "DmsUploadFailed", "Supplier digital asset file upload");
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(response.getBody());
            String documentUrl = node.path("documentUrl").asText(null);
            if (documentUrl == null || documentUrl.isBlank()) {
                discardUrlLessAsset(asset.getAssetId(), hadUrl);
                throw new ServiceLevelException("DMS", "DMS upload failed: no documentUrl in DMS response",
                        "uploadAssetFile", "DmsUploadFailed", "Supplier digital asset file upload");
            }
            asset.setDmsDocumentId(node.path("dmsId").asText(null));
            asset.setDmsDocumentUrl(documentUrl);
            if (asset.getFileName() == null || asset.getFileName().isBlank()) {
                asset.setFileName(file.getOriginalFilename());
            }
            return ResponseEntity.ok(mapToDto(assetRepo.save(asset)));
        } catch (ServiceLevelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing DMS upload response", e);
            discardUrlLessAsset(asset.getAssetId(), hadUrl);
            throw new ServiceLevelException("DMS", "DMS upload failed: " + e.getMessage(), "uploadAssetFile",
                    e.getClass().getSimpleName(), "Supplier digital asset file upload");
        }
    }

    /**
     * Remove an asset row that would otherwise persist without a file.
     * Rows that already carry a DMS url (re-uploads) are left untouched.
     * Runs in its own transaction: the caller throws afterwards, which rolls
     * back the caller's transaction — a shared transaction would undo this
     * delete as well.
     */
    private void discardUrlLessAsset(Long assetId, boolean hadUrl) {
        if (hadUrl || assetId == null) {
            return;
        }
        try {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            new TransactionTemplate(transactionManager, def)
                    .executeWithoutResult(status -> assetRepo.deleteById(assetId));
        } catch (Exception deleteError) {
            log.error("Failed to discard file-less digital asset {}", assetId, deleteError);
        }
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
