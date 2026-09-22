package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.FreightInvoice;
import com.nexus.core.entities.FreightInvoiceStatus;
import com.nexus.core.entities.Shipment;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.FreightInvoiceDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.FreightInvoiceRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.FreightInvoiceService;
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
public class FreightInvoiceServiceImpl implements FreightInvoiceService {

    private final FreightInvoiceRepo freightInvoiceRepo;
    private final ShipmentRepo shipmentRepo;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    // Valid transitions map
    private static final Map<FreightInvoiceStatus, Set<FreightInvoiceStatus>> ALLOWED = Map.of(
            FreightInvoiceStatus.DRAFT, Set.of(FreightInvoiceStatus.PENDING_APPROVAL, FreightInvoiceStatus.CANCELLED),
            FreightInvoiceStatus.PENDING_APPROVAL, Set.of(FreightInvoiceStatus.APPROVED, FreightInvoiceStatus.DISPUTED, FreightInvoiceStatus.CANCELLED),
            FreightInvoiceStatus.APPROVED, Set.of(FreightInvoiceStatus.SENT_TO_PMS, FreightInvoiceStatus.DISPUTED, FreightInvoiceStatus.CANCELLED),
            FreightInvoiceStatus.SENT_TO_PMS, Set.of(FreightInvoiceStatus.PAID, FreightInvoiceStatus.DISPUTED),
            FreightInvoiceStatus.DISPUTED, Set.of(FreightInvoiceStatus.APPROVED, FreightInvoiceStatus.CANCELLED),
            FreightInvoiceStatus.PAID, Set.of(),
            FreightInvoiceStatus.CANCELLED, Set.of()
    );

    @Override
    @Transactional
    public ResponseEntity<?> createFreightInvoice(FreightInvoiceDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Shipment shipment = shipmentRepo.findById(dto.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));

