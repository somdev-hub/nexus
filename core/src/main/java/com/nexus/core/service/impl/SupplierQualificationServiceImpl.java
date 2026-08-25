package com.nexus.core.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.QualificationStatus;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierQualification;
import com.nexus.core.payload.SupplierQualificationDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.SupplierQualificationRepository;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.service.SupplierQualificationService;

import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SupplierQualificationServiceImpl implements SupplierQualificationService {

	private final SupplierQualificationRepository qualificationRepository;
	private final SupplierRepository supplierRepository;
	private final AccountRepository accountRepository;

	@Override
	public ResponseEntity<?> createQualification(SupplierQualificationDto qualificationDto) {
		Supplier supplier = supplierRepository.findById(qualificationDto.getSupplierId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Supplier not found with ID: " + qualificationDto.getSupplierId()));

		Account retailerOrg = accountRepository.findById(qualificationDto.getRetailerOrgId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Retailer organization not found with ID: " + qualificationDto.getRetailerOrgId()));

		// Check if qualification already exists
		SupplierQualification existing = qualificationRepository.findBySupplierAndRetailerOrgAndStatus(
				supplier, retailerOrg, QualificationStatus.PENDING);
		if (existing != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Qualification already pending for this supplier and retailer");
		}

		existing = qualificationRepository.findBySupplierAndRetailerOrgAndStatus(
				supplier, retailerOrg, QualificationStatus.IN_PROGRESS);
		if (existing != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Qualification already in progress for this supplier and retailer");
		}

		SupplierQualification qualification = new SupplierQualification();
		qualification.setSupplier(supplier);
		qualification.setRetailerOrg(retailerOrg);
		qualification.setStatus(QualificationStatus.PENDING);
		qualification.setComplianceDocuments(qualificationDto.getComplianceDocuments());
		qualification.setComplianceNotes(qualificationDto.getComplianceNotes());
		qualification.setAssessedBy(qualificationDto.getAssessedBy());
		qualification.setAssessedAt(Timestamp.from(Instant.now()));
		qualification.setValidUntil(Timestamp.from(Instant.now().plusSeconds(365 * 24 * 60 * 60))); // 1 year validity

		SupplierQualification saved = qualificationRepository.save(qualification);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@Override
	public ResponseEntity<?> getQualificationById(Long id) {
		return qualificationRepository.findById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getAllQualifications(Pageable pageable) {
		Page<SupplierQualification> qualifications = qualificationRepository.findAll(pageable);
		return ResponseEntity.ok(qualifications);
	}

	@Override
	public ResponseEntity<?> getQualificationsBySupplier(Long supplierId, Pageable pageable) {
		Supplier supplier = supplierRepository.findById(supplierId)
				.orElseThrow(() -> new IllegalArgumentException("Supplier not found with ID: " + supplierId));
		Page<SupplierQualification> qualifications = qualificationRepository.findBySupplier(supplier, pageable);
		return ResponseEntity.ok(qualifications);
	}

	@Override
	public ResponseEntity<?> getQualificationsByRetailerOrg(Long retailerOrgId, Pageable pageable) {
		Account retailerOrg = accountRepository.findById(retailerOrgId)
				.orElseThrow(() -> new IllegalArgumentException(
						"Retailer organization not found with ID: " + retailerOrgId));
		Page<SupplierQualification> qualifications = qualificationRepository.findByRetailerOrg(retailerOrg, pageable);
		return ResponseEntity.ok(qualifications);
	}

	@Override
	public ResponseEntity<?> updateQualificationStatus(Long id, QualificationStatus status, String rejectionReason) {
		SupplierQualification qualification = qualificationRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Qualification not found with ID: " + id));

		qualification.setStatus(status);
		qualification.setAssessedAt(Timestamp.from(Instant.now()));

		if (status == QualificationStatus.REJECTED) {
			qualification.setRejectionReason(rejectionReason);
		}

		if (status == QualificationStatus.APPROVED) {
			// Update supplier status to ACTIVE
			Supplier supplier = qualification.getSupplier();
			supplier.setStatus(com.nexus.core.entities.SupplierStatus.ACTIVE);
			supplierRepository.save(supplier);
		}

		SupplierQualification saved = qualificationRepository.save(qualification);
		return ResponseEntity.ok(saved);
	}
}