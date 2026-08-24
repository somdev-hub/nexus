package com.nexus.core.payload;

import lombok.Data;

@Data
public class WarehouseDto {
	private String code;
	private Long warehouseManager;
	private Long org;
	private String location;
	private Double storageCapacity;
	private Double currentUtilization;
}