package com.nexus.core.entities;

import java.math.BigDecimal;
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
@Table(name = "t_invoices", schema = "core")
public class Invoice extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "invoice_id")
	private Long invoiceId;

	@Column(name = "invoice_number", unique = true)
	private String invoiceNumber;

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
	private InvoiceStatus status = InvoiceStatus.DRAFT;

	@Column(name = "invoice_date")
	private Date invoiceDate;

	@Column(name = "due_date")
	private Date dueDate;

	@Column(name = "total_amount", precision = 19, scale = 4)
	private java.math.BigDecimal totalAmount;

	@Column(name = "currency")
	private String currency = "USD";

	@Column(name = "payment_terms")
	private String paymentTerms;

	@Column(name = "supplier_invoice_number")
	private String supplierInvoiceNumber;

	@Column(name = "notes")
	private String notes;

	@Column(name = "submitted_at")
	private java.sql.Timestamp submittedAt;

	@Column(name = "approved_at")
	private java.sql.Timestamp approvedAt;

	@Column(name = "approved_by")
	private String approvedBy;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "paid_at")
	private java.sql.Timestamp paidAt;

	@Column(name = "closed_at")
	private java.sql.Timestamp closedAt;

	@Column(name = "cancelled_at")
	private java.sql.Timestamp cancelledAt;

	@Version
	private Long version = 0L;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<InvoiceLineItem> lineItems = new ArrayList<>();

	// Helper methods
	public void addLineItem(InvoiceLineItem lineItem) {
		lineItems.add(lineItem);
		lineItem.setInvoice(this);
		recalculateTotal();
	}

	public void removeLineItem(InvoiceLineItem lineItem) {
		lineItems.remove(lineItem);
		lineItem.setInvoice(null);
		recalculateTotal();
	}

	public void recalculateTotal() {
		this.totalAmount = lineItems.stream()
				.map(item -> item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}