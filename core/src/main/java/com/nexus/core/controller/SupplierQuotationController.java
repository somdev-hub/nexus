package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierQuotationDto;
import com.nexus.core.service.SupplierQuotationService;

import lombok.RequiredArgsConstructor;

import java.sql.Date;
import java.util.Map;

@RestController
@RequestMapping("/core/supplier/quotations")
@RequiredArgsConstructor
public class SupplierQuotationController {

    private final SupplierQuotationService quotationService;

    @PostMapping("/create")
    @LogActivity("Create Supplier Quotation")
    public ResponseEntity<?> createQuotation(@Valid @RequestBody SupplierQuotationDto dto) {
        return quotationService.createQuotation(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Supplier Quotation")
    public ResponseEntity<?> getQuotation(@PathVariable Long id) {
        return quotationService.getQuotation(id);
    }

    @GetMapping("/number/{quotationNumber}")
    @LogActivity("Get Quotation by Number")
    public ResponseEntity<?> getByNumber(@PathVariable String quotationNumber) {
        return quotationService.getQuotationByNumber(quotationNumber);
    }

    @GetMapping("/all")
    @LogActivity("Get All Supplier Quotations")
    public ResponseEntity<?> getAllQuotations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long buyerOrgId,
            @RequestParam(required = false) String quotationNumber,
            @RequestParam(required = false) Date validFrom,
            @RequestParam(required = false) Date validTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotationService.getAllQuotations(status, buyerOrgId, quotationNumber, validFrom, validTo, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Supplier Quotation")
    public ResponseEntity<?> updateQuotation(@PathVariable Long id, @RequestBody SupplierQuotationDto dto) {
        return quotationService.updateQuotation(id, dto);
    }

    @PutMapping("/{id}/status")
    @LogActivity("Transition Quotation Status")
    public ResponseEntity<?> transitionStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return quotationService.transitionStatus(id, newStatus, params);
    }

    @PostMapping("/{id}/new-version")
    @LogActivity("Create New Quotation Version")
    public ResponseEntity<?> createNewVersion(@PathVariable Long id, @RequestBody SupplierQuotationDto dto) {
        return quotationService.createNewVersion(id, dto);
    }

    @PostMapping("/{id}/convert-to-order")
    @LogActivity("Convert Quotation to Order")
    public ResponseEntity<?> convertToOrder(@PathVariable Long id) {
        return quotationService.convertToOrder(id);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Supplier Quotation")
    public ResponseEntity<?> deleteQuotation(@PathVariable Long id) {
        return quotationService.deleteQuotation(id);
    }

    @GetMapping("/summary")
    @LogActivity("Get Quotation Summary")
    public ResponseEntity<?> getSummary() {
        return quotationService.getSummary();
    }
}
