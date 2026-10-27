package com.nexus.core.service.interfaces;

import java.sql.Timestamp;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ProductionService {
	ResponseEntity<?> createProductionOrder(Long productId, Double quantity, Long warehouseId, String notes, Timestamp deadline);

	ResponseEntity<?> getProductionOrders(Pageable pageable);

	ResponseEntity<?> getProductionOrder(Long id);

	ResponseEntity<?> allocate(Long productionOrderId);

	ResponseEntity<?> startProduction(Long productionOrderId);

	ResponseEntity<?> completeProduction(Long productionOrderId);

	ResponseEntity<?> cancelProduction(Long productionOrderId);

	ResponseEntity<?> releaseBatch(Long stockId);
}