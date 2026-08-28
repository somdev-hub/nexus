package com.nexus.core.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.PartnershipInvitationDto;
import com.nexus.core.security.OrganizationContextFilter;
import com.nexus.core.service.PartnershipInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/partnership-invitations")
@RequiredArgsConstructor
public class PartnershipInvitationController {

	private final PartnershipInvitationService invitationService;
	
	@PostMapping("/create")
	@LogActivity("Create Partnership Invitation")
	public ResponseEntity<?> createInvitation(@Valid @RequestBody PartnershipInvitationDto invitationDto) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		// Set inviting organization from context
		invitationDto.setInvitingOrg(orgId);

		return invitationService.createInvitation(invitationDto);
	}

	@PutMapping("/{id}/respond")
	@LogActivity("Respond to Partnership Invitation")
	public ResponseEntity<?> respondToInvitation(@PathVariable Long id,
			@Valid @RequestBody PartnershipInvitationDto responseDto) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		// Set invited organization from context
		responseDto.setInvitedOrg(orgId);

		return invitationService.respondToInvitation(id, responseDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Partnership Invitation")
	public ResponseEntity<?> getInvitation(@PathVariable Long id, @RequestHeader("Authorization") String token) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return invitationService.getInvitationById(id, orgId);
	}

	@GetMapping("/sent")
	@LogActivity("Get Sent Partnership Invitations")
	public ResponseEntity<?> getSentInvitations(@RequestHeader("Authorization") String token) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return invitationService.getInvitationsByInvitingOrg(orgId);
	}

	@GetMapping("/received")
	@LogActivity("Get Received Partnership Invitations")
	public ResponseEntity<?> getReceivedInvitations(@RequestHeader("Authorization") String token) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return invitationService.getInvitationsByInvitedOrg(orgId);
	}

	@GetMapping("/pending")
	@LogActivity("Get Pending Partnership Invitations")
	public ResponseEntity<?> getPendingInvitations(@RequestHeader("Authorization") String token) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return invitationService.getPendingInvitationsForOrg(orgId);
	}

	@PutMapping("/{id}/withdraw")
	@LogActivity("Withdraw Partnership Invitation")
	public ResponseEntity<?> withdrawInvitation(@PathVariable Long id, @RequestHeader("Authorization") String token) {

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return invitationService.withdrawInvitation(id, orgId);
	}

	/**
	 * Extract organization ID from request attributes set by
	 * OrganizationContextFilter.
	 */
	private Long getOrganizationIdFromContext() {
		try {
			jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
					.getRequestAttributes())
					.getRequest();
			if (request != null) {
				Object orgContext = request.getAttribute(OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE);
				if (orgContext != null) {
					com.fasterxml.jackson.databind.JsonNode orgNode = (com.fasterxml.jackson.databind.JsonNode) orgContext;
					return orgNode.path("id").asLong();
				}
			}
		} catch (Exception e) {
			// Ignore and return null
		}
		return null;
	}
}