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

	@GetMapping("/all")
	@LogActivity("Get All Stock Movements")
	public ResponseEntity<?> getAllStockMovements(
			@RequestParam(required = false) Long stockId,
			@RequestParam(required = false) Long warehouseId,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String referenceType,
			@RequestParam(required = false) Long referenceId,
			@RequestParam(required = false) String batchNumber,
			@RequestParam(required = false) Timestamp beforeDate,
			@RequestParam(required = false) Timestamp start,
			@RequestParam(required = false) Timestamp end,
			@RequestParam(required = false) Long materialId,
			@PageableDefault(size = 20) Pageable pageable) {
		return stockMovementService.getMovementsByOrg(stockId, warehouseId, type, referenceType, referenceId,
				batchNumber, beforeDate, start, end, materialId, pageable);
	}

	@GetMapping("/summary")
	@LogActivity("Get Stock Movement Summary")
	public ResponseEntity<?> getMovementSummary(@RequestParam Timestamp start,
			@RequestParam Timestamp end) {
		return stockMovementService.getMovementSummary(start, end);
	}
}