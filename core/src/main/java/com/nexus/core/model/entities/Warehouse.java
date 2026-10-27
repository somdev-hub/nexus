package com.nexus.core.model.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_warehouses", schema = "core")
public class Warehouse extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "warehouse_id")
	private Long warehouseId;

	private String code;

	private Long warehouseManager;

	private Long org;

	private String location;

	private Double storageCapacity;

	private Double currentUtilization;

	// Production vs reserve warehouse (capacity planning)
	@Column(name = "warehouse_type")
	private String warehouseType = "RESERVE"; // PRODUCTION | RESERVE

	@Column(name = "capacity_multiplier")
	private Double capacityMultiplier = 1.0;

	@Column(name = "capacity")
	private Double capacity;

	@Column(name = "fill_order")
	private Integer fillOrder = 0;

	@OneToMany(mappedBy = "warehouse")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Material> materials;

}
