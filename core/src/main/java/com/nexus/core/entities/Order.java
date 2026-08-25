package com.nexus.core.entities;

import java.sql.Date;
import java.sql.Timestamp;

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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "t_orders", schema = "core")
public class Order extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long orderId;

	private String orderNumber;

	private Long buyerOrgId;

	private Long sellerOrgId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "material_id", referencedColumnName = "material_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Material material;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Product product;

	private Double quantity;

	private Timestamp createdOn;

	private Date promisedDeliveryDate;

	private Date actualDeliveryDate;

	private Long logisticId;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

}
