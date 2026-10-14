package com.nexus.core.payload;

import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvoiceDto {

	private Long invoiceId;

	// Auto-generated server-side when blank (INV-<year>-<6 digits>).
	private String invoiceNumber;

	@NotNull(message = "Purchase order is required")
	private Long purchaseOrderId;

	// Read alias for poNumber (UI table column).
	private String poNumber;

	// Optional on the way in: the supplier flow derives it from the PO.
	// The retailer flow validates it explicitly in the service.
	private Long supplierId;

	private com.nexus.core.model.enums.InvoiceStatus status = com.nexus.core.model.enums.InvoiceStatus.DRAFT;

	@NotNull(message = "Invoice date is required")
	private Date invoiceDate;

	private Date dueDate;

	private java.math.BigDecimal totalAmount;

	private java.math.BigDecimal taxAmount;

	private java.math.BigDecimal otherCharges;

	private String otherChargesBreakdown;

	private String currency = "USD";

	private String paymentTerms;

	private String supplierInvoiceNumber;

	private String notes;

	@NotNull(message = "At least one line item is required")
	@Size(min = 1, message = "At least one line item is required")
	private List<InvoiceLineItemDto> lineItems;
}