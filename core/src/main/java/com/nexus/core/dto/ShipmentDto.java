package com.nexus.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDto {
	private Long shipmentId;

	@NotNull(message = "Shipment number is required")
	@Size(max = 50, message = "Shipment number must not exceed 50 characters")
	private String shipmentNumber;

	@NotNull(message = "Supplier ID is required")
	private Long supplierId;

	private String supplierName;

	@NotNull(message = "Warehouse ID is required")
	private Long warehouseId;

	private String warehouseName;

	@NotNull(message = "Shipment mode is required")
	private ShipmentMode mode;

	private ShipmentStatus status;

	private String description;

	@NotNull(message = "Pickup address is required")
	@Size(max = 500, message = "Pickup address must not exceed 500 characters")
	private String pickupAddress;

	@NotNull(message = "Delivery address is required")
	@Size(max = 500, message = "Delivery address must not exceed 500 characters")
	private String deliveryAddress;

	private String pickupContactName;
	private String pickupContactPhone;
	private String pickupContactEmail;

	private String deliveryContactName;
	private String deliveryContactPhone;
	private String deliveryContactEmail;

	private LocalDateTime scheduledPickupDate;
	private LocalDateTime scheduledDeliveryDate;
	private LocalDateTime actualPickupDate;
	private LocalDateTime actualDeliveryDate;

	private BigDecimal freightCost;
	private Currency freightCurrency;
	private BigDecimal actualFreightCost;
	private Currency actualFreightCurrency;

	private String carrierName;
	private String carrierTrackingNumber;
	private String carrierReference;

	private String specialInstructions;
	private String hazardousMaterialsInfo;
	private String customsInformation;

	private Double totalWeight;
	private Double totalVolume;
	private Integer totalPackages;

	private String externalReference;
	private String purchaseOrderNumber;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Long createdBy;
	private Long updatedBy;
	private Long organizationId;

	private List<ShipmentStopDto> stops;
	private List<TrackingEventDto> trackingEvents;
	private List<ShipmentDocumentDto> documents;
}