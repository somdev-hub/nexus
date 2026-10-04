package com.nexus.core.service.implementations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.Supplier;
import com.nexus.core.model.entities.SupplierContract;
import com.nexus.core.payload.SupplierContractDto;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.SupplierContractRepo;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierContractService;
import com.nexus.core.utils.RestService;

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
		return ResponseEntity.ok(toDto(saved));
	}

	@Override
	public ResponseEntity<?> getContractById(Long contractId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(c -> ResponseEntity.ok(toDto(c)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getContractByNumber(String contractNumber) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findByAccountAccountIdAndContractNumber(orgId, contractNumber)
				.map(c -> ResponseEntity.ok(toDto(c)))
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
		return ResponseEntity.ok(contracts.map(c -> toDto(c)));
	}

	@Override
	public ResponseEntity<?> getExpiringContracts(LocalDate beforeDate) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<SupplierContract.ContractStatus> activeStatuses = List.of(
				SupplierContract.ContractStatus.ACTIVE,
				SupplierContract.ContractStatus.RENEWAL_PENDING);
		List<SupplierContract> contracts = contractRepo.findByAccountAccountIdAndExpiryDateBeforeAndStatusIn(orgId, beforeDate,
				activeStatuses);
		return ResponseEntity.ok(contracts.stream().map(c -> toDto(c)).toList());
	}

	@Override
	public ResponseEntity<?> getAutoRenewalContracts(LocalDate beforeDate) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<SupplierContract> contracts = contractRepo.findByAccountAccountIdAndAutoRenewalTrueAndExpiryDateBefore(orgId,
				beforeDate);
		return ResponseEntity.ok(contracts.stream().map(c -> toDto(c)).toList());
	}

	@Override
	public ResponseEntity<?> getActiveContractBySupplierAndDate(Long supplierId, LocalDate date) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Optional<SupplierContract> contract = contractRepo.findActiveContractBySupplierAndDate(orgId, supplierId, date);
		return contract.map(c -> ResponseEntity.ok(toDto(c)))
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
					if (dto.getContractNumber() != null
							&& !dto.getContractNumber().equals(existing.getContractNumber()) &&
							contractRepo.findByAccountAccountIdAndContractNumber(orgId, dto.getContractNumber()).isPresent()) {
						return ResponseEntity.badRequest().body("Contract number already exists for this organization");
					}

					if (dto.getContractNumber() != null) {
						existing.setContractNumber(dto.getContractNumber());
					}
					if (dto.getContractName() != null) {
						existing.setContractName(dto.getContractName());
					}
					if (dto.getDescription() != null) {
						existing.setDescription(dto.getDescription());
					}
					if (dto.getContractType() != null) {
						existing.setContractType(
								SupplierContract.ContractType.valueOf(dto.getContractType().name()));
					}
					if (dto.getStatus() != null) {
						existing.setStatus(SupplierContract.ContractStatus.valueOf(dto.getStatus().name()));
					}
					if (dto.getEffectiveDate() != null) {
						existing.setEffectiveDate(dto.getEffectiveDate());
					}
					if (dto.getExpiryDate() != null) {
						existing.setExpiryDate(dto.getExpiryDate());
					}
					if (dto.getAutoRenewal() != null) {
						existing.setAutoRenewal(dto.getAutoRenewal());
					}
					if (dto.getRenewalNoticeDays() != null) {
						existing.setRenewalNoticeDays(dto.getRenewalNoticeDays());
					}
					if (dto.getBaseCurrency() != null) {
						existing.setBaseCurrency(dto.getBaseCurrency());
					}
					if (dto.getContractAmount() != null) {
						existing.setContractAmount(dto.getContractAmount());
					}
					if (dto.getPaymentTermsDays() != null) {
						existing.setPaymentTermsDays(dto.getPaymentTermsDays());
					}
					if (dto.getIncoterms() != null) {
						existing.setIncoterms(dto.getIncoterms());
					}
					if (dto.getSlaLeadTimeDays() != null) {
						existing.setSlaLeadTimeDays(dto.getSlaLeadTimeDays());
					}
					if (dto.getSlaOnTimeDeliveryPct() != null) {
						existing.setSlaOnTimeDeliveryPct(dto.getSlaOnTimeDeliveryPct());
					}
					if (dto.getSlaQualityDefectRatePct() != null) {
						existing.setSlaQualityDefectRatePct(dto.getSlaQualityDefectRatePct());
					}
					if (dto.getSlaResponseTimeHours() != null) {
						existing.setSlaResponseTimeHours(dto.getSlaResponseTimeHours());
					}
					if (dto.getVolumeDiscountTier1Qty() != null) {
						existing.setVolumeDiscountTier1Qty(dto.getVolumeDiscountTier1Qty());
					}
					if (dto.getVolumeDiscountTier1Pct() != null) {
						existing.setVolumeDiscountTier1Pct(dto.getVolumeDiscountTier1Pct());
					}
					if (dto.getVolumeDiscountTier2Qty() != null) {
						existing.setVolumeDiscountTier2Qty(dto.getVolumeDiscountTier2Qty());
					}
					if (dto.getVolumeDiscountTier2Pct() != null) {
						existing.setVolumeDiscountTier2Pct(dto.getVolumeDiscountTier2Pct());
					}
					if (dto.getVolumeDiscountTier3Qty() != null) {
						existing.setVolumeDiscountTier3Qty(dto.getVolumeDiscountTier3Qty());
					}
					if (dto.getVolumeDiscountTier3Pct() != null) {
						existing.setVolumeDiscountTier3Pct(dto.getVolumeDiscountTier3Pct());
					}
					if (dto.getDmsDocumentId() != null) {
						existing.setDmsDocumentId(dto.getDmsDocumentId());
					}
					if (dto.getDmsDocumentName() != null) {
						existing.setDmsDocumentName(dto.getDmsDocumentName());
					}
					if (dto.getDmsDocumentVersion() != null) {
						existing.setDmsDocumentVersion(dto.getDmsDocumentVersion());
					}
					if (dto.getApprovedBy() != null) {
						existing.setApprovedBy(dto.getApprovedBy());
					}
					if (dto.getApprovedAt() != null) {
						existing.setApprovedAt(dto.getApprovedAt());
					}
					if (dto.getRejectionReason() != null) {
						existing.setRejectionReason(dto.getRejectionReason());
					}

					existing.setAccount(accountRepo.findById(orgId).orElseThrow());
					if (dto.getSupplierId() != null) {
						existing.setSupplier(supplierRepository.findById(dto.getSupplierId()).orElseThrow());
					}

					SupplierContract updated = contractRepo.save(existing);
					return ResponseEntity.ok(toDto(updated));
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
					return ResponseEntity.ok(toDto(updated));
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
	@Transactional
	public ResponseEntity<?> uploadContractTextDocument(Long contractId, String fileName, String content,
			String authToken) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(contract -> {
					try {
						if (content == null || content.isBlank()) {
							return ResponseEntity.badRequest().body("Document content is required");
						}
						String safeName = fileName != null && !fileName.isBlank()
								? fileName
								: "contract-" + contract.getContractNumber() + ".md";
						// CMS pattern: encrypt server-side with NexusEncryption,
						// store the encrypted bytes in DMS as a text file.
						String encrypted = com.nexus.nexusencryption.NexusEncryption.encrypt(content);
						byte[] bytes = encrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8);
						org.springframework.web.multipart.MultipartFile file = new com.nexus.core.utils.InMemoryMultipartFile(
								"file", safeName, "text/markdown", bytes);
						ResponseEntity<String> response = restService.uploadToDmsOrg(
								file,
								safeName,
								orgId,
								"Supplier contract document for contract: " + contract.getContractNumber(),
								"CONTRACT",
								"RETAILER",
								authToken,
								orgId);

						if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
							JsonNode responseNode = objectMapper.readTree(response.getBody());
							String dmsId = responseNode.path("dmsId").asText();
							String documentUrl = responseNode.path("documentUrl").asText();

							contract.setDmsDocumentId(dmsId);
							contract.setDmsDocumentName(safeName);
							contract.setDmsDocumentUrl(documentUrl);
							contract.setDmsDocumentVersion(contract.getDmsDocumentVersion() + 1);
							contractRepo.save(contract);

							return ResponseEntity.ok(java.util.Map.of(
									"message", "Contract document uploaded successfully",
									"dmsId", dmsId,
									"documentUrl", documentUrl));
						} else {
							return ResponseEntity.status(response.getStatusCode())
									.body("Failed to upload document to DMS: " + response.getBody());
						}
					} catch (Exception e) {
						log.error("Error uploading contract text document", e);
						return ResponseEntity.internalServerError()
								.body("Error uploading document: " + e.getMessage());
					}
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getContractDocumentContent(Long contractId, String authToken) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return contractRepo.findById(contractId)
				.filter(c -> c.getAccount().getAccountId().equals(orgId))
				.map(contract -> {
					// The DMS file-download API does not exist — fetch the
					// stored documentUrl returned at upload time instead
					// (same pattern as CMS template fetching).
					if (contract.getDmsDocumentUrl() == null
							|| contract.getDmsDocumentUrl().isBlank()) {
						return ResponseEntity.notFound().build();
					}

					try {
						ResponseEntity<String> response = restService
								.fetchTextFromUrl(contract.getDmsDocumentUrl());
						if (!response.getStatusCode().is2xxSuccessful()
								|| response.getBody() == null) {
							return ResponseEntity.status(response.getStatusCode())
									.body("Failed to fetch document from DMS");
						}
						String decrypted = com.nexus.nexusencryption.NexusEncryption
								.decrypt(extractDmsText(response.getBody()));
						return ResponseEntity.ok(java.util.Map.of(
								"fileName", contract.getDmsDocumentName() != null
										? contract.getDmsDocumentName()
										: "contract.md",
								"content", decrypted));
					} catch (Exception e) {
						log.error("Error retrieving contract document content", e);
						return ResponseEntity.internalServerError()
								.body("Error retrieving document: " + e.getMessage());
					}
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * DMS may return raw file bytes or a JSON envelope — unwrap common
	 * content fields before decrypting.
	 */
	private String extractDmsText(String body) {
		if (body == null) {
			return "";
		}
		String trimmed = body.trim();
		if (!trimmed.startsWith("{")) {
			return body;
		}
		try {
			JsonNode node = objectMapper.readTree(body);
			for (String field : new String[] { "content", "data", "fileContent", "text" }) {
				JsonNode child = node.path(field);
				if (child.isTextual() && !child.asText().isBlank()) {
					return child.asText();
				}
			}
		} catch (Exception ignored) {
		}
		return body;
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
					return ResponseEntity.ok(toDto(updated));
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
					return ResponseEntity.ok(toDto(updated));
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
					return ResponseEntity.ok(toDto(updated));
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
					return ResponseEntity.ok(toDto(updated));
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
					return ResponseEntity.ok(toDto(updated));
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

	// ============================================
	// Supplier-side (countersign) flow
	// ============================================

	private java.util.Optional<SupplierContract> findSupplierContract(Long contractId, Long orgId) {
		return contractRepo.findByContractIdAndSupplierSupplierOrgAccountId(contractId, orgId);
	}

	@Override
	public ResponseEntity<?> getSupplierContracts(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<SupplierContract> contracts = contractRepo.findBySupplierSupplierOrgAccountId(orgId, pageable);
		return ResponseEntity.ok(contracts.map(SupplierContractServiceImpl::toDto));
	}

	@Override
	public ResponseEntity<?> getSupplierContractById(Long contractId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return findSupplierContract(contractId, orgId)
				.map(c -> ResponseEntity.ok(toDto(c)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> approveSupplierContract(Long contractId, String decidedBy) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return findSupplierContract(contractId, orgId)
				.map(contract -> {
					if (contract.getStatus() != SupplierContract.ContractStatus.ACTIVE
							&& contract.getStatus() != SupplierContract.ContractStatus.PENDING_APPROVAL) {
						return ResponseEntity.badRequest()
								.body("Only ACTIVE or PENDING_APPROVAL contracts can be approved by the supplier");
					}
					contract.setSupplierStatus(SupplierContract.SupplierStatus.APPROVED);
					contract.setSupplierDecidedBy(decidedBy);
					contract.setSupplierDecidedAt(java.time.LocalDateTime.now());
					return ResponseEntity.ok(toDto(contractRepo.save(contract)));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> rejectSupplierContract(Long contractId, String comments, String decidedBy) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return findSupplierContract(contractId, orgId)
				.map(contract -> {
					if (comments == null || comments.isBlank()) {
						return ResponseEntity.badRequest().body("Comments are required to reject a contract");
					}
					contract.setSupplierStatus(SupplierContract.SupplierStatus.REJECTED);
					contract.setSupplierComments(comments);
					contract.setSupplierDecidedBy(decidedBy);
					contract.setSupplierDecidedAt(java.time.LocalDateTime.now());
					return ResponseEntity.ok(toDto(contractRepo.save(contract)));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	@Transactional
	public ResponseEntity<?> requestSupplierContractAmendments(Long contractId, String comments, String decidedBy) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return findSupplierContract(contractId, orgId)
				.map(contract -> {
					if (comments == null || comments.isBlank()) {
						return ResponseEntity.badRequest()
								.body("Comments are required to request amendments");
					}
					contract.setSupplierStatus(SupplierContract.SupplierStatus.AMENDMENTS_REQUESTED);
					contract.setSupplierComments(comments);
					contract.setSupplierDecidedBy(decidedBy);
					contract.setSupplierDecidedAt(java.time.LocalDateTime.now());
					return ResponseEntity.ok(toDto(contractRepo.save(contract)));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<?> getSupplierContractDocumentContent(Long contractId, String authToken) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		return findSupplierContract(contractId, orgId)
				.map(contract -> {
					if (contract.getDmsDocumentUrl() == null
							|| contract.getDmsDocumentUrl().isBlank()) {
						return ResponseEntity.notFound().build();
					}
					try {
						ResponseEntity<String> response = restService
								.fetchTextFromUrl(contract.getDmsDocumentUrl());
						if (!response.getStatusCode().is2xxSuccessful()
								|| response.getBody() == null) {
							return ResponseEntity.status(response.getStatusCode())
									.body("Failed to fetch document from DMS");
						}
						String decrypted = com.nexus.nexusencryption.NexusEncryption
								.decrypt(extractDmsText(response.getBody()));
						return ResponseEntity.ok(java.util.Map.of(
								"fileName", contract.getDmsDocumentName() != null
										? contract.getDmsDocumentName()
										: "contract.md",
								"content", decrypted));
					} catch (Exception e) {
						log.error("Error retrieving supplier contract document content", e);
						return ResponseEntity.internalServerError()
								.body("Error retrieving document: " + e.getMessage());
					}
				})
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * ModelMapper cannot map SupplierContract → SupplierContractDto: nested *Id
	 * properties (account.accountId/org, supplier.supplierId/…) are ambiguous
	 * for the scalar accountId/supplierId destinations and throw
	 * ConfigurationException at runtime. Map explicitly — never entity graphs.
	 */
	private static SupplierContractDto toDto(SupplierContract c) {
		SupplierContractDto dto = new SupplierContractDto();
		dto.setContractId(c.getContractId());
		dto.setAccountId(c.getAccount() != null ? c.getAccount().getAccountId() : null);
		dto.setRetailerOrgName(c.getAccount() != null ? c.getAccount().getName() : null);
		dto.setSupplierId(c.getSupplier() != null ? c.getSupplier().getSupplierId() : null);
		dto.setSupplierName(c.getSupplier() != null ? c.getSupplier().getBusinessName() : null);
		dto.setContractNumber(c.getContractNumber());
		dto.setContractName(c.getContractName());
		dto.setDescription(c.getDescription());
		if (c.getContractType() != null) {
			dto.setContractType(SupplierContractDto.ContractType.valueOf(c.getContractType().name()));
		}
		if (c.getStatus() != null) {
			dto.setStatus(SupplierContractDto.ContractStatus.valueOf(c.getStatus().name()));
		}
		dto.setEffectiveDate(c.getEffectiveDate());
		dto.setExpiryDate(c.getExpiryDate());
		dto.setAutoRenewal(c.getAutoRenewal());
		dto.setRenewalNoticeDays(c.getRenewalNoticeDays());
		dto.setBaseCurrency(c.getBaseCurrency());
		dto.setContractAmount(c.getContractAmount());
		dto.setPaymentTermsDays(c.getPaymentTermsDays());
		dto.setIncoterms(c.getIncoterms());
		dto.setSlaLeadTimeDays(c.getSlaLeadTimeDays());
		dto.setSlaOnTimeDeliveryPct(c.getSlaOnTimeDeliveryPct());
		dto.setSlaQualityDefectRatePct(c.getSlaQualityDefectRatePct());
		dto.setSlaResponseTimeHours(c.getSlaResponseTimeHours());
		dto.setVolumeDiscountTier1Qty(c.getVolumeDiscountTier1Qty());
		dto.setVolumeDiscountTier1Pct(c.getVolumeDiscountTier1Pct());
		dto.setVolumeDiscountTier2Qty(c.getVolumeDiscountTier2Qty());
		dto.setVolumeDiscountTier2Pct(c.getVolumeDiscountTier2Pct());
		dto.setVolumeDiscountTier3Qty(c.getVolumeDiscountTier3Qty());
		dto.setVolumeDiscountTier3Pct(c.getVolumeDiscountTier3Pct());
		dto.setDmsDocumentId(c.getDmsDocumentId());
		dto.setDmsDocumentName(c.getDmsDocumentName());
		dto.setDmsDocumentVersion(c.getDmsDocumentVersion());
		dto.setApprovedBy(c.getApprovedBy());
		dto.setApprovedAt(c.getApprovedAt());
		dto.setRejectionReason(c.getRejectionReason());
		dto.setSupplierStatus(c.getSupplierStatus());
		dto.setSupplierComments(c.getSupplierComments());
		dto.setSupplierDecidedAt(c.getSupplierDecidedAt());
		dto.setSupplierDecidedBy(c.getSupplierDecidedBy());
		if (c.getCreatedAt() != null) {
			dto.setCreatedAt(c.getCreatedAt().toLocalDateTime());
		}
		if (c.getUpdatedAt() != null) {
			dto.setUpdatedAt(c.getUpdatedAt().toLocalDateTime());
		}
		return dto;
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