package com.nexus.core.service.impl;

import com.nexus.core.dto.SupplierRiskMonitoringDTO;
import com.nexus.core.dto.SupplierRiskMonitoringCreateRequest;
import com.nexus.core.dto.SupplierRiskMonitoringUpdateRequest;
import com.nexus.core.dto.SupplierRiskSummaryDTO;
import com.nexus.core.entities.Supplier;
import com.nexus.core.entities.SupplierRiskMonitoring;
import com.nexus.core.entities.Partnership;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.repository.SupplierRepository;
import com.nexus.core.repository.SupplierRiskMonitoringRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierRiskMonitoringService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of Supplier Risk Monitoring Service.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Service
@RequiredArgsConstructor
public class SupplierRiskMonitoringServiceImpl implements SupplierRiskMonitoringService {

	private final SupplierRiskMonitoringRepo riskMonitoringRepo;
	private final SupplierRepository supplierRepo;
	private final PartnershipRepo partnershipRepo;
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public SupplierRiskMonitoringDTO createRiskMonitoring(SupplierRiskMonitoringCreateRequest request, Long orgId) {
		// Validate supplier exists and belongs to org
		Supplier supplier = supplierRepo.findBySupplierIdAndAccountAccountId(request.getSupplierId(), orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", request.getSupplierId()));

		// Validate partnership if provided
		Partnership partnership = null;
		if (request.getPartnershipId() != null) {
			partnership = partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(request.getPartnershipId(), orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId",
							request.getPartnershipId()));
		}

		// Validate risk category
		validateRiskCategory(request.getRiskCategory());

		// Validate risk level
		validateRiskLevel(request.getRiskLevel());

		// Validate status
		validateStatus(request.getStatus());

		SupplierRiskMonitoring riskMonitoring = new SupplierRiskMonitoring();
		riskMonitoring.setSupplier(supplier);
		riskMonitoring.setPartnership(partnership);
		riskMonitoring.setRiskCategory(request.getRiskCategory().toUpperCase());
		riskMonitoring.setRiskLevel(request.getRiskLevel().toUpperCase());
		riskMonitoring.setRiskScore(request.getRiskScore());
		riskMonitoring.setRiskDescription(request.getRiskDescription());
		riskMonitoring.setMitigationPlan(request.getMitigationPlan());
		riskMonitoring.setIdentifiedDate(request.getIdentifiedDate());
		riskMonitoring.setLastAssessedDate(request.getLastAssessedDate());
		riskMonitoring.setNextReviewDate(request.getNextReviewDate());
		riskMonitoring.setIsActive(request.getIsActive());
		riskMonitoring.setSource(request.getSource());
		riskMonitoring.setReferenceDocumentId(request.getReferenceDocumentId());
		riskMonitoring.setAssessedBy(request.getAssessedBy());
		riskMonitoring.setReviewedBy(request.getReviewedBy());
		riskMonitoring.setStatus(request.getStatus().toUpperCase());
		riskMonitoring.setEscalationLevel(request.getEscalationLevel());
		riskMonitoring.setNotes(request.getNotes());

		SupplierRiskMonitoring saved = riskMonitoringRepo.save(riskMonitoring);
		return mapToDTO(saved);
	}

	@Override
	public SupplierRiskMonitoringDTO getRiskMonitoringById(Long riskMonitoringId, Long orgId) {
		SupplierRiskMonitoring riskMonitoring = riskMonitoringRepo
				.findByIdAndSupplierSupplierId(riskMonitoringId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("SupplierRiskMonitoring", "riskMonitoringId",
						riskMonitoringId));
		return mapToDTO(riskMonitoring);
	}

