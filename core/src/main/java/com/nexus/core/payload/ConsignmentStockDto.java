package com.nexus.core.payload;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsignmentStockDto {

    private Long consignmentId;

    @NotNull(message = "Retailer org ID is required")
    private Long retailerOrgId;

    private String retailerOrgName;

    private Long warehouseId;

    private String warehouseCode;

    @NotNull(message = "Material ID is required")
    private Long materialId;

    private String materialName;

    private Double quantityOnHand;

    private Double quantityReserved;

    private Double quantityAvailable;

    private String consignmentNumber;

    private String notes;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
