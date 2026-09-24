package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.ProductionCapacityDto;

public interface ProductionCapacityService {

    ResponseEntity<?> createCapacity(ProductionCapacityDto dto);

    ResponseEntity<?> getCapacity(Long id);

    ResponseEntity<?> getAllCapacities(String productLine, String shift, java.sql.Date periodStart, java.sql.Date periodEnd, Pageable pageable);

    ResponseEntity<?> updateCapacity(Long id, ProductionCapacityDto dto);

    ResponseEntity<?> deleteCapacity(Long id);

    ResponseEntity<?> getCapacitySummary();
}
