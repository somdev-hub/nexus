package com.nexus.core.entities;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "t_products", schema = "core")
public class Product extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long productId;

	private String name;

	private String code;

	@Column(columnDefinition = "TEXT")
	private String description;

	private List<String> productImages;

	@OneToMany(mappedBy = "product")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<MaterialRequirement> materialRequirements;

	private Long org;

	private Long productManager;

	private Double price;

	private Double sellingPrice;

	private Double cost;

	private Boolean taxCharged;

	private Double taxPercentage;

	@Enumerated(EnumType.STRING)
	private ProductStatus productStatus;

	@Enumerated(EnumType.STRING)
	private ProductCategory productCategory;

	@OneToMany(mappedBy = "product")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Order> orders;

}
