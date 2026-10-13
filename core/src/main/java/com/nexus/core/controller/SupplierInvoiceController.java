package com.nexus.core.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.model.enums.InvoiceStatus;
import com.nexus.core.payload.InvoiceDto;
import com.nexus.core.service.interfaces.InvoiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/invoices")
@RequiredArgsConstructor
public class SupplierInvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/create")
    @LogActivity("Create Supplier Invoice")
    public ResponseEntity<?> createInvoice(@Valid @RequestBody InvoiceDto invoiceDto) {
        return invoiceService.createSupplierInvoice(invoiceDto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Supplier Invoice")
    public ResponseEntity<?> getInvoice(@PathVariable Long id) {
        return invoiceService.getSupplierInvoiceById(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Supplier Invoices")
    public ResponseEntity<?> getAllInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long purchaseOrderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return invoiceService.getSupplierInvoices(status, purchaseOrderId, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Supplier Invoice")
    public ResponseEntity<?> updateInvoice(@PathVariable Long id,
            @RequestBody InvoiceDto invoiceDto) {
        return invoiceService.updateSupplierInvoice(id, invoiceDto);
    }

    @PutMapping("/{id}/transition")
    @LogActivity("Transition Supplier Invoice Status")
    public ResponseEntity<?> transitionStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            InvoiceStatus targetStatus = InvoiceStatus.valueOf(newStatus.toUpperCase());
            return invoiceService.transitionSupplierInvoice(id, targetStatus, params);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + newStatus);
        }
    }
}
