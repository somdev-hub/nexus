package com.nexus.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import com.nexus.core.entities.QualificationStatus;
import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.SupplierQualificationDto;
import com.nexus.core.service.SupplierQualificationService;
import com.nexus.core.utils.CommonUtils;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/suppliers/qualification")
@RequiredArgsConstructor
public class SupplierQualificationController {

	private final SupplierQualificationService qualificationService;
	private final CommonUtils commonUtils;

	@PostMapping("/create")
	@LogActivity("Create Supplier Qualification")
	public ResponseEntity<?> createQualification(@Valid @RequestBody SupplierQualificationDto qualificationDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return qualificationService.createQualification(qualificationDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Supplier Qualification")
	public ResponseEntity<?> getQualification(@PathVariable Long id, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return qualificationService.getQualificationById(id);
	}

	@GetMapping("/all")
	@LogActivity("Get All Supplier Qualifications")
	public ResponseEntity<?> getAllQualifications(@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return qualificationService.getAllQualifications(pageable);
	}

	@GetMapping("/supplier/{supplierId}")
	@LogActivity("Get Supplier Qualifications By Supplier")
	public ResponseEntity<?> getQualificationsBySupplier(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return qualificationService.getQualificationsBySupplier(supplierId, pageable);
	}

	@GetMapping("/retailer/{retailerOrgId}")
	@LogActivity("Get Supplier Qualifications By Retailer")
	public ResponseEntity<?> getQualificationsByRetailerOrg(@PathVariable Long retailerOrgId,
			@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return qualificationService.getQualificationsByRetailerOrg(retailerOrgId, pageable);
	}

	@PutMapping("/{id}/status")
	@LogActivity("Update Supplier Qualification Status")
	public ResponseEntity<?> updateQualificationStatus(@PathVariable Long id,
			@Valid @RequestBody QualificationStatusUpdateDto statusDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return qualificationService.updateQualificationStatus(id, statusDto.getStatus(),
				statusDto.getRejectionReason());
	}

	@Data
	public static class QualificationStatusUpdateDto {
		@NotNull(message = "Status is required")
		private QualificationStatus status;
		private String rejectionReason;
	}
}