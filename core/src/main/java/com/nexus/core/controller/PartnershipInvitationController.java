package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.PartnershipInvitationDto;
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
		return invitationService.createInvitation(invitationDto);
	}

	@PutMapping("/{id}/respond")
	@LogActivity("Respond to Partnership Invitation")
	public ResponseEntity<?> respondToInvitation(@PathVariable Long id,
			@Valid @RequestBody PartnershipInvitationDto responseDto) {
		return invitationService.respondToInvitation(id, responseDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Partnership Invitation")
	public ResponseEntity<?> getInvitation(@PathVariable Long id) {
		return invitationService.getInvitationById(id);
	}

	@GetMapping("/sent")
	@LogActivity("Get Sent Partnership Invitations")
	public ResponseEntity<?> getSentInvitations(@PageableDefault(size = 20) Pageable pageable) {
		return invitationService.getInvitationsByInvitingOrg(pageable);
	}

	@GetMapping("/received")
	@LogActivity("Get Received Partnership Invitations")
	public ResponseEntity<?> getReceivedInvitations(@PageableDefault(size = 20) Pageable pageable) {
		return invitationService.getInvitationsByInvitedOrg(pageable);
	}

	@GetMapping("/pending")
	@LogActivity("Get Pending Partnership Invitations")
	public ResponseEntity<?> getPendingInvitations(@PageableDefault(size = 20) Pageable pageable) {
		return invitationService.getPendingInvitationsForOrg(pageable);
	}

	@PutMapping("/{id}/withdraw")
	@LogActivity("Withdraw Partnership Invitation")
	public ResponseEntity<?> withdrawInvitation(@PathVariable Long id) {
		return invitationService.withdrawInvitation(id);
	}
}