package com.nexus.core.service.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.entities.Account;
import com.nexus.core.entities.Partnership;
import com.nexus.core.entities.PartnershipStatus;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.service.PartnershipService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.RestService;
import com.nexus.core.utils.WebConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnershipServiceImpl implements PartnershipService {

	private final PartnershipRepo partnershipRepo;
	private final ModelMapper modelMapper;
	private final AccountRepo accountRepo;
	private final WebConstants webConstants;
	private final CommonUtils commonUtils;
	private final RestService restService;
	private final ObjectMapper objectMapper;

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

	// ============================================
	// Partnership Agreement with DMS Integration
	// ============================================

	@Override
	public ResponseEntity<?> uploadPartnershipAgreement(Long partnershipId, Long orgId, MultipartFile file,
			String documentName, String remarks, String authToken) {
		try {
			// Validate partnership exists and belongs to the organization
			Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(partnershipId, orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));

			// Validate file
			if (file == null || file.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is required");
			}

			// Use RestService to upload to DMS
			ResponseEntity<String> dmsResponse = restService.uploadToDmsOrg(
					file,
					documentName != null ? documentName : file.getOriginalFilename(),
					orgId,
					remarks != null ? remarks : "Partnership agreement for partnership ID: " + partnershipId,
					"CONTRACT",
					"RETAILER", // Default, could be determined from org
					authToken,
					orgId);

			if (dmsResponse.getStatusCode().is2xxSuccessful() && dmsResponse.getBody() != null) {
				// Extract document ID from DMS response
				JsonNode responseNode = objectMapper.readTree(dmsResponse.getBody());
				Long documentId = responseNode.path("id").asLong();

				// Update partnership with agreement document ID
				partnership.setAgreementDocumentId(documentId);
				partnershipRepo.save(partnership);

				return ResponseEntity.ok(dmsResponse.getBody());
			} else {
				return ResponseEntity.status(dmsResponse.getStatusCode())
						.body("Failed to upload agreement to DMS: " + dmsResponse.getBody());
			}

		} catch (IOException e) {
			log.error("Error uploading partnership agreement", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error uploading agreement: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error uploading partnership agreement", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error uploading agreement: " + e.getMessage());
		}
	}

	@Override
	public ResponseEntity<?> getPartnershipAgreement(Long partnershipId, Long orgId, String authToken) {
		try {
			// Validate partnership exists and belongs to the organization
			Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(partnershipId, orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));

			if (partnership.getAgreementDocumentId() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("No agreement document found for this partnership");
			}

			// Use RestService to get document from DMS
			ResponseEntity<String> dmsResponse = restService.getFromDms(
					partnership.getAgreementDocumentId().toString(),
					authToken,
					orgId);

			return ResponseEntity.status(dmsResponse.getStatusCode()).body(dmsResponse.getBody());

		} catch (Exception e) {
			log.error("Error retrieving partnership agreement", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving agreement: " + e.getMessage());
		}
	}

	@Override
	public ResponseEntity<?> deletePartnershipAgreement(Long partnershipId, Long orgId, String authToken) {
		try {
			// Validate partnership exists and belongs to the organization
			Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(partnershipId, orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));

			if (partnership.getAgreementDocumentId() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body("No agreement document found for this partnership");
			}

			// Use RestService to delete document from DMS
			ResponseEntity<String> dmsResponse = restService.deleteFromDms(
					partnership.getAgreementDocumentId().toString(),
					authToken,
					orgId);

			if (dmsResponse.getStatusCode().is2xxSuccessful()) {
				// Clear agreement document ID from partnership
				partnership.setAgreementDocumentId(null);
				partnershipRepo.save(partnership);

				return ResponseEntity.ok("Agreement document deleted successfully");
			} else {
				return ResponseEntity.status(dmsResponse.getStatusCode())
						.body("Failed to delete agreement from DMS: " + dmsResponse.getBody());
			}

		} catch (Exception e) {
			log.error("Error deleting partnership agreement", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error deleting agreement: " + e.getMessage());
		}
	}

	// ============================================
	// Partnership Lifecycle Management
	// ============================================

	@Override
	public ResponseEntity<?> transitionPartnershipStatus(Long id, Long orgId,
			com.nexus.core.entities.PartnershipStatus newStatus, String reason, String authToken) {
		try {
			// Validate partnership exists and belongs to the organization
			Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(id, orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", id));

			PartnershipStatus currentStatus = partnership.getStatus();

			// Validate state transition
			if (!isValidTransition(currentStatus, newStatus)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Invalid status transition from " + currentStatus + " to " + newStatus);
			}

			// Update status
			partnership.setStatus(newStatus);

			// Handle specific transitions
			switch (newStatus) {
				case ACTIVE:
					if (partnership.getStartDate() == null) {
						partnership.setStartDate(Timestamp.valueOf(LocalDateTime.now()));
					}
					break;
				case SUSPENDED:
					// Could add suspension reason tracking
					break;
				case TERMINATED:
					if (partnership.getEndDate() == null) {
						partnership.setEndDate(Timestamp.valueOf(LocalDateTime.now()));
					}
					break;
				case RENEWAL_PENDING:
					// Could add renewal tracking
					break;
				default:
					break;
			}

			Partnership savedPartnership = partnershipRepo.save(partnership);

			// Log the transition (could be enhanced with audit trail)
			log.info("Partnership {} transitioned from {} to {} by org {}. Reason: {}",
					id, currentStatus, newStatus, orgId, reason);

			return new ResponseEntity<>(modelMapper.map(savedPartnership, PartnershipDto.class), HttpStatus.OK);

		} catch (Exception e) {
			log.error("Error transitioning partnership status", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error transitioning partnership status: " + e.getMessage());
		}
	}

	/**
	 * Validates if a partnership status transition is allowed according to the
	 * lifecycle:
	 * DRAFT → PENDING_REVIEW → NEGOTIATING → ACTIVE
	 * PENDING_REVIEW → REJECTED
	 * ACTIVE → SUSPENDED → TERMINATED
	 * ACTIVE → RENEWAL_PENDING → ACTIVE
	 * ACTIVE → RENEWAL_PENDING → TERMINATED
	 */
	private boolean isValidTransition(PartnershipStatus from, PartnershipStatus to) {
		// Same status is not a transition
		if (from == to) {
			return false;
		}

		// Define valid transitions
		return switch (from) {
			case DRAFT -> to == PartnershipStatus.PENDING_REVIEW;
			case PENDING_REVIEW -> to == PartnershipStatus.NEGOTIATING || to == PartnershipStatus.REJECTED;
			case NEGOTIATING -> to == PartnershipStatus.ACTIVE || to == PartnershipStatus.REJECTED;
			case ACTIVE -> to == PartnershipStatus.SUSPENDED || to == PartnershipStatus.TERMINATED
					|| to == PartnershipStatus.RENEWAL_PENDING;
			case SUSPENDED -> to == PartnershipStatus.ACTIVE || to == PartnershipStatus.TERMINATED;
			case TERMINATED -> false; // Terminal state
			case REJECTED -> false; // Terminal state
			case RENEWAL_PENDING -> to == PartnershipStatus.ACTIVE || to == PartnershipStatus.TERMINATED;
		};
	}
}