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
@Table(name = "t_invoice_line_items", schema = "core")
public class InvoiceLineItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "invoice_line_item_id")
	private Long invoiceLineItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invoice_id", referencedColumnName = "invoice_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Invoice invoice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "po_line_item_id", referencedColumnName = "line_item_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PurchaseOrderLineItem poLineItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gr_line_item_id", referencedColumnName = "gr_line_item_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private GoodsReceiptLineItem grLineItem;

	@Column(name = "line_number")
	private Integer lineNumber;

	@Column(name = "quantity_invoiced", precision = 19, scale = 4)
	private BigDecimal quantityInvoiced;

	@Column(name = "unit_price", precision = 19, scale = 4)
	private BigDecimal unitPrice;

	@Column(name = "line_total", precision = 19, scale = 4)
	private BigDecimal lineTotal;

	@Column(name = "unit_of_measure")
	private String unitOfMeasure;

	@Column(name = "notes")
	private String notes;

	@Version
	private Long version = 0L;
}