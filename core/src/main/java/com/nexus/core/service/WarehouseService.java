package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.WarehouseDto;

public interface WarehouseService {
	public ResponseEntity<?> addWarehouse(WarehouseDto warehouseDto);

	public ResponseEntity<?> getWarehouseByIdAndOrg(Long id, Long orgId);

	public ResponseEntity<?> getAllWarehousesByOrgId(Long orgId, Pageable pageable);
}