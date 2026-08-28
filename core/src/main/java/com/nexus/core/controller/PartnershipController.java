package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.PartnershipDto;
import com.nexus.core.payload.PartnershipStatusTransitionDto;
import com.nexus.core.security.OrganizationContextFilter;
import com.nexus.core.service.PartnershipService;
import com.nexus.core.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/partnerships")
@RequiredArgsConstructor
public class PartnershipController {

	private final PartnershipService partnershipService;
	private final CommonUtils commonUtils;

	@PostMapping("/add")
	@LogActivity("Create Partnership")
	public ResponseEntity<?> addPartnership(@Valid @RequestBody PartnershipDto partnershipDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context (set by OrganizationContextFilter)
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		// Set primary organization ID on the partnership DTO
		partnershipDto.setPrimaryOrg(orgId);

		return partnershipService.addPartnership(partnershipDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Partnership")
	public ResponseEntity<?> getPartnership(@PathVariable Long id, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.getPartnershipByIdAndOrg(id, orgId);
	}

	@GetMapping("/all")
	@LogActivity("Get All Partnerships")
	public ResponseEntity<?> getAllPartnerships(@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.getAllPartnershipsByOrgId(orgId, pageable);
	}

	@GetMapping("/status/{status}")
	@LogActivity("Get Partnerships By Status")
	public ResponseEntity<?> getPartnershipsByStatus(@PathVariable com.nexus.core.entities.PartnershipStatus status,
			@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.getPartnershipsByStatus(orgId, status, pageable);
	}

	@GetMapping("/active")
	@LogActivity("Get Active Partnerships")
	public ResponseEntity<?> getActivePartnerships(@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.getActivePartnershipsByOrgId(orgId, pageable);
	}

	@PostMapping("/{id}/status")
	@LogActivity("Update Partnership Status")
	public ResponseEntity<?> updatePartnershipStatus(@PathVariable Long id,
			@RequestBody com.nexus.core.entities.PartnershipStatus newStatus,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.updatePartnershipStatus(id, orgId, newStatus);
	}

	// ============================================
	// Partnership Agreement with DMS Integration
	// ============================================

	@PostMapping("/{id}/agreement")
	@LogActivity("Upload Partnership Agreement")
	public ResponseEntity<?> uploadPartnershipAgreement(@PathVariable Long id,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "documentName", required = false) String documentName,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.uploadPartnershipAgreement(id, orgId, file, documentName, remarks, token);
	}

	@GetMapping("/{id}/agreement")
	@LogActivity("Get Partnership Agreement")
	public ResponseEntity<?> getPartnershipAgreement(@PathVariable Long id,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.getPartnershipAgreement(id, orgId, token);
	}

	@DeleteMapping("/{id}/agreement")
	@LogActivity("Delete Partnership Agreement")
	public ResponseEntity<?> deletePartnershipAgreement(@PathVariable Long id,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.deletePartnershipAgreement(id, orgId, token);
	}

	// ============================================
	// Partnership Lifecycle Management
	// ============================================

	@PostMapping("/{id}/transition")
	@LogActivity("Transition Partnership Status")
	public ResponseEntity<?> transitionPartnershipStatus(@PathVariable Long id,
			@Valid @RequestBody PartnershipStatusTransitionDto transitionDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return partnershipService.transitionPartnershipStatus(id, orgId, transitionDto.getNewStatus(),
				transitionDto.getReason(), token);
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