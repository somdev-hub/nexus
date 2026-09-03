package com.nexus.core.dto;

import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * DTO for Supplier Risk Monitoring.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SupplierRiskMonitoringDTO {

	private Long riskMonitoringId;
	private Long supplierId;
	private String supplierName;
	private Long partnershipId;
	private String partnershipName;
	private String riskCategory;
	private String riskLevel;
	private Double riskScore;
	private String riskDescription;
	private String mitigationPlan;
	private LocalDate identifiedDate;
	private LocalDate lastAssessedDate;
	private LocalDate nextReviewDate;
	private Boolean isActive;
	private String source;
	private String referenceDocumentId;
	private String assessedBy;
	private String reviewedBy;
	private String status;
	private Integer escalationLevel;
	private String notes;
	private Timestamp createdAt;
	private Timestamp updatedAt;
}