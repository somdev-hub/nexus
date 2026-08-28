package com.nexus.core.entities;

import java.util.List;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_materials", schema = "core")
public class Material extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "material_id")
	private Long materialId;

	private String name;

	private String code;

	private Long org;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Warehouse warehouse;

	private Double pricePerUnit;

	private String unit;

	private Double productionCostPerUnit;

	private Double productionCapacityPerMonth;

	private Double availableQuantity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Product product;

	@OneToMany(mappedBy = "material")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<MaterialRequirement> materialRequirements;

	@OneToMany(mappedBy = "material")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<BuyerOrgMaterialAvailability> buyerAvailabilities;

	@OneToMany(mappedBy = "material")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Order> orders;

	// FR-RET-011: Reorder Point Automation
	@Column(name = "reorder_point")
	private Double reorderPoint;

	@Column(name = "reorder_quantity")
	private Double reorderQuantity;

	@Column(name = "min_stock_level")
	private Double minStockLevel;

	@Column(name = "max_stock_level")
	private Double maxStockLevel;

	@Column(name = "lead_time_days")
	private Integer leadTimeDays;

	@Column(name = "safety_stock")
	private Double safetyStock;

	// FR-RET-013: Expiry and Batch Tracking
	@Column(name = "track_batches")
	private Boolean trackBatches = false;

	@Column(name = "track_expiry")
	private Boolean trackExpiry = false;

	@Column(name = "shelf_life_days")
	private Integer shelfLifeDays;

	// FR-RET-014: Inventory Valuation
	@Enumerated(EnumType.STRING)
	@Column(name = "valuation_method")
	private Stock.ValuationMethod valuationMethod = Stock.ValuationMethod.WEIGHTED_AVERAGE;

	@Column(name = "standard_cost")
	private Double standardCost;

	@Column(name = "last_purchase_price")
	private Double lastPurchasePrice;

	@Column(name = "average_cost")
	private Double averageCost;
}
