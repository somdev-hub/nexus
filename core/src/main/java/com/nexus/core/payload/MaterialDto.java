package com.nexus.core.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import com.nexus.core.model.entities.Stock.ValuationMethod;

@Data
public class MaterialDto {
	private Long materialId;

	@NotBlank(message = "Material name is required")
	private String name;

	@NotBlank(message = "Material code is required")
	private String code;

	private String category;

	@NotNull(message = "Organization is required")
	private Long org;

	private Long warehouseId;

	private Double pricePerUnit;

	private String currency = "USD";

	private String unit;

	private Double availableQuantity;

	// FR-RET-011: Reorder Point Automation
	private Double reorderPoint;

	private Double reorderQuantity;

	private Double minStockLevel;

	private Double maxStockLevel;

	private Integer leadTimeDays;

	// FR-RET-014: Inventory Valuation
	private ValuationMethod valuationMethod = ValuationMethod.WEIGHTED_AVERAGE;

	private Double standardCost;

	private Double averageCost;

	private Boolean isActive = true;
}
