package com.nexus.core.payload;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GoodsReceiptLineItemDto {

	private Long grLineItemId;

	@NotNull(message = "PO line item is required")
	private Long poLineItemId;

	@NotNull(message = "Line number is required")
	private Integer lineNumber;

	@NotNull(message = "Quantity received is required")
	@Positive(message = "Quantity received must be positive")
	private BigDecimal quantityReceived;

	private BigDecimal quantityAccepted;

	private BigDecimal quantityRejected;

	private String unitOfMeasure;

	private String notes;
}