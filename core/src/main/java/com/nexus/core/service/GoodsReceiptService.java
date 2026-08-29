package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.GoodsReceiptDto;

import java.util.Map;

public interface GoodsReceiptService {

	ResponseEntity<?> createGoodsReceipt(GoodsReceiptDto grDto);

	ResponseEntity<?> getGoodsReceiptById(Long id);

	ResponseEntity<?> getAllGoodsReceipts(String status, Long purchaseOrderId, Long supplierId, Pageable pageable);

	ResponseEntity<?> updateGoodsReceipt(Long id, GoodsReceiptDto grDto);

	ResponseEntity<?> transitionStatus(Long id, com.nexus.core.entities.GoodsReceiptStatus newStatus,
			Map<String, Object> params);
}