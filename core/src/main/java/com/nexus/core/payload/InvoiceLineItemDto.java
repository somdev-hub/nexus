package com.nexus.core.payload;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InvoiceLineItemDto {

	private Long invoiceLineItemId;

	@NotNull(message = "PO line item is required")
	private Long poLineItemId;

	private Long grLineItemId;

	@NotNull(message = "Line number is required")
	private Integer lineNumber;

	@NotNull(message = "Quantity invoiced is required")
	@Positive(message = "Quantity invoiced must be positive")
	private BigDecimal quantityInvoiced;

	@NotNull(message = "Unit price is required")
	@Positive(message = "Unit price must be positive")
	private BigDecimal unitPrice;

	private BigDecimal lineTotal;

	private String unitOfMeasure;

	private String notes;
}