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
@Table(name = "t_buyer_org_material_availabilities", schema = "core")
public class BuyerOrgMaterialAvailability extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "availability_id")
	private Long availabilityId;

	private Long org;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "material_id", referencedColumnName = "material_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Material material;

	private Double availableQuantity;

	private Double reservedQuantity;

}
