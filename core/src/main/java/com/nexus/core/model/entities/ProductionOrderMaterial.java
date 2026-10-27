package com.nexus.core.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_production_order_materials", schema = "core")
public class ProductionOrderMaterial extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "production_order_material_id")
	private Long productionOrderMaterialId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "production_order_id", referencedColumnName = "production_order_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private ProductionOrder productionOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "material_id", referencedColumnName = "material_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Material material;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Warehouse warehouse;

	private Double requiredQty;

	private Double allocatedQty;
}