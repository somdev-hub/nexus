package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PartnershipInvitationDto;

public interface PartnershipInvitationService {
	public ResponseEntity<?> createInvitation(PartnershipInvitationDto invitationDto);

	public ResponseEntity<?> respondToInvitation(Long invitationId, PartnershipInvitationDto responseDto);

	public ResponseEntity<?> getInvitationById(Long id);

	public ResponseEntity<?> getInvitationsByInvitingOrg(Pageable pageable);

	public ResponseEntity<?> getInvitationsByInvitedOrg(Pageable pageable);

	public ResponseEntity<?> getPendingInvitationsForOrg(Pageable pageable);

	public ResponseEntity<?> withdrawInvitation(Long invitationId);
}