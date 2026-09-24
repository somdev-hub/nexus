package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierPriceTierDto;

import java.sql.Date;

public interface SupplierPriceTierService {

    ResponseEntity<?> createPriceTier(SupplierPriceTierDto dto);

    ResponseEntity<?> getPriceTier(Long id);

    ResponseEntity<?> getAllPriceTiers(Long catalogId, String customerSegment, Long contractId, Date validFrom, Date validTo, Pageable pageable);

    ResponseEntity<?> updatePriceTier(Long id, SupplierPriceTierDto dto);

    ResponseEntity<?> deletePriceTier(Long id);

    ResponseEntity<?> getPriceForQuantity(Long catalogId, Double quantity, String customerSegment);
}
