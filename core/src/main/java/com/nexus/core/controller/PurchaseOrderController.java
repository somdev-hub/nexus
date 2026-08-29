package com.nexus.core.controller;

import jakarta.validation.Valid;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import com.nexus.core.payload.PurchaseOrderDto;
import com.nexus.core.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

	private final PurchaseOrderService purchaseOrderService;

	@PostMapping("/create")
	@LogActivity("Create Purchase Order")
	public ResponseEntity<?> createPurchaseOrder(@Valid @RequestBody PurchaseOrderDto poDto) {
		return purchaseOrderService.createPurchaseOrder(poDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Purchase Order")
	public ResponseEntity<?> getPurchaseOrder(@PathVariable Long id) {
		return purchaseOrderService.getPurchaseOrderById(id);
	}

	@GetMapping("/all")
	@LogActivity("Get All Purchase Orders")
	public ResponseEntity<?> getAllPurchaseOrders(
			@RequestParam(required = false) String status,
			@PageableDefault(size = 20) Pageable pageable) {
		return purchaseOrderService.getAllPurchaseOrders(status, pageable);
	}

	@PutMapping("/{id}/update")
	@LogActivity("Update Purchase Order")
	public ResponseEntity<?> updatePurchaseOrder(@PathVariable Long id,
			@RequestBody PurchaseOrderDto poDto) {
		return purchaseOrderService.updatePurchaseOrder(id, poDto);
	}

	@PutMapping("/{id}/transition")
	@LogActivity("Transition Purchase Order Status")
	public ResponseEntity<?> transitionStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params) {
		try {
			com.nexus.core.entities.PurchaseOrderStatus targetStatus = com.nexus.core.entities.PurchaseOrderStatus
					.valueOf(newStatus.toUpperCase());
			return purchaseOrderService.transitionStatus(id, targetStatus, params);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + newStatus);
		}
	}

	@PostMapping("/{parentPoId}/amend")
	@LogActivity("Create Purchase Order Amendment")
	public ResponseEntity<?> createAmendment(@PathVariable Long parentPoId,
			@RequestBody PurchaseOrderDto amendmentDto) {
		return purchaseOrderService.createAmendment(parentPoId, amendmentDto);
	}

	@GetMapping("/{parentPoId}/amendments")
	@LogActivity("Get Purchase Order Amendments")
	public ResponseEntity<?> getAmendments(@PathVariable Long parentPoId) {
		return purchaseOrderService.getAmendmentsByParentPoId(parentPoId);
	}
}