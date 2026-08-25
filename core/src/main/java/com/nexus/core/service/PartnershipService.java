package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PartnershipDto;

public interface PartnershipService {
	public ResponseEntity<?> addPartnership(PartnershipDto partnershipDto);

	public ResponseEntity<?> getPartnershipByIdAndOrg(Long id, Long orgId);

	public ResponseEntity<?> getAllPartnershipsByOrgId(Long orgId, Pageable pageable);

	public ResponseEntity<?> updatePartnershipStatus(Long id, Long orgId,
			com.nexus.core.entities.PartnershipStatus newStatus);

	public ResponseEntity<?> getPartnershipsByStatus(Long orgId, com.nexus.core.entities.PartnershipStatus status,
			Pageable pageable);

	public ResponseEntity<?> getActivePartnershipsByOrgId(Long orgId, Pageable pageable);
}