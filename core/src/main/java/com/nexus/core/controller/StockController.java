package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.StockDto;
import com.nexus.core.service.StockService;

import lombok.RequiredArgsConstructor;

/**
 * Controller for Stock operations.
 * Supports FR-RET-010: Multi-Warehouse Inventory
 * Supports FR-RET-011: Reorder Point Automation
 * Supports FR-RET-014: Inventory Valuation
 */
@RestController
@RequestMapping("/core/stocks")
@RequiredArgsConstructor
public class StockController {

	private final StockService stockService;

	@PostMapping("/add")
	@LogActivity("Create Stock")
	public ResponseEntity<?> addStock(@Valid @RequestBody StockDto stockDto) {
		return stockService.addStock(stockDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Stock")
	public ResponseEntity<?> getStock(@PathVariable Long id) {
		return stockService.getStockById(id);
	}

	@GetMapping("/material/{materialId}/warehouse/{warehouseId}")
	@LogActivity("Get Stock by Material and Warehouse")
	public ResponseEntity<?> getStockByMaterialAndWarehouse(@PathVariable Long materialId,
			@PathVariable Long warehouseId) {
		return stockService.getStockByMaterialAndWarehouse(materialId, warehouseId);
	}

	@GetMapping("/all")
	@LogActivity("Get All Stocks")
	public ResponseEntity<?> getAllStocks(@PageableDefault(size = 20) Pageable pageable) {
		return stockService.getAllStockByOrgId(pageable);
	}

	@GetMapping("/warehouse/{warehouseId}")
	@LogActivity("Get Stocks by Warehouse")
	public ResponseEntity<?> getStocksByWarehouse(@PathVariable Long warehouseId,
			@PageableDefault(size = 20) Pageable pageable) {
		return stockService.getAllStockByWarehouse(warehouseId, pageable);
	}

	@GetMapping("/material/{materialId}")
	@LogActivity("Get Stocks by Material")
	public ResponseEntity<?> getStocksByMaterial(@PathVariable Long materialId,
			@PageableDefault(size = 20) Pageable pageable) {
		return stockService.getAllStockByMaterial(materialId, pageable);
	}

	@GetMapping("/reorder-point")
	@LogActivity("Get Stocks Below Reorder Point")
	public ResponseEntity<?> getStocksBelowReorderPoint() {
		return stockService.getStockBelowReorderPoint();
	}

	@GetMapping("/min-level")
	@LogActivity("Get Stocks At or Below Min Level")
	public ResponseEntity<?> getStocksAtOrBelowMinLevel() {
		return stockService.getStockAtOrBelowMinLevel();
	}

	@GetMapping("/valuation")
	@LogActivity("Get Inventory Valuation")
	public ResponseEntity<?> getInventoryValuation() {
		return stockService.getInventoryValuation();
	}

	@GetMapping("/valuation/warehouse/{warehouseId}")
	@LogActivity("Get Warehouse Inventory Valuation")
	public ResponseEntity<?> getWarehouseInventoryValuation(@PathVariable Long warehouseId) {
		return stockService.getWarehouseInventoryValuation(warehouseId);
	}

	@PostMapping("/{stockId}/adjust")
	@LogActivity("Adjust Stock")
	public ResponseEntity<?> adjustStock(@PathVariable Long stockId, @RequestParam Double quantity,
			@RequestParam String reason, @RequestParam String referenceType, @RequestParam Long referenceId) {
		return stockService.adjustStock(stockId, quantity, reason, referenceType, referenceId);
	}

	@PostMapping("/{stockId}/reserve")
	@LogActivity("Reserve Stock")
	public ResponseEntity<?> reserveStock(@PathVariable Long stockId, @RequestParam Double quantity,
			@RequestParam String referenceType, @RequestParam Long referenceId) {
		return stockService.reserveStock(stockId, quantity, referenceType, referenceId);
	}

	@PostMapping("/{stockId}/release-reservation")
	@LogActivity("Release Stock Reservation")
	public ResponseEntity<?> releaseReservation(@PathVariable Long stockId, @RequestParam Double quantity,
			@RequestParam String referenceType, @RequestParam Long referenceId) {
		return stockService.releaseReservation(stockId, quantity, referenceType, referenceId);
	}

	@PostMapping("/{fromStockId}/transfer")
	@LogActivity("Transfer Stock")
	public ResponseEntity<?> transferStock(@PathVariable Long fromStockId, @RequestParam Long toWarehouseId,
			@RequestParam Double quantity, @RequestParam String reason) {
		return stockService.transferStock(fromStockId, toWarehouseId, quantity, reason);
	}

	@PostMapping("/{stockId}/cycle-count")
	@LogActivity("Record Cycle Count")
	public ResponseEntity<?> recordCycleCount(@PathVariable Long stockId, @RequestParam Double countedQuantity,
			@RequestParam String countedBy) {
		return stockService.recordCycleCount(stockId, countedQuantity, countedBy);
	}

	@GetMapping("/reorder-suggestions")
	@LogActivity("Get Reorder Suggestions")
	public ResponseEntity<?> getReorderSuggestions() {
		return stockService.getReorderSuggestions();
	}

	@PutMapping("/{stockId}/settings")
	@LogActivity("Update Stock Settings")
	public ResponseEntity<?> updateStockSettings(@PathVariable Long stockId,
			@RequestParam(required = false) Double reorderPoint,
			@RequestParam(required = false) Double reorderQuantity,
			@RequestParam(required = false) Double minStockLevel,
			@RequestParam(required = false) Double maxStockLevel) {
		return stockService.updateStockSettings(stockId, reorderPoint, reorderQuantity, minStockLevel, maxStockLevel);
	}
}