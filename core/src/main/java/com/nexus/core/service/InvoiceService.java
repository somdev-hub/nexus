package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.InvoiceDto;

import java.util.Map;

public interface InvoiceService {

	ResponseEntity<?> createInvoice(InvoiceDto invoiceDto);

	ResponseEntity<?> getInvoiceById(Long id);

	ResponseEntity<?> getAllInvoices(String status, Long purchaseOrderId, Long supplierId, Pageable pageable);

	ResponseEntity<?> updateInvoice(Long id, InvoiceDto invoiceDto);

	ResponseEntity<?> transitionStatus(Long id, com.nexus.core.entities.InvoiceStatus newStatus,
			Map<String, Object> params);
}