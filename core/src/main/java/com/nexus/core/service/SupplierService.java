package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierDto;
import com.nexus.core.payload.SupplierDiscoveryDto;

public interface SupplierService {
	public ResponseEntity<?> addSupplier(SupplierDto supplierDto);

	public ResponseEntity<?> getSupplierById(Long id);

	public ResponseEntity<?> getAllSuppliers(Long accountId, String category, String location, Double minRating,
			String certification, Pageable pageable);

	public ResponseEntity<?> discoverSuppliers(SupplierDiscoveryDto discoveryDto, Pageable pageable);
}