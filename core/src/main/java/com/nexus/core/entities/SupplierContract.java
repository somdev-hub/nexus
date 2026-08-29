package com.nexus.core.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_supplier_contracts", schema = "core", uniqueConstraints = {
		@UniqueConstraint(name = "uk_supplier_contract_number", columnNames = { "account_id", "contract_number" })
})
public class SupplierContract extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "contract_id")
	private Long contractId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", referencedColumnName = "account_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account account;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Supplier supplier;

	@Column(name = "contract_number", nullable = false, length = 50)
	private String contractNumber;

	@Column(name = "contract_name", nullable = false, length = 200)
	private String contractName;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "contract_type", nullable = false, length = 30)
	private ContractType contractType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private ContractStatus status;

	@Column(name = "effective_date", nullable = false)
	private LocalDate effectiveDate;

	@Column(name = "expiry_date")
	private LocalDate expiryDate;

	@Column(name = "auto_renewal")
	private Boolean autoRenewal = false;

	@Column(name = "renewal_notice_days")
	private Integer renewalNoticeDays = 30;

	// Pricing tiers
	@Column(name = "base_currency", length = 3)
	private String baseCurrency = "USD";

	@Column(name = "payment_terms_days")
	private Integer paymentTermsDays = 30;

	@Column(name = "incoterms", length = 10)
	private String incoterms;

	// SLA fields
	@Column(name = "sla_lead_time_days")
	private Integer slaLeadTimeDays;

	@Column(name = "sla_on_time_delivery_pct", precision = 5, scale = 2)
	private BigDecimal slaOnTimeDeliveryPct;

	@Column(name = "sla_quality_defect_rate_pct", precision = 5, scale = 2)
	private BigDecimal slaQualityDefectRatePct;

	@Column(name = "sla_response_time_hours")
	private Integer slaResponseTimeHours;

	// Volume discounts
	@Column(name = "volume_discount_tier1_qty")
	private Integer volumeDiscountTier1Qty;

	@Column(name = "volume_discount_tier1_pct", precision = 5, scale = 2)
	private BigDecimal volumeDiscountTier1Pct;

	@Column(name = "volume_discount_tier2_qty")
	private Integer volumeDiscountTier2Qty;

	@Column(name = "volume_discount_tier2_pct", precision = 5, scale = 2)
	private BigDecimal volumeDiscountTier2Pct;

	@Column(name = "volume_discount_tier3_qty")
	private Integer volumeDiscountTier3Qty;

	@Column(name = "volume_discount_tier3_pct", precision = 5, scale = 2)
	private BigDecimal volumeDiscountTier3Pct;

	// DMS document reference
	@Column(name = "dms_document_id")
	private String dmsDocumentId;

	@Column(name = "dms_document_name")
	private String dmsDocumentName;

	@Column(name = "dms_document_url")
	private String dmsDocumentUrl;

	@Column(name = "dms_document_version")
	private Integer dmsDocumentVersion = 1;

	// Approval workflow
	@Column(name = "approved_by")
	private String approvedBy;

	@Column(name = "approved_at")
	private java.time.LocalDateTime approvedAt;

	@Column(name = "rejection_reason", columnDefinition = "TEXT")
	private String rejectionReason;

	public enum ContractType {
		STANDARD, // Standard purchase agreement
		BLANKET, // Blanket order agreement
		FRAMEWORK, // Framework agreement
		CONSIGNMENT, // Consignment agreement
		VMI // Vendor Managed Inventory
	}

	public enum ContractStatus {
		DRAFT,
		PENDING_APPROVAL,
		ACTIVE,
		EXPIRED,
		TERMINATED,
		SUSPENDED,
		RENEWAL_PENDING
	}
}