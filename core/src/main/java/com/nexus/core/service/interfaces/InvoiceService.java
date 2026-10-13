package com.nexus.core.service.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.InvoiceDto;

import java.util.Map;

public interface InvoiceService {

	ResponseEntity<?> createInvoice(InvoiceDto invoiceDto);

	ResponseEntity<?> getInvoiceById(Long id);

	ResponseEntity<?> getAllInvoices(String status, Long purchaseOrderId, Long supplierId, Pageable pageable);

	ResponseEntity<?> updateInvoice(Long id, InvoiceDto invoiceDto);

	ResponseEntity<?> transitionStatus(Long id, com.nexus.core.model.enums.InvoiceStatus newStatus,
			Map<String, Object> params);

	// Supplier-raised invoicing: the supplier builds the invoice (charges,
	// taxes) from a shared goods receipt and submits it; the retailer only
	// reviews, approves/rejects and pays.
	ResponseEntity<?> createSupplierInvoice(InvoiceDto invoiceDto);

	ResponseEntity<?> getSupplierInvoiceById(Long id);

	ResponseEntity<?> getSupplierInvoices(String status, Long purchaseOrderId, Pageable pageable);

	ResponseEntity<?> updateSupplierInvoice(Long id, InvoiceDto invoiceDto);

	ResponseEntity<?> transitionSupplierInvoice(Long id, com.nexus.core.model.enums.InvoiceStatus newStatus,
			Map<String, Object> params);
}