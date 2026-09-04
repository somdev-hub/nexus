package com.nexus.core.dto;

import com.nexus.core.entities.StopStatus;
import com.nexus.core.entities.StopType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStopDto {
	private Long stopId;

	@NotNull(message = "Shipment ID is required")
	private Long shipmentId;

	@NotNull(message = "Sequence number is required")
	private Integer sequenceNumber;

	@NotNull(message = "Stop type is required")
	private StopType stopType;

	private StopStatus status;

	@NotNull(message = "Address is required")
	@Size(max = 500, message = "Address must not exceed 500 characters")
	private String address;

	private String contactName;
	private String contactPhone;
	private String contactEmail;

	private LocalDateTime scheduledArrival;
	private LocalDateTime scheduledDeparture;
	private LocalDateTime actualArrival;
	private LocalDateTime actualDeparture;

	private Double estimatedWeight;
	private Double estimatedVolume;
	private Integer estimatedPackages;
	private Double actualWeight;
	private Double actualVolume;
	private Integer actualPackages;

	private String instructions;
	private String notes;
	private String proofOfDelivery;
	private String proofOfPickup;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Long createdBy;
	private Long updatedBy;
}