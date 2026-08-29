package com.nexus.core.controller;

import jakarta.validation.Valid;
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
import com.nexus.core.payload.InvoiceDto;
import com.nexus.core.service.InvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/invoices")
@RequiredArgsConstructor
public class InvoiceController {

	private final InvoiceService invoiceService;

	@PostMapping("/create")
	@LogActivity("Create Invoice")
	public ResponseEntity<?> createInvoice(@Valid @RequestBody InvoiceDto invoiceDto) {
		return invoiceService.createInvoice(invoiceDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Invoice")
	public ResponseEntity<?> getInvoice(@PathVariable Long id) {
		return invoiceService.getInvoiceById(id);
	}

	@GetMapping("/all")
	@LogActivity("Get All Invoices")
	public ResponseEntity<?> getAllInvoices(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long purchaseOrderId,
			@RequestParam(required = false) Long supplierId,
			@PageableDefault(size = 20) Pageable pageable) {
		return invoiceService.getAllInvoices(status, purchaseOrderId, supplierId, pageable);
	}

	@PutMapping("/{id}/update")
	@LogActivity("Update Invoice")
	public ResponseEntity<?> updateInvoice(@PathVariable Long id,
			@RequestBody InvoiceDto invoiceDto) {
		return invoiceService.updateInvoice(id, invoiceDto);
	}

	@PutMapping("/{id}/transition")
	@LogActivity("Transition Invoice Status")
	public ResponseEntity<?> transitionStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params) {
		try {
			com.nexus.core.entities.InvoiceStatus targetStatus = com.nexus.core.entities.InvoiceStatus
					.valueOf(newStatus.toUpperCase());
			return invoiceService.transitionStatus(id, targetStatus, params);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + newStatus);
		}
	}
}