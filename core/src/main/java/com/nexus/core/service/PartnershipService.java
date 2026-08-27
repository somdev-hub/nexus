package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

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

	// Partnership Agreement with DMS integration
	public ResponseEntity<?> uploadPartnershipAgreement(Long partnershipId, Long orgId, MultipartFile file,
			String documentName, String remarks, String authToken);

	public ResponseEntity<?> getPartnershipAgreement(Long partnershipId, Long orgId, String authToken);

	public ResponseEntity<?> deletePartnershipAgreement(Long partnershipId, Long orgId, String authToken);

	// Partnership Lifecycle Management
	public ResponseEntity<?> transitionPartnershipStatus(Long id, Long orgId,
			com.nexus.core.entities.PartnershipStatus newStatus, String reason, String authToken);
}