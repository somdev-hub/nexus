package com.nexus.core.payload;

import lombok.Data;

@Data
public class MaterialDto {
    private String name;

    private String code;

    private Long org;

    private Long warehouseId;

    private Double pricePerUnit;

    private String unit;

    private Double productionCostPerUnit;

    private Double productionCapacityPerMonth;

    private Double availableQuantity;
}
