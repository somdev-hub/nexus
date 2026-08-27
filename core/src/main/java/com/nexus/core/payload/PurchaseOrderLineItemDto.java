package com.nexus.core.payload;

import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PurchaseOrderLineItemDto {

	private Long lineItemId;

	@NotNull(message = "Line number is required")
	private Integer lineNumber;

	private Long materialId;

	private Long productId;

	@NotBlank(message = "Description is required")
	private String description;

	@NotNull(message = "Quantity ordered is required")
	@Positive(message = "Quantity must be positive")
	private Double quantityOrdered;

	private Double quantityReceived = 0.0;

	private Double quantityInvoiced = 0.0;

	@NotNull(message = "Unit price is required")
	@Positive(message = "Unit price must be positive")
	private Double unitPrice;

	private Double totalPrice;

	private String unitOfMeasure;

	private String incoterms;

	private String deliveryLocation;
}