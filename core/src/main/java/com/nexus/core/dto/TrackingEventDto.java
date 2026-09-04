package com.nexus.core.dto;

import com.nexus.core.entities.TrackingEventType;
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
public class TrackingEventDto {
	private Long eventId;

	@NotNull(message = "Shipment ID is required")
	private Long shipmentId;

	@NotNull(message = "Event type is required")
	private TrackingEventType eventType;

	@NotNull(message = "Event timestamp is required")
	private LocalDateTime eventTimestamp;

	@Size(max = 500, message = "Location must not exceed 500 characters")
	private String location;

	private Double latitude;
	private Double longitude;

	@Size(max = 1000, message = "Description must not exceed 1000 characters")
	private String description;

	private String recordedBy;
	private String source;

	private Double temperature;
	private Double humidity;
	private Double shockLevel;

	private String gpsAccuracy;
	private String deviceId;

	private LocalDateTime createdAt;
	private Long createdBy;
}