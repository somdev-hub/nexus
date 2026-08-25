package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PartnershipInvitationDto;

public interface PartnershipInvitationService {
	public ResponseEntity<?> createInvitation(PartnershipInvitationDto invitationDto);

	public ResponseEntity<?> respondToInvitation(Long invitationId, PartnershipInvitationDto responseDto);

	public ResponseEntity<?> getInvitationById(Long id, Long orgId);

	public ResponseEntity<?> getInvitationsByInvitingOrg(Long orgId);

	public ResponseEntity<?> getInvitationsByInvitedOrg(Long orgId);

	public ResponseEntity<?> getPendingInvitationsForOrg(Long orgId);

	public ResponseEntity<?> withdrawInvitation(Long invitationId, Long orgId);
}