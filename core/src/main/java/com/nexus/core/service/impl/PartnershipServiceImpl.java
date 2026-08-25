package com.nexus.core.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.Partnership;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.service.PartnershipService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PartnershipServiceImpl implements PartnershipService {

	private final PartnershipRepo partnershipRepo;
	private final ModelMapper modelMapper;
	private final AccountRepo accountRepo;

	@Override
	public ResponseEntity<?> addPartnership(PartnershipDto partnershipDto) {
		Partnership partnership = modelMapper.map(partnershipDto, Partnership.class);

		// Set primary and secondary organizations
		if (partnershipDto.getPrimaryOrg() != null) {
			Account primaryOrg = accountRepo.findByAccountId(partnershipDto.getPrimaryOrg())
					.orElseThrow(() -> new ResourceNotFoundException("Account", "accountId",
							partnershipDto.getPrimaryOrg()));
			partnership.setPrimaryOrg(primaryOrg);
		}

		if (partnershipDto.getSecondaryOrg() != null) {
			Account secondaryOrg = accountRepo.findByAccountId(partnershipDto.getSecondaryOrg())
					.orElseThrow(() -> new ResourceNotFoundException("Account", "accountId",
							partnershipDto.getSecondaryOrg()));
			partnership.setSecondaryOrg(secondaryOrg);
		}

		Partnership savedPartnership = partnershipRepo.save(partnership);
		return new ResponseEntity<>(modelMapper.map(savedPartnership, PartnershipDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getPartnershipByIdAndOrg(Long id, Long orgId) {
		Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", id));
		return new ResponseEntity<>(modelMapper.map(partnership, PartnershipDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllPartnershipsByOrgId(Long orgId, Pageable pageable) {
		Page<Partnership> partnerships = partnershipRepo.findByPrimaryOrgAccountId(orgId, pageable);
		Page<PartnershipDto> partnershipDtos = partnerships
				.map(partnership -> modelMapper.map(partnership, PartnershipDto.class));
		return new ResponseEntity<>(partnershipDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> updatePartnershipStatus(Long id, Long orgId,
			com.nexus.core.entities.PartnershipStatus newStatus) {
		Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", id));
		partnership.setStatus(newStatus);
		Partnership savedPartnership = partnershipRepo.save(partnership);
		return new ResponseEntity<>(modelMapper.map(savedPartnership, PartnershipDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPartnershipsByStatus(Long orgId, com.nexus.core.entities.PartnershipStatus status,
			Pageable pageable) {
		Page<Partnership> partnerships = partnershipRepo.findByPrimaryOrgAccountIdAndStatus(orgId, status, pageable);
		Page<PartnershipDto> partnershipDtos = partnerships
				.map(partnership -> modelMapper.map(partnership, PartnershipDto.class));
		return new ResponseEntity<>(partnershipDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getActivePartnershipsByOrgId(Long orgId, Pageable pageable) {
		Page<Partnership> partnerships = partnershipRepo.findActiveByPrimaryOrg(orgId, pageable);
		Page<PartnershipDto> partnershipDtos = partnerships
				.map(partnership -> modelMapper.map(partnership, PartnershipDto.class));
		return new ResponseEntity<>(partnershipDtos, HttpStatus.OK);
	}
}