package com.nexus.core.payload;

import com.nexus.core.entities.Stock;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for Stock entity.
 * Supports FR-RET-010: Multi-Warehouse Inventory
 * Supports FR-RET-011: Reorder Point Automation
 * Supports FR-RET-014: Inventory Valuation
 */
@Data
public class StockDto {

	@NotNull(message = "Material ID is required")
	private Long materialId;

	@NotNull(message = "Warehouse ID is required")
	private Long warehouseId;

	private Double quantityOnHand = 0.0;

	private Double quantityReserved = 0.0;

	private Double quantityAvailable = 0.0;

	private Double reorderPoint;

	private Double reorderQuantity;

	private Double maxStockLevel;

	private Double minStockLevel;

	private Stock.ValuationMethod valuationMethod = Stock.ValuationMethod.WEIGHTED_AVERAGE;

	private Double averageCost = 0.0;

	private Double lastCost = 0.0;

	private Double standardCost = 0.0;

	private java.sql.Timestamp lastCountedAt;

	private String lastCountedBy;

	private Boolean isActive = true;

	// Computed fields for response
	private String materialCode;
	private String materialName;
	private String warehouseCode;
	private String warehouseLocation;
	private Boolean belowReorderPoint = false;
	private Boolean atOrBelowMinLevel = false;
	private Boolean atOrAboveMaxLevel = false;
	private Double totalValue = 0.0;
}