        // Verify shipment belongs to org
        if (shipment.getRetailerOrg() == null || !shipment.getRetailerOrg().getAccountId().equals(orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Shipment does not belong to organization"));
        }

        Account retailerOrg = accountRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account logisticsOrg = shipment.getLogisticsOrg();

        FreightInvoice fi = modelMapper.map(dto, FreightInvoice.class);
        fi.setFreightInvoiceId(null);
        fi.setInvoiceNumber(generateInvoiceNumber());
        fi.setShipment(shipment);
        fi.setRetailerOrg(retailerOrg);
        fi.setLogisticsOrg(logisticsOrg);
        fi.setStatus(FreightInvoiceStatus.DRAFT);
        fi.setIssuedDate(dto.getIssuedDate() != null ? dto.getIssuedDate() : Date.valueOf(LocalDate.now()));
        fi.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
        // Calculate total if not provided
        if (fi.getTotalAmount() == null) {
            BigDecimal total = BigDecimal.ZERO;
            if (fi.getFreightCost() != null) total = total.add(fi.getFreightCost());
            if (fi.getAccessorialCharges() != null) total = total.add(fi.getAccessorialCharges());
            if (fi.getFuelSurcharge() != null) total = total.add(fi.getFuelSurcharge());
            if (fi.getTaxAmount() != null) total = total.add(fi.getTaxAmount());
            fi.setTotalAmount(total);
        }
        FreightInvoice saved = freightInvoiceRepo.save(fi);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFreightInvoice(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByFreightInvoiceIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "freightInvoiceId", id));
        return ResponseEntity.ok(mapToDto(fi));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFreightInvoiceByNumber(String invoiceNumber) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByInvoiceNumberAndRetailerOrgAccountId(invoiceNumber, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "invoiceNumber", invoiceNumber));
        return ResponseEntity.ok(mapToDto(fi));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllFreightInvoices(String status, Long shipmentId, Long logisticsOrgId,
            Date issuedStart, Date issuedEnd, Date dueStart, Date dueEnd, String pmsStatus, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoiceStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = FreightInvoiceStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status)); }
        }
        Page<FreightInvoice> page = freightInvoiceRepo.findByOrgWithFilters(orgId, st, shipmentId, logisticsOrgId,
                issuedStart, issuedEnd, dueStart, dueEnd, pmsStatus, pageable);
        Page<FreightInvoiceDto> dtoPage = page.map(this::mapToDto);
        return ResponseEntity.ok(dtoPage);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateFreightInvoice(Long id, FreightInvoiceDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByFreightInvoiceIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "freightInvoiceId", id));
        if (fi.getStatus() != FreightInvoiceStatus.DRAFT && fi.getStatus() != FreightInvoiceStatus.DISPUTED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Only DRAFT or DISPUTED invoices can be updated"));
        }
        if (dto.getFreightCost() != null) fi.setFreightCost(dto.getFreightCost());
        if (dto.getAccessorialCharges() != null) fi.setAccessorialCharges(dto.getAccessorialCharges());
        if (dto.getFuelSurcharge() != null) fi.setFuelSurcharge(dto.getFuelSurcharge());
        if (dto.getTaxAmount() != null) fi.setTaxAmount(dto.getTaxAmount());
        if (dto.getDueDate() != null) fi.setDueDate(dto.getDueDate());
        if (dto.getNotes() != null) fi.setNotes(dto.getNotes());
        // recalc total
        BigDecimal total = BigDecimal.ZERO;
        if (fi.getFreightCost() != null) total = total.add(fi.getFreightCost());
        if (fi.getAccessorialCharges() != null) total = total.add(fi.getAccessorialCharges());
        if (fi.getFuelSurcharge() != null) total = total.add(fi.getFuelSurcharge());
        if (fi.getTaxAmount() != null) total = total.add(fi.getTaxAmount());
        fi.setTotalAmount(total);
        FreightInvoice saved = freightInvoiceRepo.save(fi);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByFreightInvoiceIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "freightInvoiceId", id));
        FreightInvoiceStatus target;
        try { target = FreightInvoiceStatus.valueOf(newStatus.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus)); }

        if (!isTransitionAllowed(fi.getStatus(), target)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Transition not allowed: " + fi.getStatus() + " -> " + target));
        }
        fi.setStatus(target);
        if (target == FreightInvoiceStatus.SENT_TO_PMS) {
            // Simulate PMS handoff: generate reference if missing
            if (fi.getPmsReferenceId() == null) fi.setPmsReferenceId("PMS-" + System.currentTimeMillis());
            fi.setPmsStatus("INITIATED");
        }
        if (target == FreightInvoiceStatus.PAID) {
            fi.setPaidDate(new java.sql.Timestamp(System.currentTimeMillis()));
            fi.setPmsStatus("PAID");
        }
        if (params != null && params.get("reason") != null) {
            fi.setDiscrepancyReason(params.get("reason").toString());
        }
        FreightInvoice saved = freightInvoiceRepo.save(fi);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteFreightInvoice(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByFreightInvoiceIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "freightInvoiceId", id));
        if (fi.getStatus() != FreightInvoiceStatus.DRAFT && fi.getStatus() != FreightInvoiceStatus.CANCELLED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Only DRAFT or CANCELLED invoices can be deleted"));
        }
        freightInvoiceRepo.delete(fi);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFreightInvoiceSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        long draft = freightInvoiceRepo.countByOrgAndStatus(orgId, FreightInvoiceStatus.DRAFT);
        long pending = freightInvoiceRepo.countByOrgAndStatus(orgId, FreightInvoiceStatus.PENDING_APPROVAL);
        long approved = freightInvoiceRepo.countByOrgAndStatus(orgId, FreightInvoiceStatus.APPROVED);
        long sent = freightInvoiceRepo.countByOrgAndStatus(orgId, FreightInvoiceStatus.SENT_TO_PMS);
        long paid = freightInvoiceRepo.countByOrgAndStatus(orgId, FreightInvoiceStatus.PAID);
        long disputed = freightInvoiceRepo.countByOrgAndStatus(orgId, FreightInvoiceStatus.DISPUTED);
        BigDecimal total = freightInvoiceRepo.sumTotalByOrg(orgId);
        var summary = Map.of(
                "draft", draft,
                "pendingApproval", pending,
                "approved", approved,
                "sentToPms", sent,
                "paid", paid,
                "disputed", disputed,
                "totalAmount", total != null ? total : BigDecimal.ZERO
        );
        return ResponseEntity.ok(summary);
    }

    @Override
    @Transactional
    public ResponseEntity<?> handoffToPms(Long id) {
        return transitionStatus(id, FreightInvoiceStatus.SENT_TO_PMS.name(), Map.of("reason", "PMS handoff initiated"));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPmsStatus(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByFreightInvoiceIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "freightInvoiceId", id));
        return ResponseEntity.ok(Map.of(
                "invoiceNumber", fi.getInvoiceNumber(),
                "pmsReferenceId", fi.getPmsReferenceId() != null ? fi.getPmsReferenceId() : "",
                "pmsStatus", fi.getPmsStatus() != null ? fi.getPmsStatus() : "NOT_SENT",
                "status", fi.getStatus().name()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> recordDiscrepancy(Long id, String reason, BigDecimal amount) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        FreightInvoice fi = freightInvoiceRepo.findByFreightInvoiceIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FreightInvoice", "freightInvoiceId", id));
        fi.setDiscrepancyReason(reason);
        fi.setDiscrepancyAmount(amount);
        fi.setStatus(FreightInvoiceStatus.DISPUTED);
        fi.setPmsStatus("DISPUTED");
        FreightInvoice saved = freightInvoiceRepo.save(fi);
        return ResponseEntity.ok(mapToDto(saved));
    }

    private boolean isTransitionAllowed(FreightInvoiceStatus from, FreightInvoiceStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    private String generateInvoiceNumber() {
        return "FI-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private FreightInvoiceDto mapToDto(FreightInvoice fi) {
        FreightInvoiceDto dto = modelMapper.map(fi, FreightInvoiceDto.class);
        if (fi.getShipment() != null) {
            dto.setShipmentId(fi.getShipment().getShipmentId());
            dto.setShipmentNumber(fi.getShipment().getShipmentNumber());
        }
        if (fi.getRetailerOrg() != null) dto.setRetailerOrgId(fi.getRetailerOrg().getAccountId());
        if (fi.getLogisticsOrg() != null) dto.setLogisticsOrgId(fi.getLogisticsOrg().getAccountId());
        return dto;
    }
}
