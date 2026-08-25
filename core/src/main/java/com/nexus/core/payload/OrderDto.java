package com.nexus.core.payload;

import java.sql.Date;
import java.sql.Timestamp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderDto {
	@NotBlank(message = "Order number is required")
	private String orderNumber;

	@NotNull(message = "Buyer organization is required")
	private Long buyerOrgId;

	@NotNull(message = "Seller organization is required")
	private Long sellerOrgId;

	private Long materialId;

	private Long productId;

	@NotNull(message = "Quantity is required")
	private Double quantity;

	private Timestamp createdOn;

	private Date promisedDeliveryDate;

	private Date actualDeliveryDate;

	private Long logisticId;

	private com.nexus.core.entities.OrderStatus status;
}