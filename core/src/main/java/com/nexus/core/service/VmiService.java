package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.VmiConfigDto;

public interface VmiService {

    ResponseEntity<?> createVmi(VmiConfigDto dto);

    ResponseEntity<?> getVmi(Long id);

    ResponseEntity<?> getAllVmis(Long retailerOrgId, Long warehouseId, Long materialId, Boolean autoReplenish, Pageable pageable);

    ResponseEntity<?> updateVmi(Long id, VmiConfigDto dto);

    ResponseEntity<?> deleteVmi(Long id);

    ResponseEntity<?> triggerReplenishment(Long id);

    ResponseEntity<?> getReplenishmentSuggestions();
}
