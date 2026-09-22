package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.FreightInvoiceDto;
import com.nexus.core.service.FreightInvoiceService;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Map;

@RestController
@RequestMapping("/core/freight-invoices")
@RequiredArgsConstructor
public class FreightInvoiceController {

    private final FreightInvoiceService freightInvoiceService;

    @PostMapping("/create")
    @LogActivity("Create Freight Invoice")
    public ResponseEntity<?> createFreightInvoice(@Valid @RequestBody FreightInvoiceDto dto) {
        return freightInvoiceService.createFreightInvoice(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Freight Invoice")
    public ResponseEntity<?> getFreightInvoice(@PathVariable Long id) {
        return freightInvoiceService.getFreightInvoice(id);
    }

    @GetMapping("/number/{invoiceNumber}")
    @LogActivity("Get Freight Invoice by Number")
    public ResponseEntity<?> getByNumber(@PathVariable String invoiceNumber) {
        return freightInvoiceService.getFreightInvoiceByNumber(invoiceNumber);
    }

    /**
     * Consolidated GET all with filters – avoids duplicated endpoints.
     * FR-FIN-001..007 consolidated.
     */
    @GetMapping("/all")
    @LogActivity("Get All Freight Invoices")
    public ResponseEntity<?> getAllFreightInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) Long logisticsOrgId,
            @RequestParam(required = false) Date issuedStart,
            @RequestParam(required = false) Date issuedEnd,
            @RequestParam(required = false) Date dueStart,
            @RequestParam(required = false) Date dueEnd,
            @RequestParam(required = false) String pmsStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        return freightInvoiceService.getAllFreightInvoices(status, shipmentId, logisticsOrgId,
                issuedStart, issuedEnd, dueStart, dueEnd, pmsStatus, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Freight Invoice")
    public ResponseEntity<?> updateFreightInvoice(@PathVariable Long id, @RequestBody FreightInvoiceDto dto) {
        return freightInvoiceService.updateFreightInvoice(id, dto);
    }

    @PutMapping("/{id}/status")
    @LogActivity("Transition Freight Invoice Status")
    public ResponseEntity<?> transitionStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return freightInvoiceService.transitionStatus(id, newStatus, params);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Freight Invoice")
    public ResponseEntity<?> deleteFreightInvoice(@PathVariable Long id) {
        return freightInvoiceService.deleteFreightInvoice(id);
    }

    @GetMapping("/summary")
    @LogActivity("Get Freight Invoice Summary")
    public ResponseEntity<?> getSummary() {
        return freightInvoiceService.getFreightInvoiceSummary();
    }

    @PostMapping("/{id}/handoff-pms")
    @LogActivity("Handoff Freight Invoice to PMS")
    public ResponseEntity<?> handoffToPms(@PathVariable Long id) {
        return freightInvoiceService.handoffToPms(id);
    }

    @GetMapping("/{id}/pms-status")
    @LogActivity("Get PMS Status")
    public ResponseEntity<?> getPmsStatus(@PathVariable Long id) {
        return freightInvoiceService.getPmsStatus(id);
    }

    @PostMapping("/{id}/discrepancy")
    @LogActivity("Record Freight Invoice Discrepancy")
    public ResponseEntity<?> recordDiscrepancy(@PathVariable Long id,
            @RequestParam String reason,
            @RequestParam BigDecimal amount) {
        return freightInvoiceService.recordDiscrepancy(id, reason, amount);
    }
}
