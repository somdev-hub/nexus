package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.SupplierQualificationDto;

public interface SupplierQualificationService {
	public ResponseEntity<?> createQualification(SupplierQualificationDto qualificationDto);

	public ResponseEntity<?> getQualificationById(Long id);

	public ResponseEntity<?> getAllQualifications(Pageable pageable);

	public ResponseEntity<?> getQualificationsBySupplier(Long supplierId, Pageable pageable);

	public ResponseEntity<?> getQualificationsByRetailerOrg(Long retailerOrgId, Pageable pageable);

	public ResponseEntity<?> updateQualificationStatus(Long id, com.nexus.core.entities.QualificationStatus status,
			String rejectionReason);

	public ResponseEntity<?> getQualificationsByStatus(com.nexus.core.entities.QualificationStatus status,
			Pageable pageable);
}