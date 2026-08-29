package com.nexus.core.payload;

import java.sql.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoodsReceiptDto {

	private Long goodsReceiptId;

	@NotBlank(message = "GR number is required")
	private String grNumber;

	@NotNull(message = "Purchase order is required")
	private Long purchaseOrderId;

	@NotNull(message = "Supplier is required")
	private Long supplierId;

	private com.nexus.core.entities.GoodsReceiptStatus status = com.nexus.core.entities.GoodsReceiptStatus.DRAFT;

	@NotNull(message = "Received date is required")
	private Date receivedDate;

	private String deliveryNoteNumber;

	private String carrier;

	private String trackingNumber;

	private String notes;

	@NotNull(message = "At least one line item is required")
	@Size(min = 1, message = "At least one line item is required")
	private List<GoodsReceiptLineItemDto> lineItems;
}