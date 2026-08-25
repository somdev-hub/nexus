package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
}
