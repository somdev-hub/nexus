package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PurchaseOrderDto;

import java.util.Map;

public interface PurchaseOrderService {

	ResponseEntity<?> createPurchaseOrder(PurchaseOrderDto poDto);

	ResponseEntity<?> getPurchaseOrderById(Long id);

	ResponseEntity<?> getAllPurchaseOrders(String status, Pageable pageable);

	ResponseEntity<?> updatePurchaseOrder(Long id, PurchaseOrderDto poDto);

	ResponseEntity<?> transitionStatus(Long id, com.nexus.core.entities.PurchaseOrderStatus newStatus,
			Map<String, Object> params);

	ResponseEntity<?> createAmendment(Long parentPoId, PurchaseOrderDto amendmentDto);

	ResponseEntity<?> getAmendmentsByParentPoId(Long parentPoId);
}