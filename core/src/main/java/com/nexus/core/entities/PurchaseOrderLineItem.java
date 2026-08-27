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
@Table(name = "t_purchase_order_line_items", schema = "core")
public class PurchaseOrderLineItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "line_item_id")
	private Long lineItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_id", referencedColumnName = "purchase_order_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PurchaseOrder purchaseOrder;

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

	private String description;

	@Column(name = "quantity_ordered")
	private Double quantityOrdered;

	@Column(name = "quantity_received")
	private Double quantityReceived = 0.0;

	@Column(name = "quantity_invoiced")
	private Double quantityInvoiced = 0.0;

	@Column(name = "unit_price")
	private Double unitPrice;

	@Column(name = "total_price")
	private Double totalPrice;

	private String unitOfMeasure;

	private Integer lineNumber;

	private String incoterms;

	private String deliveryLocation;
}