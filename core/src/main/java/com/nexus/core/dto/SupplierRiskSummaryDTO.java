package com.nexus.core.dto;

import lombok.*;

import java.util.List;

/**
 * Summary DTO for Supplier Risk Monitoring.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SupplierRiskSummaryDTO {

	private Long supplierId;
	private String supplierName;
	private Long totalRisks;
	private Long criticalRisks;
	private Long highRisks;
	private Long mediumRisks;
	private Long lowRisks;
	private Double averageRiskScore;
	private String overallRiskLevel; // LOW, MEDIUM, HIGH, CRITICAL
	private Long openRisks;
	private Long inProgressRisks;
	private Long mitigatedRisks;
	private Long closedRisks;
	private Long escalatedRisks;
	private Long overdueReviews;
	private List<RiskCategorySummary> riskByCategory;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@ToString
	public static class RiskCategorySummary {
		private String riskCategory;
		private Long count;
		private Double averageScore;
		private String highestRiskLevel;
	}
}