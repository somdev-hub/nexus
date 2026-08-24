package com.nexus.core.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.Partnership;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ErrorResponse;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.service.PartnershipService;

@Service
public class PartnershipServiceImpl implements PartnershipService {

	@Autowired
	private PartnershipRepo partnershipRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private AccountRepo accountRepo;

	@Override
	public ResponseEntity<?> addPartnership(PartnershipDto partnershipDto) {
		if (ObjectUtils.isEmpty(partnershipDto) || ObjectUtils.isEmpty(partnershipDto.getPrimaryOrg())
				|| ObjectUtils.isEmpty(partnershipDto.getSecondaryOrg())) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse("Empty Details sent", HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()), "Necessary details are not sent!"),
					HttpStatus.BAD_REQUEST);

		}
		try {

			Partnership partnership = modelMapper.map(partnershipDto, Partnership.class);

			// Set primary and secondary organizations
			if (partnershipDto.getPrimaryOrg() != null) {
				Account primaryOrg = accountRepo.findById(partnershipDto.getPrimaryOrg()).orElse(null);
				partnership.setPrimaryOrg(primaryOrg);
			}

			if (partnershipDto.getSecondaryOrg() != null) {
				Account secondaryOrg = accountRepo.findById(partnershipDto.getSecondaryOrg()).orElse(null);
				partnership.setSecondaryOrg(secondaryOrg);
			}

			Partnership savedPartnership = partnershipRepo.save(partnership);
			return new ResponseEntity<>(modelMapper.map(savedPartnership, PartnershipDto.class), HttpStatus.CREATED);

		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse("Failed to add partnership", HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()), e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public ResponseEntity<?> getPartnershipByIdAndOrg(Long id, Long orgId) {
		if (ObjectUtils.isEmpty(id) || ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Partnership ID and Organization ID cannot be null or empty",
							HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							"Invalid Partnership ID or Organization ID"),
					HttpStatus.BAD_REQUEST);
		}
		try {
			Partnership partnership = partnershipRepo.findByIdAndPrimaryOrg(id, orgId).orElse(null);
			if (ObjectUtils.isEmpty(partnership)) {
				return new ResponseEntity<ErrorResponse>(
						new ErrorResponse(
								"Partnership not found in organization",
								HttpStatus.NOT_FOUND.value(),
								Timestamp.valueOf(LocalDateTime.now()),
								"No partnership found with the given ID in the organization"),
						HttpStatus.NOT_FOUND);
			}
			return new ResponseEntity<>(modelMapper.map(partnership, PartnershipDto.class), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Failed to retrieve partnership",
							HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<?> getAllPartnershipsByOrgId(Long orgId) {
		if (ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Organization ID cannot be null or empty",
							HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							"Invalid Organization ID"),
					HttpStatus.BAD_REQUEST);

		}
		try {
			List<Partnership> partnerships = partnershipRepo.findByPrimaryOrg(orgId).orElseThrow(() -> {
				throw new ResourceNotFoundException("Partnerships", "orgId", orgId);
			});
			List<PartnershipDto> partnershipDtos = new java.util.ArrayList<>();
			for (Partnership partnership : partnerships) {
				partnershipDtos.add(modelMapper.map(partnership, PartnershipDto.class));
			}
			return new ResponseEntity<>(partnershipDtos, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Failed to retrieve partnerships",
							HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}