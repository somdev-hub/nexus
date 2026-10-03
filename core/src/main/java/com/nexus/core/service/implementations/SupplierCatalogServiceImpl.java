package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.enums.CatalogAccessLevel;
import com.nexus.core.model.enums.CatalogStatus;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.CatalogBrowseDto;
import com.nexus.core.payload.SupplierCatalogDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierCatalogServiceImpl implements SupplierCatalogService {

    private final SupplierCatalogRepo catalogRepo;
    private final AccountRepository accountRepo;
    private final ModelMapper modelMapper;

    private static final Map<CatalogStatus, Set<CatalogStatus>> ALLOWED = Map.of(
            CatalogStatus.DRAFT, Set.of(CatalogStatus.PUBLISHED, CatalogStatus.ARCHIVED),
            CatalogStatus.PUBLISHED, Set.of(CatalogStatus.ARCHIVED, CatalogStatus.DRAFT),
            CatalogStatus.ARCHIVED, Set.of(CatalogStatus.DRAFT)
    );

    @Override
    @Transactional
    public ResponseEntity<?> createCatalog(SupplierCatalogDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));

        if (catalogRepo.existsByCodeAndSupplierOrgAccountId(dto.getCode(), orgId)) {
            throw new ValidationException("Catalog code already exists for this organization");
        }
        if (dto.getSku() != null && catalogRepo.existsBySkuAndSupplierOrgAccountId(dto.getSku(), orgId)) {
            throw new ValidationException("SKU already exists for this organization");
        }

        SupplierCatalog catalog = modelMapper.map(dto, SupplierCatalog.class);
        catalog.setCatalogId(null);
        catalog.setSupplierOrg(org);
        if (catalog.getStatus() == null) catalog.setStatus(CatalogStatus.DRAFT);
        if (catalog.getAccessLevel() == null) catalog.setAccessLevel(CatalogAccessLevel.PRIVATE);
        if (catalog.getCurrency() == null) catalog.setCurrency("USD");
        catalog.setIsPublished(false);
        SupplierCatalog saved = catalogRepo.save(catalog);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCatalog(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", id));
        return ResponseEntity.ok(mapToDto(catalog));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCatalogs(String status, String category, String family, String accessLevel, Boolean isPublished, String search, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        CatalogStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = CatalogStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status)); }
        }
        CatalogAccessLevel al = null;
        if (accessLevel != null && !accessLevel.isBlank()) {
            try { al = CatalogAccessLevel.valueOf(accessLevel.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid accessLevel: " + accessLevel)); }
        }
        Page<SupplierCatalog> page = catalogRepo.findByOrgWithFilters(orgId, st, category, family, al, isPublished, search, pageable);
        Page<SupplierCatalogDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateCatalog(Long id, SupplierCatalogDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", id));

        if (dto.getName() != null) catalog.setName(dto.getName());
        if (dto.getDescription() != null) catalog.setDescription(dto.getDescription());
        if (dto.getCategory() != null) catalog.setCategory(dto.getCategory());
        if (dto.getFamily() != null) catalog.setFamily(dto.getFamily());
        if (dto.getSku() != null && !dto.getSku().equals(catalog.getSku())) {
            if (catalogRepo.existsBySkuAndSupplierOrgAccountId(dto.getSku(), orgId)) throw new ValidationException("SKU already exists");
            catalog.setSku(dto.getSku());
        }
        if (dto.getCode() != null && !dto.getCode().equals(catalog.getCode())) {
            if (catalogRepo.existsByCodeAndSupplierOrgAccountId(dto.getCode(), orgId)) throw new ValidationException("Code already exists");
            catalog.setCode(dto.getCode());
        }
        if (dto.getAttributes() != null) catalog.setAttributes(dto.getAttributes());
        if (dto.getSpecifications() != null) catalog.setSpecifications(dto.getSpecifications());
        if (dto.getBasePrice() != null) catalog.setBasePrice(dto.getBasePrice());
        if (dto.getUnitOfMeasure() != null) catalog.setUnitOfMeasure(dto.getUnitOfMeasure());
        if (dto.getCurrency() != null) catalog.setCurrency(dto.getCurrency());
        if (dto.getAccessLevel() != null) catalog.setAccessLevel(dto.getAccessLevel());
        if (dto.getAllowedPartnerOrgIds() != null) catalog.setAllowedPartnerOrgIds(dto.getAllowedPartnerOrgIds());
        // status via transitionStatus ideally, but allow draft update
        if (dto.getStatus() != null) catalog.setStatus(dto.getStatus());

        SupplierCatalog saved = catalogRepo.save(catalog);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", id));
        CatalogStatus target;
        try { target = CatalogStatus.valueOf(newStatus.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus)); }

        if (!ALLOWED.getOrDefault(catalog.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transition not allowed: " + catalog.getStatus() + " -> " + target));
        }
        catalog.setStatus(target);
        if (target == CatalogStatus.PUBLISHED) {
            catalog.setIsPublished(true);
            catalog.setPublishedAt(Timestamp.valueOf(LocalDateTime.now()));
            if (params != null && params.get("accessLevel") != null) {
                try { catalog.setAccessLevel(CatalogAccessLevel.valueOf(params.get("accessLevel").toString().toUpperCase())); } catch (Exception ignored) {}
            }
        } else if (target == CatalogStatus.ARCHIVED || target == CatalogStatus.DRAFT) {
            catalog.setIsPublished(false);
        }
        SupplierCatalog saved = catalogRepo.save(catalog);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteCatalog(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", id));
        if (catalog.getStatus() == CatalogStatus.PUBLISHED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete PUBLISHED catalog. Archive first."));
        }
        catalogRepo.delete(catalog);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        long draft = catalogRepo.countByOrgAndStatus(orgId, CatalogStatus.DRAFT);
        long published = catalogRepo.countByOrgAndStatus(orgId, CatalogStatus.PUBLISHED);
        long archived = catalogRepo.countByOrgAndStatus(orgId, CatalogStatus.ARCHIVED);
        long total = catalogRepo.findByOrgWithFilters(orgId, null, null, null, null, null, null, Pageable.unpaged()).getTotalElements();
        return ResponseEntity.ok(Map.of("draft", draft, "published", published, "archived", archived, "total", total));
    }

    private SupplierCatalogDto mapToDto(SupplierCatalog c) {
        SupplierCatalogDto dto = modelMapper.map(c, SupplierCatalogDto.class);
        if (c.getSupplierOrg() != null) dto.setSupplierOrgId(c.getSupplierOrg().getAccountId());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> browseCatalogs(String search, String category, String family, Long supplierOrgId, Pageable pageable) {
        Long callerOrgId = OrganizationContextHolder.requireOrganizationId();
        Pageable effective = pageable;
        if (effective.getSort().isUnsorted()) {
            effective = PageRequest.of(effective.getPageNumber(), effective.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "updatedAt"));
        }
        Page<SupplierCatalog> page = catalogRepo.findBrowseCandidates(
                callerOrgId.toString(),
                blankToNull(category),
                blankToNull(family),
                supplierOrgId,
                blankToNull(search),
                effective);
        // Defensive second gate: re-verify allowlist in Java (exact token match).
        // Agrees with the SQL-side gate, so this is normally a no-op; the total is
        // adjusted in the pathological case where they diverge.
        java.util.List<CatalogBrowseDto> dtos = page.getContent().stream()
                .filter(c -> isVisibleToCaller(c, callerOrgId))
                .map(c -> mapToBrowseDto(c, callerOrgId))
                .toList();
        long removed = page.getContent().size() - dtos.size();
        Page<CatalogBrowseDto> dtoPage =
                new org.springframework.data.domain.PageImpl<>(dtos, effective, page.getTotalElements() - removed);
        return ResponseEntity.ok(dtoPage);
    }

    private CatalogBrowseDto mapToBrowseDto(SupplierCatalog c, Long callerOrgId) {
        Long orgId = c.getSupplierOrg() != null ? c.getSupplierOrg().getAccountId() : null;
        String orgName = c.getSupplierOrg() != null ? c.getSupplierOrg().getName() : null;
        return CatalogBrowseDto.builder()
                .catalogId(c.getCatalogId())
                .name(c.getName())
                .code(c.getCode())
                .sku(c.getSku())
                .description(c.getDescription())
                .category(c.getCategory())
                .family(c.getFamily())
                .basePrice(c.getBasePrice())
                .unitOfMeasure(c.getUnitOfMeasure())
                .currency(c.getCurrency())
                .status(c.getStatus())
                .accessLevel(c.getAccessLevel())
                .isPublished(c.getIsPublished())
                .publishedAt(c.getPublishedAt())
                .supplierOrgId(orgId)
                .supplierOrgName(orgName)
                .variantCount(c.getVariants() == null ? 0 : c.getVariants().size())
                .priceTierCount(c.getPriceTiers() == null ? 0 : c.getPriceTiers().size())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    /**
     * Defensive allowlist check for PARTNER_ONLY items: exact token match against
     * the comma-separated {@code allowedPartnerOrgIds} (trims entries, ignores
     * blanks). Mirrors the SQL-side gate in
     * {@link SupplierCatalogRepo#findBrowseCandidates}.
     */
    private boolean isVisibleToCaller(SupplierCatalog c, Long callerOrgId) {
        if (c.getStatus() != CatalogStatus.PUBLISHED || !Boolean.TRUE.equals(c.getIsPublished())) return false;
        if (c.getAccessLevel() == CatalogAccessLevel.PUBLIC) return true;
        if (c.getAccessLevel() == CatalogAccessLevel.PARTNER_ONLY) {
            String allowlist = c.getAllowedPartnerOrgIds();
            if (allowlist == null || allowlist.isBlank() || callerOrgId == null) return false;
            String caller = callerOrgId.toString();
            for (String token : allowlist.split(",")) {
                if (caller.equals(token.trim())) return true;
            }
        }
        return false;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
