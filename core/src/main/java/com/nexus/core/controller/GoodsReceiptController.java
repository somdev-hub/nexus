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
import com.nexus.core.payload.GoodsReceiptDto;
import com.nexus.core.service.GoodsReceiptService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/goods-receipts")
@RequiredArgsConstructor
public class GoodsReceiptController {

	private final GoodsReceiptService goodsReceiptService;

	@PostMapping("/create")
	@LogActivity("Create Goods Receipt")
	public ResponseEntity<?> createGoodsReceipt(@Valid @RequestBody GoodsReceiptDto grDto) {
		return goodsReceiptService.createGoodsReceipt(grDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Goods Receipt")
	public ResponseEntity<?> getGoodsReceipt(@PathVariable Long id) {
		return goodsReceiptService.getGoodsReceiptById(id);
	}

	@GetMapping("/all")
	@LogActivity("Get All Goods Receipts")
	public ResponseEntity<?> getAllGoodsReceipts(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long purchaseOrderId,
			@RequestParam(required = false) Long supplierId,
			@PageableDefault(size = 20) Pageable pageable) {
		return goodsReceiptService.getAllGoodsReceipts(status, purchaseOrderId, supplierId, pageable);
	}

	@PutMapping("/{id}/update")
	@LogActivity("Update Goods Receipt")
	public ResponseEntity<?> updateGoodsReceipt(@PathVariable Long id,
			@RequestBody GoodsReceiptDto grDto) {
		return goodsReceiptService.updateGoodsReceipt(id, grDto);
	}

	@PutMapping("/{id}/transition")
	@LogActivity("Transition Goods Receipt Status")
	public ResponseEntity<?> transitionStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params) {
		try {
			com.nexus.core.entities.GoodsReceiptStatus targetStatus = com.nexus.core.entities.GoodsReceiptStatus
					.valueOf(newStatus.toUpperCase());
			return goodsReceiptService.transitionStatus(id, targetStatus, params);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + newStatus);
		}
	}
}