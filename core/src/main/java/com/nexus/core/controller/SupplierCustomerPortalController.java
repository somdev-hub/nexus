package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.SupplierCustomerPortalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/customer-portal")
@RequiredArgsConstructor
public class SupplierCustomerPortalController {

    private final SupplierCustomerPortalService portalService;

    @GetMapping("/orders")
    @LogActivity("Get Customer Orders")
    public ResponseEntity<?> getCustomerOrders(
            @RequestParam(required = false) Long buyerOrgId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return portalService.getCustomerOrders(buyerOrgId, status, pageable);
    }

    @GetMapping("/invoices")
    @LogActivity("Get Customer Invoices")
    public ResponseEntity<?> getCustomerInvoices(
            @RequestParam(required = false) Long buyerOrgId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return portalService.getCustomerInvoices(buyerOrgId, status, pageable);
    }

    @GetMapping("/shipments")
    @LogActivity("Get Customer Shipments")
    public ResponseEntity<?> getCustomerShipments(
            @RequestParam(required = false) Long buyerOrgId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return portalService.getCustomerShipments(buyerOrgId, status, pageable);
    }

    @GetMapping("/returns")
    @LogActivity("Get Customer Returns")
    public ResponseEntity<?> getCustomerReturns(@PageableDefault(size = 20) Pageable pageable) {
        return portalService.getCustomerReturns(pageable);
    }

    @GetMapping("/summary")
    @LogActivity("Get Customer Portal Summary")
    public ResponseEntity<?> getSummary(@RequestParam(required = false) Long buyerOrgId) {
        return portalService.getCustomerSummary(buyerOrgId);
    }
}
