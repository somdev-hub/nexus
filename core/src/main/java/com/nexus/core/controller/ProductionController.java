package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.ProductionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/production-orders")
@RequiredArgsConstructor
public class ProductionController {

	private final ProductionService productionService;

	@PostMapping
	@LogActivity("Create Production Order")
	public ResponseEntity<?> create(@RequestParam Long productId,
			@RequestParam Double quantity,
			@RequestParam(required = false) Long warehouseId,
			@RequestParam(required = false) String notes,
			@RequestParam(required = false) String deadline) {
		java.sql.Timestamp ts = null;
		if (deadline != null && !deadline.isBlank()) {
			try {
				ts = java.sql.Timestamp.valueOf(deadline.replace("T", " ") + (deadline.length() <= 10 ? " 00:00:00" : ""));
			} catch (Exception ignored) {
				try { ts = java.sql.Timestamp.valueOf(deadline); } catch (Exception ignored2) {}
			}
		}
		return productionService.createProductionOrder(productId, quantity, warehouseId, notes, ts);
	}

	@GetMapping
	@LogActivity("Get Production Orders")
	public ResponseEntity<?> list(@PageableDefault(size = 20) Pageable pageable) {
		return productionService.getProductionOrders(pageable);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Production Order")
	public ResponseEntity<?> get(@PathVariable Long id) {
		return productionService.getProductionOrder(id);
	}

	@PostMapping("/{id}/allocate")
	@LogActivity("Allocate Production Materials")
	public ResponseEntity<?> allocate(@PathVariable Long id) {
		return productionService.allocate(id);
	}

	@PostMapping("/{id}/start")
	@LogActivity("Start Production")
	public ResponseEntity<?> start(@PathVariable Long id) {
		return productionService.startProduction(id);
	}

	@PostMapping("/{id}/complete")
	@LogActivity("Complete Production")
	public ResponseEntity<?> complete(@PathVariable Long id) {
		return productionService.completeProduction(id);
	}

	@PostMapping("/{id}/cancel")
	@LogActivity("Cancel Production")
	public ResponseEntity<?> cancel(@PathVariable Long id) {
		return productionService.cancelProduction(id);
	}

	@PostMapping("/release-batch/{stockId}")
	@LogActivity("Release Batch")
	public ResponseEntity<?> releaseBatch(@PathVariable Long stockId) {
		return productionService.releaseBatch(stockId);
	}
}