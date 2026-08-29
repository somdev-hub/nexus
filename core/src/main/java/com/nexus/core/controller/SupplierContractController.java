package com.nexus.core.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierContractDto;
import com.nexus.core.service.SupplierContractService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/core/supplier-contracts")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SupplierContractController {

	private final SupplierContractService contractService;

	@PostMapping
	@LogActivity("CREATE_SUPPLIER_CONTRACT")
	public ResponseEntity<?> createContract(@Valid @RequestBody SupplierContractDto dto) {
		return contractService.createContract(dto);
	}

	@GetMapping("/{contractId}")
	@LogActivity("GET_SUPPLIER_CONTRACT")
	public ResponseEntity<?> getContractById(@PathVariable Long contractId) {
		return contractService.getContractById(contractId);
	}

	@GetMapping("/number/{contractNumber}")
	@LogActivity("GET_SUPPLIER_CONTRACT_BY_NUMBER")
	public ResponseEntity<?> getContractByNumber(@PathVariable String contractNumber) {
		return contractService.getContractByNumber(contractNumber);
	}

	@GetMapping
	@LogActivity("LIST_SUPPLIER_CONTRACTS")
	public ResponseEntity<?> getAllContracts(
			@RequestParam(required = false) SupplierContractDto.ContractStatus status,
			@RequestParam(required = false) Long supplierId,
			@RequestParam(required = false) SupplierContractDto.ContractType contractType,
			@RequestParam(required = false) LocalDate effectiveStartDate,
			@RequestParam(required = false) LocalDate effectiveEndDate,
			@RequestParam(required = false) LocalDate expiryStartDate,
			@RequestParam(required = false) LocalDate expiryEndDate,
			@RequestParam(required = false) Boolean expiringOnly,
			@RequestParam(required = false) LocalDate expiringBeforeDate,
			@RequestParam(required = false) Boolean autoRenewalOnly,
			@RequestParam(required = false) LocalDate autoRenewalBeforeDate,
			@PageableDefault(size = 20) Pageable pageable) {
		return contractService.getAllContracts(
				status, supplierId, contractType,
				effectiveStartDate, effectiveEndDate,
				expiryStartDate, expiryEndDate,
				expiringOnly, expiringBeforeDate,
				autoRenewalOnly, autoRenewalBeforeDate,
				pageable);
	}

	@GetMapping("/expiring")
	@LogActivity("GET_EXPIRING_CONTRACTS")
	public ResponseEntity<?> getExpiringContracts(
			@RequestParam LocalDate beforeDate) {
		return contractService.getExpiringContracts(beforeDate);
	}

	@GetMapping("/auto-renewal")
	@LogActivity("GET_AUTO_RENEWAL_CONTRACTS")
	public ResponseEntity<?> getAutoRenewalContracts(
			@RequestParam LocalDate beforeDate) {
		return contractService.getAutoRenewalContracts(beforeDate);
	}

	@GetMapping("/active-by-supplier/{supplierId}")
	@LogActivity("GET_ACTIVE_CONTRACT_BY_SUPPLIER")
	public ResponseEntity<?> getActiveContractBySupplierAndDate(
			@PathVariable Long supplierId,
			@RequestParam LocalDate date) {
		return contractService.getActiveContractBySupplierAndDate(supplierId, date);
	}

	@GetMapping("/summary")
	@LogActivity("GET_CONTRACT_SUMMARY")
	public ResponseEntity<?> getContractSummary() {
		return contractService.getContractSummary();
	}

	@PutMapping("/{contractId}")
	@LogActivity("UPDATE_SUPPLIER_CONTRACT")
	public ResponseEntity<?> updateContract(
			@PathVariable Long contractId,
			@Valid @RequestBody SupplierContractDto dto) {
		return contractService.updateContract(contractId, dto);
	}

	@PutMapping("/{contractId}/status")
	@LogActivity("UPDATE_SUPPLIER_CONTRACT_STATUS")
	public ResponseEntity<?> updateContractStatus(
			@PathVariable Long contractId,
			@RequestParam SupplierContractDto.ContractStatus status,
			@RequestParam(required = false) String reason) {
		return contractService.updateContractStatus(contractId, status, reason);
	}

	@PostMapping("/{contractId}/documents")
	@LogActivity("UPLOAD_CONTRACT_DOCUMENT")
	public ResponseEntity<?> uploadContractDocument(
			@PathVariable Long contractId,
			@RequestParam MultipartFile file,
			@RequestParam String documentName,
			@RequestParam(required = false) String remarks,
			@RequestHeader(value = "Authorization", required = false) String authToken) {
		return contractService.uploadContractDocument(contractId, file, documentName, remarks, authToken);
	}

	@GetMapping("/{contractId}/documents")
	@LogActivity("GET_CONTRACT_DOCUMENT")
	public ResponseEntity<?> getContractDocument(
			@PathVariable Long contractId,
			@RequestHeader(value = "Authorization", required = false) String authToken) {
		return contractService.getContractDocument(contractId, authToken);
	}

	@DeleteMapping("/{contractId}/documents")
	@LogActivity("DELETE_CONTRACT_DOCUMENT")
	public ResponseEntity<?> deleteContractDocument(
			@PathVariable Long contractId,
			@RequestHeader(value = "Authorization", required = false) String authToken) {
		return contractService.deleteContractDocument(contractId, authToken);
	}

	@PostMapping("/{contractId}/approve")
	@LogActivity("APPROVE_SUPPLIER_CONTRACT")
	public ResponseEntity<?> approveContract(
			@PathVariable Long contractId,
			@RequestParam String approvedBy) {
		return contractService.approveContract(contractId, approvedBy);
	}

	@PostMapping("/{contractId}/reject")
	@LogActivity("REJECT_SUPPLIER_CONTRACT")
	public ResponseEntity<?> rejectContract(
			@PathVariable Long contractId,
			@RequestParam String rejectionReason) {
		return contractService.rejectContract(contractId, rejectionReason);
	}

	@PostMapping("/{contractId}/terminate")
	@LogActivity("TERMINATE_SUPPLIER_CONTRACT")
	public ResponseEntity<?> terminateContract(
			@PathVariable Long contractId,
			@RequestParam String reason) {
		return contractService.terminateContract(contractId, reason);
	}

	@PostMapping("/{contractId}/suspend")
	@LogActivity("SUSPEND_SUPPLIER_CONTRACT")
	public ResponseEntity<?> suspendContract(
			@PathVariable Long contractId,
			@RequestParam String reason) {
		return contractService.suspendContract(contractId, reason);
	}

	@PostMapping("/{contractId}/renew")
	@LogActivity("RENEW_SUPPLIER_CONTRACT")
	public ResponseEntity<?> renewContract(
			@PathVariable Long contractId,
			@RequestParam LocalDate newExpiryDate) {
		return contractService.renewContract(contractId, newExpiryDate);
	}

	@DeleteMapping("/{contractId}")
	@LogActivity("DELETE_SUPPLIER_CONTRACT")
	public ResponseEntity<?> deleteContract(
			@PathVariable Long contractId) {
		return contractService.deleteContract(contractId);
	}
}