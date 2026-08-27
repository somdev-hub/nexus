package com.nexus.core.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.PartnershipInvitation;
import com.nexus.core.entities.PartnershipInvitationStatus;
import com.nexus.core.entities.PartnershipStatus;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.payload.PartnershipInvitationDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.PartnershipInvitationRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.service.PartnershipInvitationService;
import com.nexus.core.service.PartnershipService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartnershipInvitationServiceImpl implements PartnershipInvitationService {

	private final PartnershipInvitationRepo invitationRepo;
	private final PartnershipRepo partnershipRepo;
	private final AccountRepo accountRepo;
	private final ModelMapper modelMapper;
	private final PartnershipService partnershipService;

	@Override
	public ResponseEntity<?> createInvitation(PartnershipInvitationDto invitationDto) {
		PartnershipInvitation invitation = modelMapper.map(invitationDto, PartnershipInvitation.class);

		// Set inviting and invited organizations
		if (invitationDto.getInvitingOrg() != null) {
			Account invitingOrg = accountRepo.findByAccountId(invitationDto.getInvitingOrg())
					.orElseThrow(() -> new ResourceNotFoundException("Account", "accountId",
							invitationDto.getInvitingOrg()));
			invitation.setInvitingOrg(invitingOrg);
		}

		if (invitationDto.getInvitedOrg() != null) {
			Account invitedOrg = accountRepo.findByAccountId(invitationDto.getInvitedOrg())
					.orElseThrow(
							() -> new ResourceNotFoundException("Account", "accountId", invitationDto.getInvitedOrg()));
			invitation.setInvitedOrg(invitedOrg);
		}

		// Set default status and timestamps
		invitation.setStatus(PartnershipInvitationStatus.PENDING);
		invitation.setInvitedAt(Timestamp.valueOf(LocalDateTime.now()));
		invitation.setExpiresAt(Timestamp.valueOf(LocalDateTime.now().plusDays(30)));

		PartnershipInvitation savedInvitation = invitationRepo.save(invitation);
		return new ResponseEntity<>(modelMapper.map(savedInvitation, PartnershipInvitationDto.class),
				HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> respondToInvitation(Long invitationId, PartnershipInvitationDto responseDto) {
		PartnershipInvitation invitation = invitationRepo
				.findByInvitationIdAndInvitingOrgAccountId(invitationId, responseDto.getInvitingOrg())
				.orElseThrow(
						() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId", invitationId));

		// Update invitation with response
		invitation.setStatus(responseDto.getStatus());
		invitation.setRespondedAt(Timestamp.valueOf(LocalDateTime.now()));
		invitation.setRespondedBy(responseDto.getRespondedBy());
		invitation.setRejectionReason(responseDto.getRejectionReason());

		PartnershipInvitation updatedInvitation = invitationRepo.save(invitation);

		// If accepted, create a partnership
		if (responseDto.getStatus() == PartnershipInvitationStatus.ACCEPTED) {
			PartnershipDto partnershipDto = new PartnershipDto();
			partnershipDto.setPrimaryOrg(invitation.getInvitingOrg().getAccountId());
			partnershipDto.setSecondaryOrg(invitation.getInvitedOrg().getAccountId());
			partnershipDto.setPartnershipTerm(invitation.getProposedTerms());
			partnershipDto.setDiscountRate(invitation.getProposedDiscountRate());
			partnershipDto.setStatus(PartnershipStatus.DRAFT);
			partnershipDto.setStartDate(Timestamp.valueOf(LocalDateTime.now()));
			partnershipDto.setInvitationId(invitationId);

			partnershipService.addPartnership(partnershipDto);
		}

		return new ResponseEntity<>(modelMapper.map(updatedInvitation, PartnershipInvitationDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInvitationById(Long id, Long orgId) {
		// Check if user belongs to either inviting or invited org
		var invitationOpt = invitationRepo.findByInvitationIdAndInvitingOrgAccountId(id, orgId);
		if (invitationOpt.isEmpty()) {
			invitationOpt = invitationRepo.findByInvitationIdAndInvitedOrgAccountId(id, orgId);
		}

		PartnershipInvitation invitation = invitationOpt
				.orElseThrow(() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId", id));

		return new ResponseEntity<>(modelMapper.map(invitation, PartnershipInvitationDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInvitationsByInvitingOrg(Long orgId) {
		List<PartnershipInvitation> invitations = invitationRepo.findByInvitingOrgAccountId(orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Invitations", "orgId", orgId));
		List<PartnershipInvitationDto> invitationDtos = invitations.stream()
				.map(invitation -> modelMapper.map(invitation, PartnershipInvitationDto.class))
				.toList();
		return new ResponseEntity<>(invitationDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInvitationsByInvitedOrg(Long orgId) {
		List<PartnershipInvitation> invitations = invitationRepo.findByInvitedOrgAccountId(orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Invitations", "orgId", orgId));
		List<PartnershipInvitationDto> invitationDtos = invitations.stream()
				.map(invitation -> modelMapper.map(invitation, PartnershipInvitationDto.class))
				.toList();
		return new ResponseEntity<>(invitationDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPendingInvitationsForOrg(Long orgId) {
		List<PartnershipInvitation> invitations = invitationRepo.findPendingInvitationsForOrg(orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Invitations", "orgId", orgId));
		List<PartnershipInvitationDto> invitationDtos = invitations.stream()
				.map(invitation -> modelMapper.map(invitation, PartnershipInvitationDto.class))
				.toList();
		return new ResponseEntity<>(invitationDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> withdrawInvitation(Long invitationId, Long orgId) {
		PartnershipInvitation invitation = invitationRepo.findByInvitationIdAndInvitingOrgAccountId(invitationId, orgId)
				.orElseThrow(
						() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId", invitationId));

		if (invitation.getStatus() != PartnershipInvitationStatus.PENDING) {
			throw new IllegalStateException("Only pending invitations can be withdrawn");
		}

		invitation.setStatus(PartnershipInvitationStatus.WITHDRAWN);
		invitationRepo.save(invitation);

		return new ResponseEntity<>(modelMapper.map(invitation, PartnershipInvitationDto.class), HttpStatus.OK);
	}
}