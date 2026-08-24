package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PartnershipDto;

public interface PartnershipService {
	public ResponseEntity<?> addPartnership(PartnershipDto partnershipDto);

	public ResponseEntity<?> getPartnershipByIdAndOrg(Long id, Long orgId);

	public ResponseEntity<?> getAllPartnershipsByOrgId(Long orgId);
}