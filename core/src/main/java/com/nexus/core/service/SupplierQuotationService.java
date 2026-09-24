package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierQuotationDto;

import java.sql.Date;
import java.util.Map;

public interface SupplierQuotationService {

    ResponseEntity<?> createQuotation(SupplierQuotationDto dto);

    ResponseEntity<?> getQuotation(Long id);

    ResponseEntity<?> getQuotationByNumber(String quotationNumber);

    ResponseEntity<?> getAllQuotations(String status, Long buyerOrgId, String quotationNumber, Date validFrom, Date validTo, Pageable pageable);

    ResponseEntity<?> updateQuotation(Long id, SupplierQuotationDto dto);

    ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params);

    ResponseEntity<?> createNewVersion(Long id, SupplierQuotationDto dto);

    ResponseEntity<?> convertToOrder(Long id);

    ResponseEntity<?> deleteQuotation(Long id);

    ResponseEntity<?> getSummary();
}
