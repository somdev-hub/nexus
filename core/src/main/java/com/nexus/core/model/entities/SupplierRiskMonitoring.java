package com.nexus.core.model.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Supplier Risk Monitoring entity for tracking supplier risk indicators.
 * FR-RET-024: Supplier Risk Monitoring
 */
@Entity
@Table(name = "supplier_risk_monitoring")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "supplier", "partnership" })
@EqualsAndHashCode(callSuper = false, exclude = { "supplier", "partnership" })
public class SupplierRiskMonitoring extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "risk_monitoring_id")
	private Long riskMonitoringId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partnership_id")
	private Partnership partnership;

	@Column(name = "risk_category", nullable = false, length = 50)
	private String riskCategory; // FINANCIAL, OPERATIONAL, COMPLIANCE, REPUTATIONAL, GEOPOLITICAL, CYBER

	@Column(name = "risk_level", nullable = false, length = 20)
	private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

	@Column(name = "risk_score")
	private Double riskScore; // 0-100 scale

	@Column(name = "risk_description", columnDefinition = "TEXT")
	private String riskDescription;

	@Column(name = "mitigation_plan", columnDefinition = "TEXT")
	private String mitigationPlan;

	@Column(name = "identified_date", nullable = false)
	private LocalDate identifiedDate;

	@Column(name = "last_assessed_date")
	private LocalDate lastAssessedDate;

	@Column(name = "next_review_date")
	private LocalDate nextReviewDate;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "source")
	private String source; // INTERNAL_ASSESSMENT, EXTERNAL_REPORT, NEWS, FINANCIAL_STATEMENT, AUDIT

	@Column(name = "reference_document_id")
	private String referenceDocumentId; // DMS document reference

	@Column(name = "assessed_by")
	private String assessedBy;

	@Column(name = "reviewed_by")
	private String reviewedBy;

	@Column(name = "status", nullable = false, length = 20)
	private String status = "OPEN"; // OPEN, IN_PROGRESS, MITIGATED, CLOSED, ESCALATED

	@Column(name = "escalation_level")
	private Integer escalationLevel = 0;

	@Column(name = "notes", columnDefinition = "TEXT")
	private String notes;
}