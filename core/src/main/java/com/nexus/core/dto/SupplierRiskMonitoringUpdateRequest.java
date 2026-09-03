package com.nexus.core.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Update request for Supplier Risk Monitoring.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SupplierRiskMonitoringUpdateRequest {

	private Long partnershipId;

	@Size(max = 50, message = "Risk category must not exceed 50 characters")
	private String riskCategory;

	@Size(max = 20, message = "Risk level must not exceed 20 characters")
	private String riskLevel;

	@DecimalMin(value = "0.0", message = "Risk score must be >= 0")
	@DecimalMax(value = "100.0", message = "Risk score must be <= 100")
	private Double riskScore;

	@Size(max = 2000, message = "Risk description must not exceed 2000 characters")
	private String riskDescription;

	@Size(max = 2000, message = "Mitigation plan must not exceed 2000 characters")
	private String mitigationPlan;

	private LocalDate lastAssessedDate;

	private LocalDate nextReviewDate;

	private Boolean isActive;

	@Size(max = 50, message = "Source must not exceed 50 characters")
	private String source;

	@Size(max = 100, message = "Reference document ID must not exceed 100 characters")
	private String referenceDocumentId;

	@Size(max = 100, message = "Assessed by must not exceed 100 characters")
	private String assessedBy;

	@Size(max = 100, message = "Reviewed by must not exceed 100 characters")
	private String reviewedBy;

	@Size(max = 20, message = "Status must not exceed 20 characters")
	private String status;

	private Integer escalationLevel;

	@Size(max = 2000, message = "Notes must not exceed 2000 characters")
	private String notes;
}