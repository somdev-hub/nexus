package com.nexus.core.service.interfaces;

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

    /**
     * Read-only capacity check for the quotation dialog: remaining capacity
     * of the caller's org for a catalog item (+ optional variant) over the
     * requested window, compared against the requested quantity.
     */
    ResponseEntity<?> checkAvailability(Long catalogId, Long variantId, Double quantity,
            java.sql.Date periodStart, java.sql.Date periodEnd);
}
