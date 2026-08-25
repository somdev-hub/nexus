package com.nexus.core.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WarehouseDto {
	@NotBlank(message = "Warehouse code is required")
	private String code;

	private Long warehouseManager;

	@NotNull(message = "Organization is required")
	private Long org;

	@NotBlank(message = "Location is required")
	private String location;

	@NotNull(message = "Storage capacity is required")
	private Double storageCapacity;

	private Double currentUtilization;
}