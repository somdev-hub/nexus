package com.nexus.core.service.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.ConsignmentStockDto;

public interface ConsignmentStockService {

    ResponseEntity<?> createConsignment(ConsignmentStockDto dto);

    ResponseEntity<?> getConsignment(Long id);

    ResponseEntity<?> getAllConsignments(Long retailerOrgId, Long warehouseId, Long materialId, Pageable pageable);

    ResponseEntity<?> getRetailerConsignments(Long warehouseId, Long materialId, Pageable pageable);

    ResponseEntity<?> consumeConsignmentByRetailer(Long id, Double quantity);

    ResponseEntity<?> returnConsignmentByRetailer(Long id, Double quantity);

    ResponseEntity<?> receiveConsignmentByRetailer(Long id, Double quantity);

    ResponseEntity<?> updateConsignment(Long id, ConsignmentStockDto dto);

    ResponseEntity<?> adjustQuantity(Long id, Double quantity, String reason);

    ResponseEntity<?> deleteConsignment(Long id);

    ResponseEntity<?> fillConsignment(Long id, String pickupAddress, String deliveryAddress);

    ResponseEntity<?> getSummary();
}
