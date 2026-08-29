package com.nexus.core.entities;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_supplier_performance", schema = "core")
public class SupplierPerformance extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "performance_id")
	private Long performanceId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Supplier supplier;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account account;

	@Column(name = "evaluation_period_start")
	private Date evaluationPeriodStart;

	@Column(name = "evaluation_period_end")
	private Date evaluationPeriodEnd;

	// OTIF (On-Time In-Full) - percentage
	@Column(name = "otif_score", precision = 5, scale = 2)
	private BigDecimal otifScore;

	// Quality defect rate - percentage
	@Column(name = "quality_defect_rate", precision = 5, scale = 2)
	private BigDecimal qualityDefectRate;

	// Average lead time in days
	@Column(name = "avg_lead_time_days")
	private Integer avgLeadTimeDays;

	// Responsiveness score - percentage (response time to communications, issues)
	@Column(name = "responsiveness_score", precision = 5, scale = 2)
	private BigDecimal responsivenessScore;

	// Overall composite score
	@Column(name = "overall_score", precision = 5, scale = 2)
	private BigDecimal overallScore;

	// Performance tier
	@Enumerated(EnumType.STRING)
	@Column(name = "performance_tier")
	private PerformanceTier performanceTier;

	// Total orders in evaluation period
	@Column(name = "total_orders_evaluated")
	private Integer totalOrdersEvaluated;

	// On-time deliveries count
	@Column(name = "on_time_deliveries")
	private Integer onTimeDeliveries;

	// In-full deliveries count
	@Column(name = "in_full_deliveries")
	private Integer inFullDeliveries;

	// Total defects reported
	@Column(name = "total_defects")
	private Integer totalDefects;

	// Total units received
	@Column(name = "total_units_received")
	private Integer totalUnitsReceived;

	// Average response time in hours
	@Column(name = "avg_response_time_hours")
	private Integer avgResponseTimeHours;

	// Calculated at timestamp
	@Column(name = "calculated_at")
	private Timestamp calculatedAt;

	// Calculated by user
	@Column(name = "calculated_by")
	private String calculatedBy;

	@Version
	private Long version = 0L;

	public enum PerformanceTier {
		EXCELLENT, // >= 90%
		GOOD, // >= 75%
		AVERAGE, // >= 60%
		BELOW_AVERAGE, // >= 40%
		POOR // < 40%
	}

	// Helper method to calculate overall score
	public void calculateOverallScore() {
		if (otifScore != null && qualityDefectRate != null && responsivenessScore != null) {
			// Weighted average: OTIF 40%, Quality (inverse of defect rate) 30%,
			// Responsiveness 30%
			BigDecimal qualityScore = BigDecimal.valueOf(100).subtract(qualityDefectRate);
			this.overallScore = otifScore.multiply(BigDecimal.valueOf(0.4))
					.add(qualityScore.multiply(BigDecimal.valueOf(0.3)))
					.add(responsivenessScore.multiply(BigDecimal.valueOf(0.3)));
			this.overallScore = this.overallScore.setScale(2, BigDecimal.ROUND_HALF_UP);
		}
	}

	// Helper method to determine performance tier
	public void determinePerformanceTier() {
		if (overallScore != null) {
			if (overallScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
				this.performanceTier = PerformanceTier.EXCELLENT;
			} else if (overallScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
				this.performanceTier = PerformanceTier.GOOD;
			} else if (overallScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
				this.performanceTier = PerformanceTier.AVERAGE;
			} else if (overallScore.compareTo(BigDecimal.valueOf(40)) >= 0) {
				this.performanceTier = PerformanceTier.BELOW_AVERAGE;
			} else {
				this.performanceTier = PerformanceTier.POOR;
			}
		}
	}
}