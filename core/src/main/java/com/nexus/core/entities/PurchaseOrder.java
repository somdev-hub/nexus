package com.nexus.core.entities;

import java.sql.Date;
import java.sql.Timestamp;
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
@Table(name = "t_purchase_orders", schema = "core")
public class PurchaseOrder extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "purchase_order_id")
	private Long purchaseOrderId;

	@Column(name = "po_number", unique = true)
	private String poNumber;

	@Column(name = "revision_number")
	private Integer revisionNumber = 0;

	@Column(name = "parent_po_id")
	private Long parentPoId; // For versioning/amendments

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "buyer_org_id", referencedColumnName = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account buyerOrg;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Supplier supplier;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partnership_id", referencedColumnName = "partnership_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Partnership partnership;

	@Enumerated(EnumType.STRING)
	private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

	@Column(name = "total_amount")
	private Double totalAmount = 0.0;

	private String currency = "USD";

	private String paymentTerms;

	private String incoterms;

	@Column(name = "requested_delivery_date")
	private Date requestedDeliveryDate;

	@Column(name = "expected_delivery_date")
	private Date expectedDeliveryDate;

	@Column(name = "approved_at")
	private Timestamp approvedAt;

	@Column(name = "approved_by")
	private String approvedBy;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	// Approval workflow fields (FR-RET-002)
	@Enumerated(EnumType.STRING)
	@Column(name = "approval_level")
	private ApprovalLevel approvalLevel;

	@Column(name = "required_approver_level")
	private String requiredApproverLevel; // e.g., "MANAGER", "DIRECTOR"

	@Column(name = "current_approver")
	private String currentApprover;

	@Column(name = "approval_delegated_to")
	private String approvalDelegatedTo;

	@Column(name = "sent_to_supplier_at")
	private Timestamp sentToSupplierAt;

	@Column(name = "acknowledged_at")
	private Timestamp acknowledgedAt;

	@Column(name = "notes")
	private String notes;

	@Column(name = "is_blanket_order")
	private Boolean isBlanketOrder = false;

	@Column(name = "blanket_start_date")
	private Date blanketStartDate;

	@Column(name = "blanket_end_date")
	private Date blanketEndDate;

	@Column(name = "release_schedule")
	private String releaseSchedule; // JSON or cron expression

	@Version
	private Long version = 0L;

	@OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<PurchaseOrderLineItem> lineItems = new ArrayList<>();

	// Helper methods
	public void addLineItem(PurchaseOrderLineItem lineItem) {
		lineItems.add(lineItem);
		lineItem.setPurchaseOrder(this);
		recalculateTotal();
	}

	public void removeLineItem(PurchaseOrderLineItem lineItem) {
		lineItems.remove(lineItem);
		lineItem.setPurchaseOrder(null);
		recalculateTotal();
	}

	public void recalculateTotal() {
		this.totalAmount = lineItems.stream()
				.mapToDouble(item -> item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
				.sum();
	}

	public boolean isAmendment() {
		return parentPoId != null;
	}

	public boolean isBlanketOrder() {
		return Boolean.TRUE.equals(isBlanketOrder);
	}
}