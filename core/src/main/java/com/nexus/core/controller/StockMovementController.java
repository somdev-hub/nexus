package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.StockMovementDto;
import com.nexus.core.service.StockMovementService;

import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

/**
 * Controller for StockMovement operations.
 * Provides audit trail for inventory movements.
 */
@RestController
@RequestMapping("/core/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

	private final StockMovementService stockMovementService;

	@PostMapping("/add")
	@LogActivity("Create Stock Movement")
	public ResponseEntity<?> addStockMovement(@Valid @RequestBody StockMovementDto movementDto) {
		return stockMovementService.addStockMovement(movementDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Stock Movement")
	public ResponseEntity<?> getStockMovement(@PathVariable Long id) {
		return stockMovementService.getStockMovementById(id);
	}

	@GetMapping("/stock/{stockId}")
	@LogActivity("Get Stock Movements by Stock")
	public ResponseEntity<?> getMovementsByStock(@PathVariable Long stockId,
			@PageableDefault(size = 20) Pageable pageable) {
		return stockMovementService.getMovementsByStock(stockId, pageable);
	}

	@GetMapping("/all")
	@LogActivity("Get All Stock Movements")
	public ResponseEntity<?> getAllMovements(@PageableDefault(size = 20) Pageable pageable) {
		return stockMovementService.getMovementsByOrg(pageable);
	}

	@GetMapping("/warehouse/{warehouseId}")
	@LogActivity("Get Stock Movements by Warehouse")
	public ResponseEntity<?> getMovementsByWarehouse(@PathVariable Long warehouseId,
			@PageableDefault(size = 20) Pageable pageable) {
		return stockMovementService.getMovementsByWarehouse(warehouseId, pageable);
	}

	@GetMapping("/type/{type}")
	@LogActivity("Get Stock Movements by Type")
	public ResponseEntity<?> getMovementsByType(@PathVariable String type,
			@PageableDefault(size = 20) Pageable pageable) {
		try {
			com.nexus.core.entities.StockMovement.MovementType movementType = com.nexus.core.entities.StockMovement.MovementType
					.valueOf(type.toUpperCase());
			return stockMovementService.getMovementsByType(movementType, pageable);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("Invalid movement type: " + type);
		}
	}

	@GetMapping("/reference")
	@LogActivity("Get Stock Movements by Reference")
	public ResponseEntity<?> getMovementsByReference(@RequestParam String referenceType,
			@RequestParam Long referenceId) {
		return stockMovementService.getMovementsByReference(referenceType, referenceId);
	}

	@GetMapping("/batch/{batchNumber}")
	@LogActivity("Get Stock Movements by Batch Number")
	public ResponseEntity<?> getMovementsByBatchNumber(@PathVariable String batchNumber) {
		return stockMovementService.getMovementsByBatchNumber(batchNumber);
	}

	@GetMapping("/expiring")
	@LogActivity("Get Expiring Stock")
	public ResponseEntity<?> getExpiringStock(@RequestParam Timestamp beforeDate) {
		return stockMovementService.getExpiringStock(beforeDate);
	}

	@GetMapping("/date-range")
	@LogActivity("Get Stock Movements by Date Range")
	public ResponseEntity<?> getMovementsByDateRange(@RequestParam Timestamp start,
			@RequestParam Timestamp end, @PageableDefault(size = 20) Pageable pageable) {
		return stockMovementService.getMovementsByDateRange(start, end, pageable);
	}

	@GetMapping("/material/{materialId}")
	@LogActivity("Get Stock Movements by Material")
	public ResponseEntity<?> getMovementsByMaterial(@PathVariable Long materialId,
			@PageableDefault(size = 20) Pageable pageable) {
		return stockMovementService.getMovementsByMaterial(materialId, pageable);
	}

	@GetMapping("/summary")
	@LogActivity("Get Stock Movement Summary")
	public ResponseEntity<?> getMovementSummary(@RequestParam Timestamp start,
			@RequestParam Timestamp end) {
		return stockMovementService.getMovementSummary(start, end);
	}
}