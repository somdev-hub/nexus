package com.nexus.core.payload;

import java.sql.Date;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class OrderDto {
	private String orderNumber;
	private Long buyerOrgId;
	private Long sellerOrgId;
	private Long materialId;
	private Long productId;
	private Double quantity;
	private Timestamp createdOn;
	private Date promisedDeliveryDate;
	private Date actualDeliveryDate;
	private Long logisticId;
	private com.nexus.core.entities.OrderStatus status;
}