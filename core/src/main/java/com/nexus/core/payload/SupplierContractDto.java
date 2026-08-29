package com.nexus.core.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierContractDto {

	private Long contractId;

	@NotNull(message = "Account ID is required")
	private Long accountId;

	@NotNull(message = "Supplier ID is required")
	private Long supplierId;

	@NotBlank(message = "Contract number is required")
	@Size(max = 50, message = "Contract number must not exceed 50 characters")
	private String contractNumber;

	@NotBlank(message = "Contract name is required")
	@Size(max = 200, message = "Contract name must not exceed 200 characters")
	private String contractName;

	@Size(max = 2000, message = "Description must not exceed 2000 characters")
	private String description;

	@NotNull(message = "Contract type is required")
	private ContractType contractType;

	@NotNull(message = "Contract status is required")
	private ContractStatus status;

	@NotNull(message = "Effective date is required")
	private LocalDate effectiveDate;

	private LocalDate expiryDate;

	private Boolean autoRenewal = false;

	@Min(value = 0, message = "Renewal notice days must be non-negative")
	private Integer renewalNoticeDays = 30;

	@Size(max = 3, message = "Base currency must be 3 characters")
	private String baseCurrency = "USD";

	@Min(value = 0, message = "Payment terms days must be non-negative")
	private Integer paymentTermsDays = 30;

	@Size(max = 10, message = "Incoterms must not exceed 10 characters")
	private String incoterms;

	@Min(value = 0, message = "SLA lead time days must be non-negative")
	private Integer slaLeadTimeDays;

	@DecimalMin(value = "0.00", message = "SLA on-time delivery percentage must be non-negative")
	@DecimalMax(value = "100.00", message = "SLA on-time delivery percentage must not exceed 100")
	private BigDecimal slaOnTimeDeliveryPct;

	@DecimalMin(value = "0.00", message = "SLA quality defect rate percentage must be non-negative")
	@DecimalMax(value = "100.00", message = "SLA quality defect rate percentage must not exceed 100")
	private BigDecimal slaQualityDefectRatePct;

	@Min(value = 0, message = "SLA response time hours must be non-negative")
	private Integer slaResponseTimeHours;

	@Min(value = 0, message = "Volume discount tier 1 quantity must be non-negative")
	private Integer volumeDiscountTier1Qty;

	@DecimalMin(value = "0.00", message = "Volume discount tier 1 percentage must be non-negative")
	@DecimalMax(value = "100.00", message = "Volume discount tier 1 percentage must not exceed 100")
	private BigDecimal volumeDiscountTier1Pct;

	@Min(value = 0, message = "Volume discount tier 2 quantity must be non-negative")
	private Integer volumeDiscountTier2Qty;

	@DecimalMin(value = "0.00", message = "Volume discount tier 2 percentage must be non-negative")
	@DecimalMax(value = "100.00", message = "Volume discount tier 2 percentage must not exceed 100")
	private BigDecimal volumeDiscountTier2Pct;

	@Min(value = 0, message = "Volume discount tier 3 quantity must be non-negative")
	private Integer volumeDiscountTier3Qty;

	@DecimalMin(value = "0.00", message = "Volume discount tier 3 percentage must be non-negative")
	@DecimalMax(value = "100.00", message = "Volume discount tier 3 percentage must not exceed 100")
	private BigDecimal volumeDiscountTier3Pct;

	private String dmsDocumentId;

	private String dmsDocumentName;

	private Integer dmsDocumentVersion = 1;

	private String approvedBy;

	private LocalDateTime approvedAt;

	private String rejectionReason;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private Long version;

	public enum ContractType {
		STANDARD,
		BLANKET,
		FRAMEWORK,
		CONSIGNMENT,
		VMI
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