package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.FreightInvoiceDto;

import java.util.Map;

public interface FreightInvoiceService {

    ResponseEntity<?> createFreightInvoice(FreightInvoiceDto dto);

    ResponseEntity<?> getFreightInvoice(Long id);

    ResponseEntity<?> getFreightInvoiceByNumber(String invoiceNumber);

    ResponseEntity<?> getAllFreightInvoices(String status, Long shipmentId, Long logisticsOrgId,
            java.sql.Date issuedStart, java.sql.Date issuedEnd,
            java.sql.Date dueStart, java.sql.Date dueEnd,
            String pmsStatus, Pageable pageable);

    ResponseEntity<?> updateFreightInvoice(Long id, FreightInvoiceDto dto);

    ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params);

    ResponseEntity<?> deleteFreightInvoice(Long id);

    ResponseEntity<?> getFreightInvoiceSummary();

    ResponseEntity<?> handoffToPms(Long id);

    ResponseEntity<?> getPmsStatus(Long id);

    ResponseEntity<?> recordDiscrepancy(Long id, String reason, java.math.BigDecimal amount);
}
