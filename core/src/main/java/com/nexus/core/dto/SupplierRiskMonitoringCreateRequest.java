package com.nexus.core.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Create request for Supplier Risk Monitoring.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SupplierRiskMonitoringCreateRequest {

	@NotNull(message = "Supplier ID is required")
	private Long supplierId;

	private Long partnershipId;

	@NotBlank(message = "Risk category is required")
	@Size(max = 50, message = "Risk category must not exceed 50 characters")
	private String riskCategory; // FINANCIAL, OPERATIONAL, COMPLIANCE, REPUTATIONAL, GEOPOLITICAL, CYBER

	@NotBlank(message = "Risk level is required")
	@Size(max = 20, message = "Risk level must not exceed 20 characters")
	private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

	@DecimalMin(value = "0.0", message = "Risk score must be >= 0")
	@DecimalMax(value = "100.0", message = "Risk score must be <= 100")
	private Double riskScore;

	@Size(max = 2000, message = "Risk description must not exceed 2000 characters")
	private String riskDescription;

	@Size(max = 2000, message = "Mitigation plan must not exceed 2000 characters")
	private String mitigationPlan;

	@NotNull(message = "Identified date is required")
	private LocalDate identifiedDate;

	private LocalDate lastAssessedDate;

	private LocalDate nextReviewDate;

	private Boolean isActive = true;

	@Size(max = 50, message = "Source must not exceed 50 characters")
	private String source; // INTERNAL_ASSESSMENT, EXTERNAL_REPORT, NEWS, FINANCIAL_STATEMENT, AUDIT

	@Size(max = 100, message = "Reference document ID must not exceed 100 characters")
	private String referenceDocumentId;

	@Size(max = 100, message = "Assessed by must not exceed 100 characters")
	private String assessedBy;

	@Size(max = 100, message = "Reviewed by must not exceed 100 characters")
	private String reviewedBy;

	@Size(max = 20, message = "Status must not exceed 20 characters")
	private String status = "OPEN"; // OPEN, IN_PROGRESS, MITIGATED, CLOSED, ESCALATED

	private Integer escalationLevel = 0;

	@Size(max = 2000, message = "Notes must not exceed 2000 characters")
	private String notes;
}