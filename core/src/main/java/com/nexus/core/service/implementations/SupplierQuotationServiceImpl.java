package com.nexus.core.service.implementations;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.QuotationLineItem;
import com.nexus.core.model.entities.SupplierCatalog;
import com.nexus.core.model.entities.SupplierDigitalAsset;
import com.nexus.core.model.entities.SupplierQuotation;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.QuotationLineItemDto;
import com.nexus.core.payload.SupplierQuotationDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierDigitalAssetRepo;
import com.nexus.core.repository.ProductVariantRepo;
import com.nexus.core.repository.SupplierPriceTierRepo;
import com.nexus.core.repository.SupplierQuotationRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierQuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierQuotationServiceImpl implements SupplierQuotationService {

    private final SupplierQuotationRepo quotationRepo;
    private final SupplierCatalogRepo catalogRepo;
    private final SupplierDigitalAssetRepo digitalAssetRepo;
    private final SupplierPriceTierRepo priceTierRepo;
    private final ProductVariantRepo productVariantRepo;
    private final AccountRepository accountRepo;
    private final ModelMapper modelMapper;

    private static final Map<SupplierQuotation.QuotationStatus, Set<SupplierQuotation.QuotationStatus>> ALLOWED = Map.of(
            SupplierQuotation.QuotationStatus.DRAFT, Set.of(SupplierQuotation.QuotationStatus.SENT, SupplierQuotation.QuotationStatus.REJECTED),
            SupplierQuotation.QuotationStatus.SENT, Set.of(SupplierQuotation.QuotationStatus.ACCEPTED, SupplierQuotation.QuotationStatus.REJECTED, SupplierQuotation.QuotationStatus.EXPIRED),
            SupplierQuotation.QuotationStatus.ACCEPTED, Set.of(SupplierQuotation.QuotationStatus.CONVERTED),
            SupplierQuotation.QuotationStatus.REJECTED, Set.of(SupplierQuotation.QuotationStatus.DRAFT),
            SupplierQuotation.QuotationStatus.EXPIRED, Set.of(SupplierQuotation.QuotationStatus.DRAFT),
            SupplierQuotation.QuotationStatus.CONVERTED, Set.of()
    );

    @Override
    @Transactional
    public ResponseEntity<?> createQuotation(SupplierQuotationDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account buyerOrg = null;
        if (dto.getBuyerOrgId() != null) {
            buyerOrg = accountRepo.findById(dto.getBuyerOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getBuyerOrgId()));
        }

        SupplierQuotation q = new SupplierQuotation();
        q.setQuotationNumber("QT-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000));
        q.setSupplierOrg(supplierOrg);
        q.setBuyerOrg(buyerOrg);
        q.setStatus(SupplierQuotation.QuotationStatus.DRAFT);
        q.setValidFrom(dto.getValidFrom() != null ? dto.getValidFrom() : Date.valueOf(LocalDate.now()));
        q.setValidTo(dto.getValidTo() != null ? dto.getValidTo() : Date.valueOf(LocalDate.now().plusDays(30)));
        q.setTerms(dto.getTerms());
        q.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");

        if (dto.getLineItems() != null) {
            for (QuotationLineItemDto liDto : dto.getLineItems()) {
                QuotationLineItem li = new QuotationLineItem();
                if (liDto.getCatalogId() != null) {
                    SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(liDto.getCatalogId(), orgId)
                            .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", liDto.getCatalogId()));
                    li.setCatalog(catalog);
                }
                resolveLineAsset(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                resolveLineTier(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                resolveLineVariant(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                li.setDescription(liDto.getDescription());
                li.setQuantity(liDto.getQuantity());
                li.setUnitPrice(liDto.getUnitPrice());
                BigDecimal total = liDto.getUnitPrice() != null && liDto.getQuantity() != null ? liDto.getUnitPrice().multiply(BigDecimal.valueOf(liDto.getQuantity())) : BigDecimal.ZERO;
                li.setTotalPrice(liDto.getTotalPrice() != null ? liDto.getTotalPrice() : total);
                li.setNotes(liDto.getNotes());
                q.addLineItem(li);
            }
        }
        SupplierQuotation saved = quotationRepo.save(q);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getQuotation(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        return ResponseEntity.ok(mapToDto(q));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getQuotationByNumber(String quotationNumber) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationNumberAndSupplierOrgAccountId(quotationNumber, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationNumber", quotationNumber));
        return ResponseEntity.ok(mapToDto(q));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllQuotations(String status, Long buyerOrgId, String quotationNumber, Date validFrom, Date validTo, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation.QuotationStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = SupplierQuotation.QuotationStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status)); }
        }
        Page<SupplierQuotation> page = quotationRepo.findByOrgWithFilters(orgId, st, buyerOrgId, quotationNumber, validFrom, validTo, pageable);
        Page<SupplierQuotationDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateQuotation(Long id, SupplierQuotationDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        if (q.getStatus() != SupplierQuotation.QuotationStatus.DRAFT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only DRAFT quotations can be updated"));
        }
        if (dto.getValidFrom() != null) q.setValidFrom(dto.getValidFrom());
        if (dto.getValidTo() != null) q.setValidTo(dto.getValidTo());
        if (dto.getTerms() != null) q.setTerms(dto.getTerms());
        if (dto.getCurrency() != null) q.setCurrency(dto.getCurrency());
        if (dto.getBuyerOrgId() != null) {
            Account buyer = accountRepo.findById(dto.getBuyerOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", dto.getBuyerOrgId()));
            q.setBuyerOrg(buyer);
        }
        // replace line items
        if (dto.getLineItems() != null) {
            q.getLineItems().clear();
            for (QuotationLineItemDto liDto : dto.getLineItems()) {
                QuotationLineItem li = new QuotationLineItem();
                if (liDto.getCatalogId() != null) {
                    SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(liDto.getCatalogId(), orgId)
                            .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", liDto.getCatalogId()));
                    li.setCatalog(catalog);
                }
                resolveLineAsset(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                resolveLineTier(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                resolveLineVariant(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                li.setDescription(liDto.getDescription());
                li.setQuantity(liDto.getQuantity());
                li.setUnitPrice(liDto.getUnitPrice());
                BigDecimal total = liDto.getUnitPrice() != null && liDto.getQuantity() != null ? liDto.getUnitPrice().multiply(BigDecimal.valueOf(liDto.getQuantity())) : BigDecimal.ZERO;
                li.setTotalPrice(total);
                q.addLineItem(li);
            }
        }
        SupplierQuotation saved = quotationRepo.save(q);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        SupplierQuotation.QuotationStatus target;
        try { target = SupplierQuotation.QuotationStatus.valueOf(newStatus.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus)); }
        // Acceptance is a retailer-only action via acceptAsBuyer: the supplier
        // must not be able to accept its own quotation through a generic
        // status transition.
        if (target == SupplierQuotation.QuotationStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only the retailer (buyer) organization can accept quotations"));
        }
        if (!ALLOWED.getOrDefault(q.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transition not allowed: " + q.getStatus() + " -> " + target));
        }
        q.setStatus(target);
        SupplierQuotation saved = quotationRepo.save(q);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createNewVersion(Long id, SupplierQuotationDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation parent = quotationRepo.findByQuotationIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        SupplierQuotation newVersion = new SupplierQuotation();
        newVersion.setQuotationNumber(parent.getQuotationNumber() + "-V" + (parent.getVersionNumber() + 1));
        newVersion.setSupplierOrg(parent.getSupplierOrg());
        newVersion.setBuyerOrg(parent.getBuyerOrg());
        newVersion.setStatus(SupplierQuotation.QuotationStatus.DRAFT);
        newVersion.setValidFrom(dto.getValidFrom() != null ? dto.getValidFrom() : parent.getValidFrom());
        newVersion.setValidTo(dto.getValidTo() != null ? dto.getValidTo() : parent.getValidTo());
        newVersion.setTerms(dto.getTerms() != null ? dto.getTerms() : parent.getTerms());
        newVersion.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : parent.getCurrency());
        newVersion.setParentQuotationId(parent.getQuotationId());
        newVersion.setVersionNumber(parent.getVersionNumber() + 1);
        // copy line items if dto doesn't provide
        var lineItems = dto.getLineItems() != null ? dto.getLineItems() : parent.getLineItems().stream().map(li -> {
            QuotationLineItemDto d = new QuotationLineItemDto();
            d.setCatalogId(li.getCatalog() != null ? li.getCatalog().getCatalogId() : null);
            d.setPriceTierId(li.getPriceTierId());
            d.setVariantId(li.getVariantId());
            d.setDescription(li.getDescription());
            d.setQuantity(li.getQuantity());
            d.setUnitPrice(li.getUnitPrice());
            return d;
        }).toList();
        for (QuotationLineItemDto liDto : lineItems) {
            QuotationLineItem li = new QuotationLineItem();
                if (liDto.getCatalogId() != null) {
                    SupplierCatalog catalog = catalogRepo.findByCatalogIdAndSupplierOrgAccountId(liDto.getCatalogId(), orgId)
                            .orElseThrow(() -> new ResourceNotFoundException("SupplierCatalog", "catalogId", liDto.getCatalogId()));
                    li.setCatalog(catalog);
                }
                resolveLineAsset(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                resolveLineTier(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
                resolveLineVariant(li, liDto.getCatalogId() != null ? li.getCatalog() : null, liDto, orgId);
            li.setDescription(liDto.getDescription());
            li.setQuantity(liDto.getQuantity());
            li.setUnitPrice(liDto.getUnitPrice());
            BigDecimal total = liDto.getUnitPrice() != null && liDto.getQuantity() != null ? liDto.getUnitPrice().multiply(BigDecimal.valueOf(liDto.getQuantity())) : BigDecimal.ZERO;
            li.setTotalPrice(total);
            newVersion.addLineItem(li);
        }
        SupplierQuotation saved = quotationRepo.save(newVersion);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> convertToOrder(Long id) {
        // Conversion is a retailer-only action via purchase-order creation:
        // the supplier must not convert its own quotation through this
        // legacy endpoint.
        return ResponseEntity.badRequest().body(Map.of("error",
                "Only the retailer (buyer) organization can convert quotations to orders — create the purchase order from the retailer"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteQuotation(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        if (q.getStatus() == SupplierQuotation.QuotationStatus.CONVERTED || q.getStatus() == SupplierQuotation.QuotationStatus.ACCEPTED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete " + q.getStatus() + " quotation"));
        }
        quotationRepo.delete(q);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSummary() {        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var all = quotationRepo.findByOrgWithFilters(orgId, null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long draft = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.DRAFT).count();
        long sent = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.SENT).count();
        long accepted = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.ACCEPTED).count();
        long converted = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.CONVERTED).count();
        return ResponseEntity.ok(Map.of("total", all.size(), "draft", draft, "sent", sent, "accepted", accepted, "converted", converted));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getByBuyer(String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        // Retailer contract: only SENT quotations are visible by default.
        // DRAFT quotations are private to the supplier until sent, so they
        // must never leak to the retailer list. An explicit status filter
        // (e.g. ACCEPTED history) is still honoured when provided.
        SupplierQuotation.QuotationStatus st = SupplierQuotation.QuotationStatus.SENT;
        if (status != null && !status.isBlank()) {
            try { st = SupplierQuotation.QuotationStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status)); }
        }
        var page = quotationRepo.findByBuyerOrgAccountIdAndStatus(orgId, st, pageable);
        return ResponseEntity.ok(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getQuotationAsBuyer(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationIdAndBuyerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        // DRAFT quotations are private to the supplier until sent and must
        // never be exposed through the buyer-scoped view.
        if (q.getStatus() == SupplierQuotation.QuotationStatus.DRAFT) {
            throw new ResourceNotFoundException("SupplierQuotation", "quotationId", id);
        }
        return ResponseEntity.ok(mapToDto(q));
    }

    @Override
    @Transactional
    public ResponseEntity<?> acceptAsBuyer(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        // Only retailer organizations may accept quotations, and only the
        // designated buyer org for that quotation.
        String orgType = OrganizationContextHolder.getCurrentOrganizationType();
        if (orgType != null && !orgType.equalsIgnoreCase("RETAILER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only retailer organizations can accept quotations"));
        }
        SupplierQuotation q = quotationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        if (q.getBuyerOrg() == null || !q.getBuyerOrg().getAccountId().equals(orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the buyer organization can accept quotation " + id));
        }
        if (q.getStatus() != SupplierQuotation.QuotationStatus.SENT) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only SENT quotations can be accepted. Current: " + q.getStatus()));
        }
        q.setStatus(SupplierQuotation.QuotationStatus.ACCEPTED);
        return ResponseEntity.ok(mapToDto(quotationRepo.save(q)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> rejectAsBuyer(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        // Only retailer organizations may reject quotations, and only the
        // designated buyer org for that quotation.
        String orgType = OrganizationContextHolder.getCurrentOrganizationType();
        if (orgType != null && !orgType.equalsIgnoreCase("RETAILER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only retailer organizations can reject quotations"));
        }
        SupplierQuotation q = quotationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        if (q.getBuyerOrg() == null || !q.getBuyerOrg().getAccountId().equals(orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the buyer organization can reject quotation " + id));
        }
        if (q.getStatus() != SupplierQuotation.QuotationStatus.SENT) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only SENT quotations can be rejected. Current: " + q.getStatus()));
        }
        q.setStatus(SupplierQuotation.QuotationStatus.REJECTED);
        return ResponseEntity.ok(mapToDto(quotationRepo.save(q)));
    }

    /**
     * Resolve the line's selected product variant: it must exist under the
     * caller's org, and when the line also has a catalog the variant must
     * belong to that same catalog.
     */
    private void resolveLineVariant(QuotationLineItem li, SupplierCatalog catalog,
            QuotationLineItemDto liDto, Long orgId) {
        if (liDto.getVariantId() == null) {
            li.setVariantId(null);
            return;
        }
        var variant = productVariantRepo
                .findByVariantIdAndCatalogSupplierOrgAccountId(liDto.getVariantId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "variantId",
                        liDto.getVariantId()));
        if (catalog != null && variant.getCatalog() != null
                && !variant.getCatalog().getCatalogId().equals(catalog.getCatalogId())) {
            throw new ValidationException(
                    "Product variant does not belong to the line's catalog");
        }
        li.setVariantId(variant.getVariantId());
    }

    /**
     * Resolve the line's selected price tier: it must exist under the
     * caller's org, and when the line also has a catalog the tier must
     * belong to that same catalog.
     */
    private void resolveLineTier(QuotationLineItem li, SupplierCatalog catalog,
            QuotationLineItemDto liDto, Long orgId) {
        if (liDto.getPriceTierId() == null) {
            li.setPriceTierId(null);
            return;
        }
        var tier = priceTierRepo
                .findByTierIdAndCatalogSupplierOrgAccountId(liDto.getPriceTierId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierPriceTier", "tierId",
                        liDto.getPriceTierId()));
        if (catalog != null && tier.getCatalog() != null
                && !tier.getCatalog().getCatalogId().equals(catalog.getCatalogId())) {
            throw new ValidationException(
                    "Price tier does not belong to the line's catalog");
        }
        li.setPriceTierId(tier.getTierId());
    }

    /**
     * Resolve the line's supporting digital assets: each must exist under the
     * caller's org, and when the line also has a catalog each asset must
     * belong to that same catalog. The first id is also kept on the legacy
     * single-asset link for backward compatibility.
     */
    private void resolveLineAsset(QuotationLineItem li, SupplierCatalog catalog,
            QuotationLineItemDto liDto, Long orgId) {
        java.util.List<Long> ids = liDto.getDigitalAssetIds() != null
                ? liDto.getDigitalAssetIds().stream()
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(java.util.stream.Collectors.toList())
                : new java.util.ArrayList<>();
        if (ids.isEmpty() && liDto.getDigitalAssetId() != null) {
            ids = java.util.List.of(liDto.getDigitalAssetId());
        }
        java.util.List<Long> storedIds = new java.util.ArrayList<>();
        SupplierDigitalAsset first = null;
        for (Long assetId : ids) {
            SupplierDigitalAsset asset = digitalAssetRepo
                    .findByAssetIdAndCatalogSupplierOrgAccountId(assetId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("SupplierDigitalAsset", "assetId",
                            assetId));
            if (catalog != null && asset.getCatalog() != null
                    && !asset.getCatalog().getCatalogId().equals(catalog.getCatalogId())) {
                throw new ValidationException(
                        "Digital asset does not belong to the line's catalog");
            }
            if (first == null) {
                first = asset;
            }
            storedIds.add(asset.getAssetId());
        }
        li.setDigitalAsset(first);
        li.setDigitalAssetIds(storedIds);
    }

    private SupplierQuotationDto mapToDto(SupplierQuotation q) {        SupplierQuotationDto dto = modelMapper.map(q, SupplierQuotationDto.class);
        if (q.getSupplierOrg() != null) {
            dto.setSupplierOrgId(q.getSupplierOrg().getAccountId());
            dto.setSupplierOrgName(q.getSupplierOrg().getName());
        }
        if (q.getBuyerOrg() != null) {
            dto.setBuyerOrgId(q.getBuyerOrg().getAccountId());
            dto.setBuyerOrgName(q.getBuyerOrg().getName());
        }
        dto.setLineItems(q.getLineItems().stream().map(li -> {
            QuotationLineItemDto d = modelMapper.map(li, QuotationLineItemDto.class);
            if (li.getCatalog() != null) {
                d.setCatalogId(li.getCatalog().getCatalogId());
                d.setCatalogName(li.getCatalog().getName());
            }
            if (li.getDigitalAsset() != null) {
                d.setDigitalAssetId(li.getDigitalAsset().getAssetId());
                d.setDigitalAssetName(li.getDigitalAsset().getFileName());
            }
            if (li.getPriceTierId() != null) {
                d.setPriceTierId(li.getPriceTierId());
                d.setPriceTierName(priceTierRepo.findById(li.getPriceTierId())
                        .map(t -> t.getTierName() != null ? t.getTierName() : ("Tier #" + li.getPriceTierId()))
                        .orElse("Tier #" + li.getPriceTierId()));
            }
            if (li.getVariantId() != null) {
                d.setVariantId(li.getVariantId());
                d.setVariantName(productVariantRepo.findById(li.getVariantId())
                        .map(v -> (v.getVariantType() != null ? v.getVariantType().name() + ": " : "")
                                + (v.getVariantValue() != null ? v.getVariantValue() : ("Variant #" + li.getVariantId())))
                        .orElse("Variant #" + li.getVariantId()));
            }
            if (li.getDigitalAssetIds() != null && !li.getDigitalAssetIds().isEmpty()) {
                d.setDigitalAssetIds(new java.util.ArrayList<>(li.getDigitalAssetIds()));
                java.util.List<String> names = new java.util.ArrayList<>();
                java.util.List<String> urls = new java.util.ArrayList<>();
                java.util.List<String> types = new java.util.ArrayList<>();
                for (Long assetId : li.getDigitalAssetIds()) {
                    var assetOpt = digitalAssetRepo.findById(assetId);
                    names.add(assetOpt
                            .map(a -> a.getFileName() != null ? a.getFileName() : ("Asset #" + assetId))
                            .orElse("Asset #" + assetId));
                    urls.add(assetOpt.map(SupplierDigitalAsset::getDmsDocumentUrl).orElse(null));
                    types.add(assetOpt.map(a -> a.getAssetType() != null ? a.getAssetType().name() : null).orElse(null));
                }
                d.setDigitalAssetNames(names);
                d.setDigitalAssetUrls(urls);
                d.setDigitalAssetTypes(types);
            }
            return d;
        }).toList());
        return dto;
    }
}
