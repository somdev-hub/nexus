package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.SupplierQuotationService;

import lombok.RequiredArgsConstructor;

/**
 * Retailer-facing quotation endpoints (exact contract paths).
 * Delegates to the same SupplierQuotationService; the supplier flow is untouched.
 */
@RestController
@RequestMapping("/core/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final SupplierQuotationService quotationService;

    @GetMapping("/by-buyer")
    @LogActivity("Get Quotations by Buyer")
    public ResponseEntity<?> getByBuyer(@RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotationService.getByBuyer(status, pageable);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Quotation as Buyer")
    public ResponseEntity<?> getQuotationAsBuyer(@PathVariable Long id) {
        return quotationService.getQuotationAsBuyer(id);
    }

    @PutMapping("/{id}/accept")
    @LogActivity("Accept Quotation as Buyer")
    public ResponseEntity<?> acceptAsBuyer(@PathVariable Long id) {
        return quotationService.acceptAsBuyer(id);
    }

    @PutMapping("/{id}/reject")
    @LogActivity("Reject Quotation as Buyer")
    public ResponseEntity<?> rejectAsBuyer(@PathVariable Long id) {
        return quotationService.rejectAsBuyer(id);
    }
}
