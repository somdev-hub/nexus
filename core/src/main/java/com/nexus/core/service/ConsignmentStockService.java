package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.ConsignmentStockDto;

public interface ConsignmentStockService {

    ResponseEntity<?> createConsignment(ConsignmentStockDto dto);

    ResponseEntity<?> getConsignment(Long id);

    ResponseEntity<?> getAllConsignments(Long retailerOrgId, Long warehouseId, Long materialId, Pageable pageable);

    ResponseEntity<?> updateConsignment(Long id, ConsignmentStockDto dto);

    ResponseEntity<?> adjustQuantity(Long id, Double quantity, String reason);

    ResponseEntity<?> deleteConsignment(Long id);

    ResponseEntity<?> getSummary();
}
