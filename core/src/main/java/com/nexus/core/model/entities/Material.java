package com.nexus.core.model.entities;

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

	private String category;

	private Long org;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Warehouse warehouse;

	private Double pricePerUnit;

	private String currency = "USD";

	private String unit;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Product product;

	// Link to the supplier catalog item this material was received from.
	// Materials are no longer created manually by the retailer; they are
	// derived from received purchase-order lines (supplier catalog items).
	@Column(name = "catalog_id")
	private Long catalogId;

	@Column(name = "supplier_org_id")
	private Long supplierOrgId;

	@Column(name = "variant")
	private String variant;

	@OneToMany(mappedBy = "material")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<MaterialRequirement> materialRequirements;

	@OneToMany(mappedBy = "material")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<BuyerOrgMaterialAvailability> buyerAvailabilities;

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

	// FR-RET-014: Inventory Valuation
	@Enumerated(EnumType.STRING)
	@Column(name = "valuation_method")
	private Stock.ValuationMethod valuationMethod = Stock.ValuationMethod.WEIGHTED_AVERAGE;

	@Column(name = "standard_cost")
	private Double standardCost;

	@Column(name = "average_cost")
	private Double averageCost;
}
