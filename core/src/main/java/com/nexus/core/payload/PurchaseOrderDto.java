package com.nexus.core.payload;

import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PurchaseOrderDto {

	private Long purchaseOrderId;

	@NotBlank(message = "PO number is required")
	private String poNumber;

	private Integer revisionNumber = 0;

	private Long parentPoId;

	@NotNull(message = "Buyer organization is required")
	private Long buyerOrgId;

	@NotNull(message = "Supplier is required")
	private Long supplierId;

	private Long partnershipId;

	private com.nexus.core.entities.PurchaseOrderStatus status = com.nexus.core.entities.PurchaseOrderStatus.DRAFT;

	private Double totalAmount = 0.0;

	private String currency = "USD";

	@NotBlank(message = "Payment terms are required")
	private String paymentTerms;

	private String incoterms;

	@NotNull(message = "Requested delivery date is required")
	private Date requestedDeliveryDate;

	private Date expectedDeliveryDate;

	private String notes;

	private Boolean isBlanketOrder = false;

	private Date blanketStartDate;

	private Date blanketEndDate;

	private String releaseSchedule;

	@NotNull(message = "At least one line item is required")
	@Size(min = 1, message = "At least one line item is required")
	private List<PurchaseOrderLineItemDto> lineItems;

	// Approval workflow fields
	private String approvedBy;

	private String rejectionReason;

	// Approval workflow tracking fields (FR-RET-002)
	private com.nexus.core.entities.ApprovalLevel approvalLevel;

	private String requiredApproverLevel;

	private String currentApprover;

	private String approvalDelegatedTo;
}