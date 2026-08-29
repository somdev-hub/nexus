package com.nexus.core.payload;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierPerformanceDto {

	private Long performanceId;

	@NotNull(message = "Supplier ID is required")
	private Long supplierId;

	@NotNull(message = "Account ID is required")
	private Long accountId;

	@NotNull(message = "Evaluation period start is required")
	private Date evaluationPeriodStart;

	@NotNull(message = "Evaluation period end is required")
	private Date evaluationPeriodEnd;

	@DecimalMin(value = "0.00", message = "OTIF score must be >= 0")
	@DecimalMax(value = "100.00", message = "OTIF score must be <= 100")
	private BigDecimal otifScore;

	@DecimalMin(value = "0.00", message = "Quality defect rate must be >= 0")
	@DecimalMax(value = "100.00", message = "Quality defect rate must be <= 100")
	private BigDecimal qualityDefectRate;

	@Min(value = 0, message = "Average lead time days must be >= 0")
	private Integer avgLeadTimeDays;

	@DecimalMin(value = "0.00", message = "Responsiveness score must be >= 0")
	@DecimalMax(value = "100.00", message = "Responsiveness score must be <= 100")
	private BigDecimal responsivenessScore;

	@DecimalMin(value = "0.00", message = "Overall score must be >= 0")
	@DecimalMax(value = "100.00", message = "Overall score must be <= 100")
	private BigDecimal overallScore;

	private com.nexus.core.entities.SupplierPerformance.PerformanceTier performanceTier;

	@Min(value = 0, message = "Total orders evaluated must be >= 0")
	private Integer totalOrdersEvaluated;

	@Min(value = 0, message = "On-time deliveries must be >= 0")
	private Integer onTimeDeliveries;

	@Min(value = 0, message = "In-full deliveries must be >= 0")
	private Integer inFullDeliveries;

	@Min(value = 0, message = "Total defects must be >= 0")
	private Integer totalDefects;

	@Min(value = 0, message = "Total units received must be >= 0")
	private Integer totalUnitsReceived;

	@Min(value = 0, message = "Average response time hours must be >= 0")
	private Integer avgResponseTimeHours;

	private Timestamp calculatedAt;

	private String calculatedBy;

	private Timestamp createdAt;

	private Timestamp updatedAt;

	private Integer version;
}