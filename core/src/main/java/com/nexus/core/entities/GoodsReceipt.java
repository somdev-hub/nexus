package com.nexus.core.entities;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_goods_receipts", schema = "core")
public class GoodsReceipt extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "goods_receipt_id")
	private Long goodsReceiptId;

	@Column(name = "gr_number", unique = true)
	private String grNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_id", referencedColumnName = "purchase_order_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private PurchaseOrder purchaseOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Supplier supplier;

	@Enumerated(EnumType.STRING)
	private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

	@Column(name = "received_date")
	private Date receivedDate;

	@Column(name = "delivery_note_number")
	private String deliveryNoteNumber;

	@Column(name = "carrier")
	private String carrier;

	@Column(name = "tracking_number")
	private String trackingNumber;

	@Column(name = "notes")
	private String notes;

	@Column(name = "received_at")
	private java.sql.Timestamp receivedAt;

	@Column(name = "returned_at")
	private java.sql.Timestamp returnedAt;

	@Column(name = "cancelled_at")
	private java.sql.Timestamp cancelledAt;

	@Version
	private Long version = 0L;

	@OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<GoodsReceiptLineItem> lineItems = new ArrayList<>();

	// Helper methods
	public void addLineItem(GoodsReceiptLineItem lineItem) {
		lineItems.add(lineItem);
		lineItem.setGoodsReceipt(this);
	}

	public void removeLineItem(GoodsReceiptLineItem lineItem) {
		lineItems.remove(lineItem);
		lineItem.setGoodsReceipt(null);
	}
}