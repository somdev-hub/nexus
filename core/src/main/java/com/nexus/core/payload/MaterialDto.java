package com.nexus.core.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import com.nexus.core.entities.Stock.ValuationMethod;

@Data
public class MaterialDto {
	@NotBlank(message = "Material name is required")
	private String name;

	@NotBlank(message = "Material code is required")
	private String code;

	@NotNull(message = "Organization is required")
	private Long org;

	private Long warehouseId;

	private Double pricePerUnit;

	private String unit;

	private Double productionCostPerUnit;

	private Double productionCapacityPerMonth;

	private Double availableQuantity;

	// FR-RET-011: Reorder Point Automation
	private Double reorderPoint;

	private Double reorderQuantity;

	private Double minStockLevel;

	private Double maxStockLevel;

	private Integer leadTimeDays;

	private Double safetyStock;

	// FR-RET-013: Expiry and Batch Tracking
	private Boolean trackBatches = false;

	private Boolean trackExpiry = false;

	private Integer shelfLifeDays;

	// FR-RET-014: Inventory Valuation
	private ValuationMethod valuationMethod = ValuationMethod.WEIGHTED_AVERAGE;

	private Double standardCost;

	private Double lastPurchasePrice;

	private Double averageCost;
}
