package com.nexus.core.service.implementations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.PartnershipInvitation;
import com.nexus.core.model.enums.PartnershipInvitationStatus;
import com.nexus.core.model.enums.PartnershipStatus;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.payload.PartnershipInvitationDto;
import com.nexus.core.repository.PartnershipInvitationRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.AccountDirectory;
import com.nexus.core.service.interfaces.PartnershipInvitationService;
import com.nexus.core.service.interfaces.PartnershipService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartnershipInvitationServiceImpl implements PartnershipInvitationService {

	private final PartnershipInvitationRepo invitationRepo;
	private final AccountDirectory accountDirectory;
	private final SupplierRepository supplierRepo;
	private final ModelMapper modelMapper;
	private final PartnershipService partnershipService;

	@Override
	public ResponseEntity<?> createInvitation(PartnershipInvitationDto invitationDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		PartnershipInvitation invitation = modelMapper.map(invitationDto, PartnershipInvitation.class);

		// Set inviting organization from context. Lazily provision the Core
		// Account row: a freshly registered org may not have touched Core yet.
		Account invitingOrg = accountDirectory.getOrCreateAccount(orgId);
		invitation.setInvitingOrg(invitingOrg);

		if (invitationDto.getInvitedOrg() != null) {
			// Same lazy provisioning for the counterparty: inviting an org
			// that exists in IAM but has never used Core must not 404.
			Account invitedOrg = accountDirectory.getOrCreateAccount(invitationDto.getInvitedOrg());
			invitation.setInvitedOrg(invitedOrg);
		}

		// One pending invitation per counterparty: resending while the
		// previous invite is still undecided is rejected so the button state
		// ("disabled until accepted or rejected") holds server-side too.
		if (invitation.getInvitedOrg() != null && invitationRepo
				.existsByInvitingOrgAccountIdAndInvitedOrgAccountIdAndStatus(orgId,
						invitation.getInvitedOrg().getAccountId(), PartnershipInvitationStatus.PENDING)) {
			throw new ValidationException(
					"An invitation to this organization is already pending");
		}

		// Set default status and timestamps
		invitation.setStatus(PartnershipInvitationStatus.PENDING);
		invitation.setInvitedAt(Timestamp.valueOf(LocalDateTime.now()));
		invitation.setExpiresAt(Timestamp.valueOf(LocalDateTime.now().plusDays(30)));

		PartnershipInvitation savedInvitation = invitationRepo.save(invitation);
		return new ResponseEntity<>(toDto(savedInvitation), HttpStatus.CREATED);
	}

	/**
	 * ModelMapper cannot convert Account relations to the Long ids on the DTO,
	 * so it silently leaves invitingOrg/invitedOrg null. Map scalar fields
	 * with ModelMapper, then stamp the org ids explicitly.
	 */
	private PartnershipInvitationDto toDto(PartnershipInvitation invitation) {
		PartnershipInvitationDto dto = modelMapper.map(invitation, PartnershipInvitationDto.class);
		if (invitation.getInvitingOrg() != null) {
			dto.setInvitingOrg(invitation.getInvitingOrg().getAccountId());
		}
		if (invitation.getInvitedOrg() != null) {
			dto.setInvitedOrg(invitation.getInvitedOrg().getAccountId());
		}
		return dto;
	}

	@Override
	public ResponseEntity<?> respondToInvitation(Long invitationId, PartnershipInvitationDto responseDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		PartnershipInvitation invitation = invitationRepo.findByInvitationIdAndInvitedOrgAccountId(invitationId, orgId)
				.orElseThrow(
						() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId", invitationId));

		if (responseDto.getStatus() == null) {
			throw new ValidationException("Response status is required");
		}

		// Update invitation with response
		invitation.setStatus(responseDto.getStatus());
		invitation.setRespondedAt(Timestamp.valueOf(LocalDateTime.now()));
		invitation.setRespondedBy(responseDto.getRespondedBy());
		invitation.setRejectionReason(responseDto.getRejectionReason());

		PartnershipInvitation updatedInvitation = invitationRepo.save(invitation);

		// If accepted, create a partnership
		if (responseDto.getStatus() == PartnershipInvitationStatus.ACCEPTED) {
			// Link the retailer-local Supplier row to the supplier counterparty org.
			if ("RETAILER_SUPPLIER".equals(invitation.getPartnershipContext())
					&& invitation.getRetailerSupplierId() != null) {
				supplierRepo.findById(invitation.getRetailerSupplierId()).ifPresent(supplier -> {
					Long retailerAccountId = supplier.getAccount() != null
							? supplier.getAccount().getAccountId()
							: null;
					Account counterparty = null;
					if (retailerAccountId != null && invitation.getInvitingOrg() != null
							&& retailerAccountId.equals(invitation.getInvitingOrg().getAccountId())) {
						counterparty = invitation.getInvitedOrg();
					} else {
						counterparty = invitation.getInvitingOrg();
					}
					if (counterparty != null) {
						supplier.setSupplierOrgAccountId(counterparty.getAccountId());
						supplierRepo.save(supplier);
					}
				});
			}

			PartnershipDto partnershipDto = new PartnershipDto();
			partnershipDto.setPrimaryOrg(invitation.getInvitingOrg().getAccountId());
			partnershipDto.setSecondaryOrg(invitation.getInvitedOrg().getAccountId());
			partnershipDto.setPartnershipTerm(invitation.getProposedTerms());
			partnershipDto.setDiscountRate(invitation.getProposedDiscountRate());
			String context = invitation.getPartnershipContext();
			if (context != null) {
				if (context.endsWith("LOGISTICS")) {
					partnershipDto.setPartnershipType("LOGISTICS");
				} else if (context.endsWith("SUPPLIER")) {
					partnershipDto.setPartnershipType("SUPPLIER");
				} else {
					partnershipDto.setPartnershipType(context);
				}
			}
			partnershipDto.setStatus(PartnershipStatus.DRAFT);
			partnershipDto.setStartDate(Timestamp.valueOf(LocalDateTime.now()));
			partnershipDto.setInvitationId(invitationId);

			partnershipService.addPartnership(partnershipDto);
		}

		return new ResponseEntity<>(toDto(updatedInvitation), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInvitationById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// Check if user belongs to either inviting or invited org
		var invitationOpt = invitationRepo.findByInvitationIdAndInvitingOrgAccountId(id, orgId);
		if (invitationOpt.isEmpty()) {
			invitationOpt = invitationRepo.findByInvitationIdAndInvitedOrgAccountId(id, orgId);
		}

		PartnershipInvitation invitation = invitationOpt
				.orElseThrow(() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId", id));

		return new ResponseEntity<>(toDto(invitation), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInvitationsByInvitingOrg(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<PartnershipInvitation> invitations = invitationRepo.findByInvitingOrgAccountId(orgId, pageable);
		return new ResponseEntity<>(invitations.map(this::toDto), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInvitationsByInvitedOrg(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<PartnershipInvitation> invitations = invitationRepo.findByInvitedOrgAccountId(orgId, pageable);
		return new ResponseEntity<>(invitations.map(this::toDto), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPendingInvitationsForOrg(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<PartnershipInvitation> invitations = invitationRepo.findPendingInvitationsForOrg(orgId, pageable);
		return new ResponseEntity<>(invitations.map(this::toDto), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> withdrawInvitation(Long invitationId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PartnershipInvitation invitation = invitationRepo.findByInvitationIdAndInvitingOrgAccountId(invitationId, orgId)
				.orElseThrow(
						() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId", invitationId));

		if (invitation.getStatus() != PartnershipInvitationStatus.PENDING) {
			throw new IllegalStateException("Only pending invitations can be withdrawn");
		}

		invitation.setStatus(PartnershipInvitationStatus.WITHDRAWN);
		invitationRepo.save(invitation);

		return new ResponseEntity<>(toDto(invitation), HttpStatus.OK);
	}
}