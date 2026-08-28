package com.nexus.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Stock entity representing inventory quantities per material per warehouse.
 * Implements FR-RET-010: Multi-Warehouse Inventory
 * <p>
 * Tracks current stock levels, reserved quantities, and available quantities
 * for each material in each warehouse.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_stock", schema = "core", uniqueConstraints = {
		@UniqueConstraint(name = "uk_stock_material_warehouse", columnNames = { "material_id", "warehouse_id" })
})
public class Stock extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stock_id")
	private Long stockId;

	@ManyToOne
	@JoinColumn(name = "material_id", referencedColumnName = "material_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Material material;

	@ManyToOne
	@JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Warehouse warehouse;

	@Column(name = "quantity_on_hand", nullable = false)
	private Double quantityOnHand = 0.0;

	@Column(name = "quantity_reserved", nullable = false)
	private Double quantityReserved = 0.0;

	@Column(name = "quantity_available", nullable = false)
	private Double quantityAvailable = 0.0;

	@Column(name = "reorder_point")
	private Double reorderPoint;

	@Column(name = "reorder_quantity")
	private Double reorderQuantity;

	@Column(name = "max_stock_level")
	private Double maxStockLevel;

	@Column(name = "min_stock_level")
	private Double minStockLevel;

	@Enumerated(EnumType.STRING)
	@Column(name = "valuation_method")
	private ValuationMethod valuationMethod = ValuationMethod.WEIGHTED_AVERAGE;

	@Column(name = "average_cost")
	private Double averageCost = 0.0;

	@Column(name = "last_cost")
	private Double lastCost = 0.0;

	@Column(name = "standard_cost")
	private Double standardCost = 0.0;

	@Column(name = "last_counted_at")
	private java.sql.Timestamp lastCountedAt;

	@Column(name = "last_counted_by")
	private String lastCountedBy;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	/**
	 * Calculate available quantity
	 */
	public void recalculateAvailable() {
		this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
		if (this.quantityAvailable < 0) {
			this.quantityAvailable = 0.0;
		}
	}

	/**
	 * Check if stock is below reorder point
	 */
	public boolean isBelowReorderPoint() {
		return reorderPoint != null && quantityAvailable <= reorderPoint;
	}

	/**
	 * Check if stock is at or below minimum level
	 */
	public boolean isAtOrBelowMinLevel() {
		return minStockLevel != null && quantityAvailable <= minStockLevel;
	}

	/**
	 * Check if stock is at or above maximum level
	 */
	public boolean isAtOrAboveMaxLevel() {
		return maxStockLevel != null && quantityOnHand >= maxStockLevel;
	}

	/**
	 * Valuation methods for inventory (FR-RET-014)
	 */
	public enum ValuationMethod {
		FIFO,
		LIFO,
		WEIGHTED_AVERAGE,
		STANDARD_COST
	}
}