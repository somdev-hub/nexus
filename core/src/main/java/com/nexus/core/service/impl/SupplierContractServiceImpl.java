package com.nexus.core.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.entities.Account;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierContract;
import com.nexus.core.payload.SupplierContractDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.SupplierContractRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierContractService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.RestService;
import com.nexus.core.utils.WebConstants;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierContractServiceImpl implements SupplierContractService {

	private final SupplierContractRepo contractRepo;
	private final SupplierRepository supplierRepository;
	private final AccountRepo accountRepo;
	private final ModelMapper modelMapper;
	private final WebConstants webConstants;
	private final CommonUtils commonUtils;
	private final RestService restService;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public ResponseEntity<?> createContract(SupplierContractDto dto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		log.info("Creating supplier contract for orgId: {}", orgId);

		Account account = accountRepo.findByAccountIdAndIsActiveTrue(orgId)
				.orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + orgId));

		Supplier supplier = supplierRepository.findBySupplierIdAndAccountAccountIdAndIsActiveTrue(dto.getSupplierId(), orgId)
				.orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + dto.getSupplierId()));

		if (contractRepo.findByAccountAccountIdAndContractNumber(orgId, dto.getContractNumber()).isPresent()) {
			return ResponseEntity.badRequest().body("Contract number already exists for this organization");
		}

		SupplierContract contract = modelMapper.map(dto, SupplierContract.class);
		contract.setAccount(account);
		contract.setSupplier(supplier);
		contract.setStatus(SupplierContract.ContractStatus.DRAFT);

		SupplierContract saved = contractRepo.save(contract);
		return ResponseEntity.ok(modelMapper.map(saved, SupplierContractDto.class));
	}

	@Override
	public ResponseEntity<?> getContractById(Long contractId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(c -> ResponseEntity.ok(modelMapper.map(c, SupplierContractDto.class)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getContractByNumber(String contractNumber) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findByAccountAccountIdAndContractNumber(orgId, contractNumber)
				.map(c -> ResponseEntity.ok(modelMapper.map(c, SupplierContractDto.class)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getAllContracts(
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
			Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<SupplierContract> contracts = contractRepo.findByAccountAccountIdWithFilters(
				orgId,
				status != null ? SupplierContract.ContractStatus.valueOf(status.name()) : null,
				supplierId,
				contractType != null ? SupplierContract.ContractType.valueOf(contractType.name()) : null,
				effectiveStartDate,
				effectiveEndDate,
				expiryStartDate,
				expiryEndDate,
				expiringOnly,
				expiringBeforeDate,
				autoRenewalOnly,
				autoRenewalBeforeDate,
				pageable);
		return ResponseEntity.ok(contracts.map(c -> modelMapper.map(c, SupplierContractDto.class)));
	}

	@Override
	public ResponseEntity<?> getExpiringContracts(LocalDate beforeDate) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<SupplierContract.ContractStatus> activeStatuses = List.of(
				SupplierContract.ContractStatus.ACTIVE,
				SupplierContract.ContractStatus.RENEWAL_PENDING);
		List<SupplierContract> contracts = contractRepo.findByAccountAccountIdAndExpiryDateBeforeAndStatusIn(orgId, beforeDate,
				activeStatuses);
		return ResponseEntity.ok(contracts.stream().map(c -> modelMapper.map(c, SupplierContractDto.class)).toList());
	}

	@Override
	public ResponseEntity<?> getAutoRenewalContracts(LocalDate beforeDate) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<SupplierContract> contracts = contractRepo.findByAccountAccountIdAndAutoRenewalTrueAndExpiryDateBefore(orgId,
				beforeDate);
		return ResponseEntity.ok(contracts.stream().map(c -> modelMapper.map(c, SupplierContractDto.class)).toList());
	}

	@Override
	public ResponseEntity<?> getActiveContractBySupplierAndDate(Long supplierId, LocalDate date) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Optional<SupplierContract> contract = contractRepo.findActiveContractBySupplierAndDate(orgId, supplierId, date);
		return contract.map(c -> ResponseEntity.ok(modelMapper.map(c, SupplierContractDto.class)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getContractSummary() {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Long activeCount = contractRepo.countActiveContractsByAccount(orgId);
		Long draftCount = contractRepo.countDraftContractsByAccount(orgId);
		Long pendingCount = contractRepo.countPendingApprovalContractsByAccount(orgId);
		Long expiredCount = contractRepo.countExpiredContractsByAccount(orgId);

		return ResponseEntity.ok(new ContractSummaryDto(activeCount, draftCount, pendingCount, expiredCount));
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateContract(Long contractId, SupplierContractDto dto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (!existing.getContractNumber().equals(dto.getContractNumber()) &&
							contractRepo.findByAccountAccountIdAndContractNumber(orgId, dto.getContractNumber()).isPresent()) {
						return ResponseEntity.badRequest().body("Contract number already exists for this organization");
					}

					modelMapper.map(dto, existing);
					existing.setAccount(accountRepo.findById(orgId).orElseThrow());
					existing.setSupplier(supplierRepository.findById(dto.getSupplierId()).orElseThrow());

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateContractStatus(Long contractId, SupplierContractDto.ContractStatus newStatus,
			String reason) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					SupplierContract.ContractStatus currentStatus = existing.getStatus();
					SupplierContract.ContractStatus targetStatus = SupplierContract.ContractStatus
							.valueOf(newStatus.name());

					if (!isValidStatusTransition(currentStatus, targetStatus)) {
						return ResponseEntity.badRequest()
								.body("Invalid status transition from " + currentStatus + " to " + targetStatus);
					}

					existing.setStatus(targetStatus);
					if (reason != null) {
						existing.setRejectionReason(reason);
					}

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> uploadContractDocument(Long contractId, MultipartFile file, String documentName,
			String remarks, String authToken) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(contract -> {
					try {
						// Use RestService to upload to DMS
						ResponseEntity<String> response = restService.uploadToDmsOrg(
								file,
								documentName != null ? documentName : file.getOriginalFilename(),
								orgId,
								remarks != null ? remarks : "Supplier contract document for contract ID: " + contractId,
								"CONTRACT",
								"RETAILER",
								authToken,
								orgId);

						if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
							// Parse DMS response to get document ID
							JsonNode responseNode = objectMapper.readTree(response.getBody());
							String dmsId = responseNode.path("dmsId").asText();
							String documentUrl = responseNode.path("documentUrl").asText();

							contract.setDmsDocumentId(dmsId);
							contract.setDmsDocumentName(documentName);
							contract.setDmsDocumentUrl(documentUrl);
							contract.setDmsDocumentVersion(contract.getDmsDocumentVersion() + 1);
							contractRepo.save(contract);

							return ResponseEntity.ok("Document uploaded successfully. DMS ID: " + dmsId);
						} else {
							return ResponseEntity.status(response.getStatusCode())
									.body("Failed to upload document to DMS: " + response.getBody());
						}
					} catch (Exception e) {
						log.error("Error uploading contract document", e);
						return ResponseEntity.internalServerError().body("Error uploading document: " + e.getMessage());
					}
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getContractDocument(Long contractId, String authToken) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(contract -> {
					if (contract.getDmsDocumentId() == null) {
						return ResponseEntity.notFound().build();
					}

					try {
						// Use RestService to get document from DMS
						ResponseEntity<String> response = restService.getFromDms(
								contract.getDmsDocumentId(),
								authToken,
								orgId);

						return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
					} catch (Exception e) {
						log.error("Error retrieving contract document", e);
						return ResponseEntity.internalServerError()
								.body("Error retrieving document: " + e.getMessage());
					}
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> deleteContractDocument(Long contractId, String authToken) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(contract -> {
					if (contract.getDmsDocumentId() == null) {
						return ResponseEntity.notFound().build();
					}

					try {
						// Use RestService to delete document from DMS
						ResponseEntity<String> response = restService.deleteFromDms(
								contract.getDmsDocumentId(),
								authToken,
								orgId);

						if (response.getStatusCode().is2xxSuccessful()) {
							contract.setDmsDocumentId(null);
							contract.setDmsDocumentName(null);
							contractRepo.save(contract);
							return ResponseEntity.ok("Document deleted successfully");
						} else {
							return ResponseEntity.status(response.getStatusCode())
									.body("Failed to delete document from DMS: " + response.getBody());
						}
					} catch (Exception e) {
						log.error("Error deleting contract document", e);
						return ResponseEntity.internalServerError().body("Error deleting document: " + e.getMessage());
					}
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> approveContract(Long contractId, String approvedBy) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (existing.getStatus() != SupplierContract.ContractStatus.DRAFT &&
							existing.getStatus() != SupplierContract.ContractStatus.PENDING_APPROVAL) {
						return ResponseEntity.badRequest()
								.body("Only DRAFT or PENDING_APPROVAL contracts can be approved");
					}

					existing.setStatus(SupplierContract.ContractStatus.ACTIVE);
					existing.setApprovedBy(approvedBy);
					existing.setApprovedAt(LocalDateTime.now());

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> rejectContract(Long contractId, String rejectionReason) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (existing.getStatus() != SupplierContract.ContractStatus.DRAFT &&
							existing.getStatus() != SupplierContract.ContractStatus.PENDING_APPROVAL) {
						return ResponseEntity.badRequest()
								.body("Only DRAFT or PENDING_APPROVAL contracts can be rejected");
					}

					existing.setStatus(SupplierContract.ContractStatus.TERMINATED);
					existing.setRejectionReason(rejectionReason);

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> terminateContract(Long contractId, String reason) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (existing.getStatus() == SupplierContract.ContractStatus.TERMINATED ||
							existing.getStatus() == SupplierContract.ContractStatus.EXPIRED) {
						return ResponseEntity.badRequest().body("Contract is already terminated or expired");
					}

					existing.setStatus(SupplierContract.ContractStatus.TERMINATED);
					existing.setRejectionReason(reason);

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> suspendContract(Long contractId, String reason) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (existing.getStatus() != SupplierContract.ContractStatus.ACTIVE) {
						return ResponseEntity.badRequest().body("Only ACTIVE contracts can be suspended");
					}

					existing.setStatus(SupplierContract.ContractStatus.SUSPENDED);
					existing.setRejectionReason(reason);

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> renewContract(Long contractId, LocalDate newExpiryDate) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (existing.getStatus() != SupplierContract.ContractStatus.ACTIVE &&
							existing.getStatus() != SupplierContract.ContractStatus.RENEWAL_PENDING &&
							existing.getStatus() != SupplierContract.ContractStatus.EXPIRED) {
						return ResponseEntity.badRequest()
								.body("Contract must be ACTIVE, RENEWAL_PENDING, or EXPIRED to renew");
					}

					existing.setStatus(SupplierContract.ContractStatus.ACTIVE);
					existing.setExpiryDate(newExpiryDate);
					existing.setDmsDocumentVersion(existing.getDmsDocumentVersion() + 1);

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(modelMapper.map(updated, SupplierContractDto.class));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> deleteContract(Long contractId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(existing -> {
					if (existing.getStatus() == SupplierContract.ContractStatus.ACTIVE) {
						return ResponseEntity.badRequest().body("Cannot delete ACTIVE contract. Terminate first.");
					}

					contractRepo.delete(existing);
					return ResponseEntity.ok("Contract deleted successfully");
				})
				.orElse(ResponseEntity.notFound().build());
	}

	private boolean isValidStatusTransition(SupplierContract.ContractStatus from, SupplierContract.ContractStatus to) {
		return switch (from) {
			case DRAFT -> to == SupplierContract.ContractStatus.PENDING_APPROVAL
					|| to == SupplierContract.ContractStatus.TERMINATED;
			case PENDING_APPROVAL -> to == SupplierContract.ContractStatus.ACTIVE
					|| to == SupplierContract.ContractStatus.TERMINATED || to == SupplierContract.ContractStatus.DRAFT;
			case ACTIVE ->
				to == SupplierContract.ContractStatus.SUSPENDED || to == SupplierContract.ContractStatus.TERMINATED
						|| to == SupplierContract.ContractStatus.RENEWAL_PENDING
						|| to == SupplierContract.ContractStatus.EXPIRED;
			case SUSPENDED ->
				to == SupplierContract.ContractStatus.ACTIVE || to == SupplierContract.ContractStatus.TERMINATED;
			case RENEWAL_PENDING ->
				to == SupplierContract.ContractStatus.ACTIVE || to == SupplierContract.ContractStatus.TERMINATED
						|| to == SupplierContract.ContractStatus.EXPIRED;
			case EXPIRED ->
				to == SupplierContract.ContractStatus.ACTIVE || to == SupplierContract.ContractStatus.TERMINATED;
			case TERMINATED -> false;
		};
	}

	private String extractDmsIdFromResponse(String responseBody) {
		// Simple extraction - in production use proper JSON parsing
		try {
			int start = responseBody.indexOf("\"dmsId\":\"") + 9;
			int end = responseBody.indexOf("\"", start);
			if (start > 8 && end > start) {
				return responseBody.substring(start, end);
			}
		} catch (Exception e) {
			log.warn("Could not parse DMS ID from response: {}", responseBody);
		}
		return "unknown";
	}

	private record ContractSummaryDto(Long activeCount, Long draftCount, Long pendingApprovalCount, Long expiredCount) {
	}
}