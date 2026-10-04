package com.nexus.core.service.interfaces;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.core.payload.SupplierContractDto;

public interface SupplierContractService {

	ResponseEntity<?> createContract(SupplierContractDto dto);

	ResponseEntity<?> getContractById(Long contractId);

	ResponseEntity<?> getContractByNumber(String contractNumber);

	ResponseEntity<?> getAllContracts(
			SupplierContractDto.ContractStatus status,
			Long supplierId,
			SupplierContractDto.ContractType contractType,
			LocalDate effectiveStartDate,
			LocalDate effectiveEndDate,
			LocalDate expiryStartDate,
			LocalDate expiryEndDate,
			Boolean expiringOnly,
			LocalDate expiringBeforeDate,
			Boolean autoRenewalOnly,
			LocalDate autoRenewalBeforeDate,
			Pageable pageable);

	ResponseEntity<?> getExpiringContracts(LocalDate beforeDate);

	ResponseEntity<?> getAutoRenewalContracts(LocalDate beforeDate);

	ResponseEntity<?> getActiveContractBySupplierAndDate(Long supplierId, LocalDate date);

	ResponseEntity<?> getContractSummary();

	ResponseEntity<?> updateContract(Long contractId, SupplierContractDto dto);

	ResponseEntity<?> updateContractStatus(Long contractId, SupplierContractDto.ContractStatus newStatus,
			String reason);

	ResponseEntity<?> uploadContractDocument(Long contractId, MultipartFile file, String documentName, String remarks,
			String authToken);

	ResponseEntity<?> uploadContractTextDocument(Long contractId, String fileName, String content, String authToken);

	ResponseEntity<?> getContractDocumentContent(Long contractId, String authToken);

	ResponseEntity<?> getContractDocument(Long contractId, String authToken);

	ResponseEntity<?> deleteContractDocument(Long contractId, String authToken);

	ResponseEntity<?> approveContract(Long contractId, String approvedBy);

	ResponseEntity<?> rejectContract(Long contractId, String rejectionReason);

	ResponseEntity<?> terminateContract(Long contractId, String reason);

	ResponseEntity<?> suspendContract(Long contractId, String reason);

	ResponseEntity<?> renewContract(Long contractId, LocalDate newExpiryDate);

	ResponseEntity<?> deleteContract(Long contractId);

	// Supplier-side (countersign) flow: contracts shared with the supplier org
	ResponseEntity<?> getSupplierContracts(org.springframework.data.domain.Pageable pageable);

	ResponseEntity<?> getSupplierContractById(Long contractId);

	ResponseEntity<?> approveSupplierContract(Long contractId, String decidedBy);

	ResponseEntity<?> rejectSupplierContract(Long contractId, String comments, String decidedBy);

	ResponseEntity<?> requestSupplierContractAmendments(Long contractId, String comments, String decidedBy);

	ResponseEntity<?> getSupplierContractDocumentContent(Long contractId, String authToken);
}