package com.nexus.core.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_goods_receipt_line_items", schema = "core")
public class GoodsReceiptLineItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "gr_line_item_id")
	private Long grLineItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "goods_receipt_id", referencedColumnName = "goods_receipt_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private GoodsReceipt goodsReceipt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "po_line_item_id", referencedColumnName = "po_line_item_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PurchaseOrderLineItem poLineItem;

	@Column(name = "line_number")
	private Integer lineNumber;

	@Column(name = "quantity_received", precision = 19, scale = 4)
	private BigDecimal quantityReceived;

	@Column(name = "quantity_accepted", precision = 19, scale = 4)
	private BigDecimal quantityAccepted;

	@Column(name = "quantity_rejected", precision = 19, scale = 4)
	private BigDecimal quantityRejected;

	@Column(name = "unit_of_measure")
	private String unitOfMeasure;

	@Column(name = "notes")
	private String notes;

	@Version
	private Long version = 0L;
}