	@Override
	public List<SupplierRiskMonitoringDTO> getRiskMonitoringBySupplierId(Long supplierId, Long orgId) {
		// Validate supplier belongs to org
		supplierRepo.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		List<SupplierRiskMonitoring> risks = riskMonitoringRepo.findBySupplierSupplierIdAndIsActiveTrue(supplierId);
		return risks.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	public List<SupplierRiskMonitoringDTO> getRiskMonitoringByPartnershipId(Long partnershipId, Long orgId) {
		// Validate partnership belongs to org
		partnershipRepo.findByPartnershipIdAndPrimaryOrgAccountId(partnershipId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));

		List<SupplierRiskMonitoring> risks = riskMonitoringRepo.findByPartnershipId(partnershipId);
		return risks.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	public List<SupplierRiskMonitoringDTO> getRiskMonitoringByRiskLevel(String riskLevel, Long orgId) {
		validateRiskLevel(riskLevel);
		List<SupplierRiskMonitoring> risks = riskMonitoringRepo.findByRiskLevel(riskLevel.toUpperCase());
		return risks.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	public List<SupplierRiskMonitoringDTO> getRiskMonitoringDueForReview(Long orgId) {
		List<SupplierRiskMonitoring> risks = riskMonitoringRepo.findDueForReview(LocalDate.now());
		return risks.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public SupplierRiskMonitoringDTO updateRiskMonitoring(Long riskMonitoringId,
			SupplierRiskMonitoringUpdateRequest request, Long orgId) {
		SupplierRiskMonitoring riskMonitoring = riskMonitoringRepo
				.findByIdAndSupplierSupplierId(riskMonitoringId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("SupplierRiskMonitoring", "riskMonitoringId",
						riskMonitoringId));

		if (request.getPartnershipId() != null) {
			Partnership partnership = partnershipRepo
					.findByPartnershipIdAndPrimaryOrgAccountId(request.getPartnershipId(), orgId)
					.orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId",
							request.getPartnershipId()));
			riskMonitoring.setPartnership(partnership);
		}

		if (request.getRiskCategory() != null) {
			validateRiskCategory(request.getRiskCategory());
			riskMonitoring.setRiskCategory(request.getRiskCategory().toUpperCase());
		}

		if (request.getRiskLevel() != null) {
			validateRiskLevel(request.getRiskLevel());
			riskMonitoring.setRiskLevel(request.getRiskLevel().toUpperCase());
		}

		if (request.getRiskScore() != null) {
			riskMonitoring.setRiskScore(request.getRiskScore());
		}

		if (request.getRiskDescription() != null) {
			riskMonitoring.setRiskDescription(request.getRiskDescription());
		}

		if (request.getMitigationPlan() != null) {
			riskMonitoring.setMitigationPlan(request.getMitigationPlan());
		}

		if (request.getLastAssessedDate() != null) {
			riskMonitoring.setLastAssessedDate(request.getLastAssessedDate());
		}

		if (request.getNextReviewDate() != null) {
			riskMonitoring.setNextReviewDate(request.getNextReviewDate());
		}

		if (request.getIsActive() != null) {
			riskMonitoring.setIsActive(request.getIsActive());
		}

		if (request.getSource() != null) {
			riskMonitoring.setSource(request.getSource());
		}

		if (request.getReferenceDocumentId() != null) {
			riskMonitoring.setReferenceDocumentId(request.getReferenceDocumentId());
		}

		if (request.getAssessedBy() != null) {
			riskMonitoring.setAssessedBy(request.getAssessedBy());
		}

		if (request.getReviewedBy() != null) {
			riskMonitoring.setReviewedBy(request.getReviewedBy());
		}

		if (request.getStatus() != null) {
			validateStatus(request.getStatus());
			riskMonitoring.setStatus(request.getStatus().toUpperCase());
		}

		if (request.getEscalationLevel() != null) {
			riskMonitoring.setEscalationLevel(request.getEscalationLevel());
		}

		if (request.getNotes() != null) {
			riskMonitoring.setNotes(request.getNotes());
		}

		SupplierRiskMonitoring saved = riskMonitoringRepo.save(riskMonitoring);
		return mapToDTO(saved);
	}

	@Override
	@Transactional
	public void deleteRiskMonitoring(Long riskMonitoringId, Long orgId) {
		SupplierRiskMonitoring riskMonitoring = riskMonitoringRepo
				.findByIdAndSupplierSupplierId(riskMonitoringId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("SupplierRiskMonitoring", "riskMonitoringId",
						riskMonitoringId));
		riskMonitoringRepo.delete(riskMonitoring);
	}

	@Override
	public SupplierRiskSummaryDTO getSupplierRiskSummary(Long supplierId, Long orgId) {
		// Validate supplier belongs to org
		Supplier supplier = supplierRepo.findBySupplierIdAndAccountAccountId(supplierId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier", "supplierId", supplierId));

		List<SupplierRiskMonitoring> risks = riskMonitoringRepo.findBySupplierSupplierIdAndIsActiveTrue(supplierId);

		SupplierRiskSummaryDTO summary = new SupplierRiskSummaryDTO();
		summary.setSupplierId(supplierId);
		summary.setSupplierName(supplier.getBusinessName());
		summary.setTotalRisks((long) risks.size());

		long critical = risks.stream().filter(r -> "CRITICAL".equals(r.getRiskLevel())).count();
		long high = risks.stream().filter(r -> "HIGH".equals(r.getRiskLevel())).count();
		long medium = risks.stream().filter(r -> "MEDIUM".equals(r.getRiskLevel())).count();
		long low = risks.stream().filter(r -> "LOW".equals(r.getRiskLevel())).count();

		summary.setCriticalRisks(critical);
		summary.setHighRisks(high);
		summary.setMediumRisks(medium);
		summary.setLowRisks(low);

		Double avgScore = risks.stream()
				.filter(r -> r.getRiskScore() != null)
				.mapToDouble(SupplierRiskMonitoring::getRiskScore)
				.average()
				.orElse(0.0);
		summary.setAverageRiskScore(avgScore);

		// Determine overall risk level
		if (critical > 0) {
			summary.setOverallRiskLevel("CRITICAL");
		} else if (high > 0) {
			summary.setOverallRiskLevel("HIGH");
		} else if (medium > 0) {
			summary.setOverallRiskLevel("MEDIUM");
		} else {
			summary.setOverallRiskLevel("LOW");
		}

		summary.setOpenRisks(risks.stream().filter(r -> "OPEN".equals(r.getStatus())).count());
		summary.setInProgressRisks(risks.stream().filter(r -> "IN_PROGRESS".equals(r.getStatus())).count());
		summary.setMitigatedRisks(risks.stream().filter(r -> "MITIGATED".equals(r.getStatus())).count());
		summary.setClosedRisks(risks.stream().filter(r -> "CLOSED".equals(r.getStatus())).count());
		summary.setEscalatedRisks(risks.stream().filter(r -> "ESCALATED".equals(r.getStatus())).count());

		long overdue = risks.stream()
				.filter(r -> r.getNextReviewDate() != null && r.getNextReviewDate().isBefore(LocalDate.now()))
				.filter(r -> "OPEN".equals(r.getStatus()) || "IN_PROGRESS".equals(r.getStatus()))
				.count();
		summary.setOverdueReviews(overdue);

		// Group by category
		List<SupplierRiskSummaryDTO.RiskCategorySummary> byCategory = risks.stream()
				.collect(Collectors.groupingBy(SupplierRiskMonitoring::getRiskCategory))
				.entrySet().stream()
				.map(entry -> {
					List<SupplierRiskMonitoring> catRisks = entry.getValue();
					Double catAvg = catRisks.stream()
							.filter(r -> r.getRiskScore() != null)
							.mapToDouble(SupplierRiskMonitoring::getRiskScore)
							.average()
							.orElse(0.0);
					String highestLevel = catRisks.stream()
							.map(SupplierRiskMonitoring::getRiskLevel)
							.max((a, b) -> getRiskLevelOrder(a) - getRiskLevelOrder(b))
							.orElse("LOW");
					return SupplierRiskSummaryDTO.RiskCategorySummary.builder()
							.riskCategory(entry.getKey())
							.count((long) catRisks.size())
							.averageScore(catAvg)
							.highestRiskLevel(highestLevel)
							.build();
				})
				.collect(Collectors.toList());
		summary.setRiskByCategory(byCategory);

		return summary;
	}

	@Override
	public List<SupplierRiskMonitoringDTO> getRiskMonitoringByCategory(Long supplierId, String riskCategory,
			Long orgId) {
		validateRiskCategory(riskCategory);
		List<SupplierRiskMonitoring> risks = riskMonitoringRepo.findBySupplierIdAndRiskCategory(supplierId,
				riskCategory.toUpperCase());
		return risks.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	@Override
	public List<SupplierRiskMonitoringDTO> getAllRiskMonitoring(Long orgId, int pageNo, int pageSize) {
		Pageable pageable = PageRequest.of(pageNo, pageSize);
		Page<SupplierRiskMonitoring> page = riskMonitoringRepo.findAll(pageable);
		return page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList());
	}

	private SupplierRiskMonitoringDTO mapToDTO(SupplierRiskMonitoring entity) {
		SupplierRiskMonitoringDTO dto = modelMapper.map(entity, SupplierRiskMonitoringDTO.class);
		dto.setSupplierId(entity.getSupplier().getSupplierId());
		dto.setSupplierName(entity.getSupplier().getBusinessName());
		if (entity.getPartnership() != null) {
			dto.setPartnershipId(entity.getPartnership().getPartnershipId());
			dto.setPartnershipName(entity.getPartnership().getPartnershipTerm());
		}
		return dto;
	}

	private void validateRiskCategory(String category) {
		List<String> validCategories = List.of("FINANCIAL", "OPERATIONAL", "COMPLIANCE", "REPUTATIONAL", "GEOPOLITICAL",
				"CYBER");
		if (!validCategories.contains(category.toUpperCase())) {
			throw new ValidationException("Invalid risk category. Valid values: " + validCategories);
		}
	}

	private void validateRiskLevel(String level) {
		List<String> validLevels = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
		if (!validLevels.contains(level.toUpperCase())) {
			throw new ValidationException("Invalid risk level. Valid values: " + validLevels);
		}
	}

	private void validateStatus(String status) {
		List<String> validStatuses = List.of("OPEN", "IN_PROGRESS", "MITIGATED", "CLOSED", "ESCALATED");
		if (!validStatuses.contains(status.toUpperCase())) {
			throw new ValidationException("Invalid status. Valid values: " + validStatuses);
		}
	}

	private int getRiskLevelOrder(String level) {
		return switch (level.toUpperCase()) {
			case "CRITICAL" -> 4;
			case "HIGH" -> 3;
			case "MEDIUM" -> 2;
			case "LOW" -> 1;
			default -> 0;
		};
	}
}