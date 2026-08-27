package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PurchaseOrderDto;

import java.util.Map;

public interface PurchaseOrderService {

	ResponseEntity<?> createPurchaseOrder(PurchaseOrderDto poDto);

	ResponseEntity<?> getPurchaseOrderByIdAndOrg(Long id, Long orgId);

	ResponseEntity<?> getAllPurchaseOrdersByOrgId(Long orgId, Pageable pageable);

	ResponseEntity<?> getPurchaseOrdersByOrgIdAndStatus(Long orgId, com.nexus.core.entities.PurchaseOrderStatus status,
			Pageable pageable);

	ResponseEntity<?> updatePurchaseOrder(Long id, PurchaseOrderDto poDto, Long orgId);

	ResponseEntity<?> transitionStatus(Long id, com.nexus.core.entities.PurchaseOrderStatus newStatus,
			Map<String, Object> params, Long orgId);

	ResponseEntity<?> createAmendment(Long parentPoId, PurchaseOrderDto amendmentDto, Long orgId);

	ResponseEntity<?> getAmendmentsByParentPoId(Long parentPoId, Long orgId);
}