package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.StockDto;
import com.nexus.core.payload.StockMovementDto;

import java.util.List;

/**
 * Service interface for Stock operations.
 * Supports FR-RET-010: Multi-Warehouse Inventory
 * Supports FR-RET-011: Reorder Point Automation
 * Supports FR-RET-014: Inventory Valuation
 */
public interface StockService {

	ResponseEntity<?> addStock(StockDto stockDto);

	ResponseEntity<?> getStockById(Long id);

	ResponseEntity<?> getStockByMaterialAndWarehouse(Long materialId, Long warehouseId);

	ResponseEntity<?> getAllStockByOrgId(Pageable pageable);

	ResponseEntity<?> getAllStockByWarehouse(Long warehouseId, Pageable pageable);

	ResponseEntity<?> getAllStockByMaterial(Long materialId, Pageable pageable);

	ResponseEntity<?> getStockBelowReorderPoint();

	ResponseEntity<?> getStockAtOrBelowMinLevel();

	ResponseEntity<?> getInventoryValuation();

	ResponseEntity<?> getWarehouseInventoryValuation(Long warehouseId);

	ResponseEntity<?> adjustStock(Long stockId, Double quantity, String reason, String referenceType, Long referenceId);

	ResponseEntity<?> reserveStock(Long stockId, Double quantity, String referenceType, Long referenceId);

	ResponseEntity<?> releaseReservation(Long stockId, Double quantity, String referenceType, Long referenceId);

	ResponseEntity<?> transferStock(Long fromStockId, Long toWarehouseId, Double quantity, String reason);

	ResponseEntity<?> recordCycleCount(Long stockId, Double countedQuantity, String countedBy);

	ResponseEntity<?> getReorderSuggestions();

	ResponseEntity<?> updateStockSettings(Long stockId, Double reorderPoint, Double reorderQuantity,
			Double minStockLevel, Double maxStockLevel);
}