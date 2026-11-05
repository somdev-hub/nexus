package com.nexus.core.payload;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GoodsReceiptLineItemDto {

	private Long grLineItemId;

	private Long poLineItemId;

	private Long materialId;

	private String description;

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