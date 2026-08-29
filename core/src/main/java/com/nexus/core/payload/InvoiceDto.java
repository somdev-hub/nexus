package com.nexus.core.payload;

import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvoiceDto {

	private Long invoiceId;

	@NotBlank(message = "Invoice number is required")
	private String invoiceNumber;

	@NotNull(message = "Purchase order is required")
	private Long purchaseOrderId;

	@NotNull(message = "Supplier is required")
	private Long supplierId;

	private com.nexus.core.entities.InvoiceStatus status = com.nexus.core.entities.InvoiceStatus.DRAFT;

	@NotNull(message = "Invoice date is required")
	private Date invoiceDate;

	private Date dueDate;

	private java.math.BigDecimal totalAmount;

	private String currency = "USD";

	private String paymentTerms;

	private String supplierInvoiceNumber;

	private String notes;

	@NotNull(message = "At least one line item is required")
	@Size(min = 1, message = "At least one line item is required")
	private List<InvoiceLineItemDto> lineItems;
}