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

	ResponseEntity<?> getMovementsByOrg(Long stockId, Long warehouseId, String type, String referenceType,
			Long referenceId, String batchNumber, Timestamp beforeDate, Timestamp start, Timestamp end, Long materialId,
			Pageable pageable);

	ResponseEntity<?> getMovementSummary(Timestamp start, Timestamp end);
}