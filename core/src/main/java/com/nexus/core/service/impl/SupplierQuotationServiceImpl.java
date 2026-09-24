package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderLineItem;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.entities.QuotationLineItem;
import com.nexus.core.entities.SupplierCatalog;
import com.nexus.core.entities.SupplierQuotation;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.payload.QuotationLineItemDto;
import com.nexus.core.payload.SupplierQuotationDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.SupplierCatalogRepo;
import com.nexus.core.repository.SupplierQuotationRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierQuotationService;
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
    private final AccountRepository accountRepo;
    private final PurchaseOrderRepo poRepo;
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
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        SupplierQuotation q = quotationRepo.findByQuotationIdAndSupplierOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierQuotation", "quotationId", id));
        if (q.getStatus() != SupplierQuotation.QuotationStatus.ACCEPTED) {
            throw new ValidationException("Only ACCEPTED quotations can be converted to order");
        }
        if (q.getConvertedToPoId() != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Already converted to PO: " + q.getConvertedToPoId()));
        }
        // create PurchaseOrder
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber("PO-" + System.currentTimeMillis());
        po.setBuyerOrg(q.getBuyerOrg());
        po.setSupplierOrg(q.getSupplierOrg());
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setCurrency(q.getCurrency());
        po.setNotes("Converted from quotation " + q.getQuotationNumber());
        for (QuotationLineItem qli : q.getLineItems()) {
            PurchaseOrderLineItem pli = new PurchaseOrderLineItem();
            if (qli.getCatalog() != null) {
                // map catalog to product? For MVP, leave product null and set description
                pli.setDescription(qli.getDescription() != null ? qli.getDescription() : qli.getCatalog().getName());
            } else {
                pli.setDescription(qli.getDescription());
            }
            pli.setQuantityOrdered(qli.getQuantity());
            pli.setUnitPrice(qli.getUnitPrice() != null ? qli.getUnitPrice().doubleValue() : 0.0);
            pli.setTotalPrice(qli.getTotalPrice() != null ? qli.getTotalPrice().doubleValue() : 0.0);
            po.addLineItem(pli);
        }
        PurchaseOrder savedPo = poRepo.save(po);
        q.setStatus(SupplierQuotation.QuotationStatus.CONVERTED);
        q.setConvertedToPoId(savedPo.getPurchaseOrderId());
        quotationRepo.save(q);
        return ResponseEntity.ok(Map.of("quotationId", q.getQuotationId(), "purchaseOrderId", savedPo.getPurchaseOrderId(), "poNumber", savedPo.getPoNumber()));
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
    public ResponseEntity<?> getSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var all = quotationRepo.findByOrgWithFilters(orgId, null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long draft = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.DRAFT).count();
        long sent = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.SENT).count();
        long accepted = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.ACCEPTED).count();
        long converted = all.stream().filter(q -> q.getStatus() == SupplierQuotation.QuotationStatus.CONVERTED).count();
        return ResponseEntity.ok(Map.of("total", all.size(), "draft", draft, "sent", sent, "accepted", accepted, "converted", converted));
    }

    private SupplierQuotationDto mapToDto(SupplierQuotation q) {
        SupplierQuotationDto dto = modelMapper.map(q, SupplierQuotationDto.class);
        if (q.getSupplierOrg() != null) dto.setSupplierOrgId(q.getSupplierOrg().getAccountId());
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
            return d;
        }).toList());
        return dto;
    }
}
