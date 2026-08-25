package com.nexus.core.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaterialDto {
	@NotBlank(message = "Material name is required")
	private String name;

	@NotBlank(message = "Material code is required")
	private String code;

	@NotNull(message = "Organization is required")
	private Long org;

	private Long warehouseId;

	private Double pricePerUnit;

	private String unit;

	private Double productionCostPerUnit;

	private Double productionCapacityPerMonth;

	private Double availableQuantity;
}
