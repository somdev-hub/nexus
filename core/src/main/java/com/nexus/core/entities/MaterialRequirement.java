package com.nexus.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_material_requirements", schema = "core")
public class MaterialRequirement extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "material_requirement_id")
	private Long materialRequirementId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "material_id", referencedColumnName = "material_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Material material;

	private Double quantity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Product product;

}
