package com.nexus.core.service.implementations;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.enums.PartnershipStatus;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.service.interfaces.PartnershipService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.RestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnershipServiceImpl implements PartnershipService {

	private final PartnershipRepo partnershipRepo;
	private final ModelMapper modelMapper;
	private final AccountRepo accountRepo;
	private final RestService restService;
	private final ObjectMapper objectMapper;
	private final CommonUtils commonUtils;

	@Override
	public ResponseEntity<?> addPartnership(PartnershipDto partnershipDto) {
		return addPartnership(partnershipDto,
				com.nexus.core.security.OrganizationContextHolder.getCurrentAuthToken());
	}

	@Override
	public ResponseEntity<?> addPartnership(PartnershipDto partnershipDto, String authToken) {
		Partnership partnership = modelMapper.map(partnershipDto, Partnership.class);

		// Set primary and secondary organizations
		if (partnershipDto.getPrimaryOrg() != null) {
			Account primaryOrg = accountRepo.findByAccountId(partnershipDto.getPrimaryOrg())
					.orElseThrow(() -> new ResourceNotFoundException("Account", "accountId",
							partnershipDto.getPrimaryOrg()));
			partnership.setPrimaryOrg(primaryOrg);
			partnership.setPrimaryOrgName(resolveOrgName(primaryOrg, authToken));
		}

		if (partnershipDto.getSecondaryOrg() != null) {
			Account secondaryOrg = accountRepo.findByAccountId(partnershipDto.getSecondaryOrg())
					.orElseThrow(() -> new ResourceNotFoundException("Account", "accountId",
							partnershipDto.getSecondaryOrg()));
			partnership.setSecondaryOrg(secondaryOrg);
			partnership.setSecondaryOrgName(resolveOrgName(secondaryOrg, authToken));
		}

		Partnership savedPartnership = partnershipRepo.save(partnership);
		return new ResponseEntity<>(toDto(savedPartnership), HttpStatus.CREATED);
	}

	private String backfillOrgName(Account account) {
		String token = com.nexus.core.security.OrganizationContextHolder.getCurrentAuthToken();
		String name = resolveOrgName(account, token);
		if (name != null) {
			return name;
		}
		return null;
	}

	/**
	 * Resolve a human-readable org name: stored on the Account first, otherwise
	 * from IAM's organization endpoint (and persisted on the Account so later
	 * reads are local). Returns null if neither source has a name.
	 */
	private String resolveOrgName(Account account, String authToken) {
		if (account == null) {
			return null;
		}
		if (account.getName() != null && !account.getName().isBlank()) {
			return account.getName();
		}
		if (authToken == null || authToken.isBlank() || account.getAccountId() == null) {
			return null;
		}
		try {
			ResponseEntity<String> response = commonUtils.getOrganizationFromIam(account.getAccountId(), authToken);
			if (response.getBody() != null) {
				JsonNode root = objectMapper.readTree(response.getBody());
				String name = firstNonBlank(root, "orgName", "name", "organizationName");
				if (name != null) {
					account.setName(name);
					accountRepo.save(account);
					return name;
				}
			}
		} catch (Exception e) {
			log.debug("Could not resolve org name for account {}: {}", account.getAccountId(), e.getMessage());
		}
		return null;
	}

	private String firstNonBlank(JsonNode root, String... fields) {
		for (String field : fields) {
			JsonNode node = root.get(field);
			if (node != null && node.isTextual() && !node.asText().isBlank()) {
				return node.asText();
			}
		}
		return null;
	}

	/**
	 * ModelMapper cannot convert Account relations to the Long ids / names on
	 * the DTO, so it silently leaves them null. Map scalars with ModelMapper,
	 * then stamp org ids and names explicitly.
	 */
	private PartnershipDto toDto(Partnership partnership) {
		PartnershipDto dto = new PartnershipDto();
		if (partnership.getPartnershipId() != null) {
			dto.setPartnershipId(partnership.getPartnershipId());
		}
		dto.setPartnershipTerm(partnership.getPartnershipTerm());
		dto.setPartnershipType(partnership.getPartnershipType());
		dto.setDiscountRate(partnership.getDiscountRate());
		dto.setStatus(partnership.getStatus());
		dto.setStartDate(partnership.getStartDate());
		dto.setEndDate(partnership.getEndDate());
		dto.setRevivedDate(partnership.getRevivedDate());
		dto.setAgreementDocumentId(partnership.getAgreementDocumentId());
		dto.setInvitationId(partnership.getInvitationId());
		dto.setCreatedAt(partnership.getCreatedAt());
		dto.setUpdatedAt(partnership.getUpdatedAt());
		if (partnership.getPrimaryOrg() != null) {
			dto.setPrimaryOrg(partnership.getPrimaryOrg().getAccountId());
			String name = partnership.getPrimaryOrgName();
			if (name == null || name.isBlank()) {
				name = partnership.getPrimaryOrg().getName();
			}
			if ((name == null || name.isBlank()) && partnership.getPrimaryOrg().getAccountId() != null) {
				name = backfillOrgName(partnership.getPrimaryOrg());
				if (name != null) {
					partnership.setPrimaryOrgName(name);
				}
			}
			dto.setPrimaryOrgName(name);
		}
		if (partnership.getSecondaryOrg() != null) {
			dto.setSecondaryOrg(partnership.getSecondaryOrg().getAccountId());
			String name = partnership.getSecondaryOrgName();
			if (name == null || name.isBlank()) {
				name = partnership.getSecondaryOrg().getName();
			}
			if ((name == null || name.isBlank()) && partnership.getSecondaryOrg().getAccountId() != null) {
				name = backfillOrgName(partnership.getSecondaryOrg());
				if (name != null) {
					partnership.setSecondaryOrgName(name);
				}
			}
			dto.setSecondaryOrgName(name);
		}
		if (partnership.getPartnershipId() != null
				&& (partnership.getPrimaryOrgName() != null || partnership.getSecondaryOrgName() != null)) {
			// Persist backfilled names so subsequent reads are local
			try {
				partnershipRepo.save(partnership);
			} catch (Exception e) {
				log.debug("Could not persist org names for partnership {}: {}", partnership.getPartnershipId(),
						e.getMessage());
			}
		}
		return dto;
	}

	@Override
	public ResponseEntity<?> getPartnershipByIdAndOrg(Long id, Long orgId) {
		Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", id));
		return new ResponseEntity<>(toDto(partnership), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllPartnershipsByOrgId(Long orgId, Pageable pageable) {
		return getAllPartnershipsByOrgId(orgId, pageable, null);
	}

	@Override
	public ResponseEntity<?> getAllPartnershipsByOrgId(Long orgId, Pageable pageable, String partnershipType) {
		Page<Partnership> partnerships = partnershipRepo.findByPrimaryOrgAccountId(orgId, pageable);
		Page<PartnershipDto> partnershipDtos = partnerships
				.map(partnership -> toDto(partnership));
		if (partnershipType != null && !partnershipType.isBlank()) {
			partnershipDtos = filterByType(partnershipDtos, partnershipType, pageable);
		}
		return new ResponseEntity<>(partnershipDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getMyPartnerships(Long orgId, Pageable pageable) {
		return getMyPartnerships(orgId, pageable, null);
	}

	@Override
	public ResponseEntity<?> getMyPartnerships(Long orgId, Pageable pageable, String partnershipType) {
		// Both sides of a partnership: primary (usually the inviter) and
		// secondary (usually the invited supplier/logistics org). Readers
		// like suppliers would otherwise see an empty list.
		List<Partnership> primary = partnershipRepo.findByPrimaryOrgAccountId(orgId, Pageable.unpaged())
				.getContent();
		List<Partnership> secondary = partnershipRepo.findBySecondaryOrgAccountId(orgId, Pageable.unpaged())
				.getContent();
		List<PartnershipDto> all = new java.util.ArrayList<>(primary.size() + secondary.size());
		for (Partnership p : primary) {
			all.add(toDto(p));
		}
		for (Partnership p : secondary) {
			all.add(toDto(p));
		}
		if (partnershipType != null && !partnershipType.isBlank()) {
			all = all.stream()
					.filter(dto -> partnershipType.equalsIgnoreCase(dto.getPartnershipType()))
					.collect(java.util.stream.Collectors.toList());
		}
		all.sort((a, b) -> Long.compare(
				b.getPartnershipId() == null ? 0L : b.getPartnershipId(),
				a.getPartnershipId() == null ? 0L : a.getPartnershipId()));
		int total = all.size();
		int start = (int) Math.min(pageable.getOffset(), total);
		int end = Math.min(start + pageable.getPageSize(), total);
		List<PartnershipDto> content = all.subList(start, end);
		return new ResponseEntity<>(
				new org.springframework.data.domain.PageImpl<>(content, pageable, total), HttpStatus.OK);
	}

	private Page<PartnershipDto> filterByType(Page<PartnershipDto> page, String partnershipType,
			Pageable pageable) {
		List<PartnershipDto> filtered = page.getContent().stream()
				.filter(dto -> partnershipType.equalsIgnoreCase(dto.getPartnershipType()))
				.collect(java.util.stream.Collectors.toList());
		return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
	}

	@Override
	public ResponseEntity<?> updatePartnership(Long id, Long orgId, PartnershipDto dto) {
		// Either party to the partnership may edit its commercial terms.
		Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(id, orgId)
				.or(() -> partnershipRepo.findByPartnershipIdAndSecondaryOrgAccountId(id, orgId))
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", id));
		if (dto.getPartnershipTerm() != null) {
			partnership.setPartnershipTerm(dto.getPartnershipTerm());
		}
		if (dto.getDiscountRate() != null) {
			partnership.setDiscountRate(dto.getDiscountRate());
		}
		if (dto.getStartDate() != null) {
			partnership.setStartDate(dto.getStartDate());
		}
		if (dto.getEndDate() != null) {
			partnership.setEndDate(dto.getEndDate());
		}
		Partnership saved = partnershipRepo.save(partnership);
		return new ResponseEntity<>(toDto(saved), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> updatePartnershipStatus(Long id, Long orgId,
			com.nexus.core.model.enums.PartnershipStatus newStatus) {		Partnership partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(id, orgId)
				.or(() -> partnershipRepo.findByPartnershipIdAndSecondaryOrgAccountId(id, orgId))
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", id));
		partnership.setStatus(newStatus);
		Partnership savedPartnership = partnershipRepo.save(partnership);
		return new ResponseEntity<>(toDto(savedPartnership), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getPartnershipsByStatus(Long orgId, com.nexus.core.model.enums.PartnershipStatus status,
			Pageable pageable) {
		Page<Partnership> partnerships = partnershipRepo.findByPrimaryOrgAccountIdAndStatus(orgId, status, pageable);
		Page<PartnershipDto> partnershipDtos = partnerships
				.map(partnership -> toDto(partnership));
		return new ResponseEntity<>(partnershipDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getActivePartnershipsByOrgId(Long orgId, Pageable pageable) {
		Page<Partnership> partnerships = partnershipRepo.findActiveByPrimaryOrg(orgId, pageable);
		Page<PartnershipDto> partnershipDtos = partnerships
				.map(partnership -> toDto(partnership));
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
			com.nexus.core.model.enums.PartnershipStatus newStatus, String reason, String authToken) {
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

			return new ResponseEntity<>(toDto(savedPartnership), HttpStatus.OK);

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