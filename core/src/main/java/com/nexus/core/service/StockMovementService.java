package com.nexus.core.service;

import com.nexus.core.entities.StockMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.StockMovementDto;

import java.sql.Timestamp;
import java.util.List;

/**
 * Service interface for StockMovement operations.
 * Provides audit trail for inventory movements.
 */
public interface StockMovementService {

	ResponseEntity<?> addStockMovement(StockMovementDto movementDto);

	ResponseEntity<?> getStockMovementById(Long id);

	ResponseEntity<?> getMovementsByStock(Long stockId, Pageable pageable);

	ResponseEntity<?> getMovementsByOrg(Pageable pageable);

	ResponseEntity<?> getMovementsByWarehouse(Long warehouseId, Pageable pageable);

	ResponseEntity<?> getMovementsByType(StockMovement.MovementType type, Pageable pageable);

	ResponseEntity<?> getMovementsByReference(String referenceType, Long referenceId);

	ResponseEntity<?> getMovementsByBatchNumber(String batchNumber);

	ResponseEntity<?> getExpiringStock(Timestamp beforeDate);

	ResponseEntity<?> getMovementsByDateRange(Timestamp start, Timestamp end, Pageable pageable);

	ResponseEntity<?> getMovementsByMaterial(Long materialId, Pageable pageable);

	ResponseEntity<?> getMovementSummary(Timestamp start, Timestamp end);
}