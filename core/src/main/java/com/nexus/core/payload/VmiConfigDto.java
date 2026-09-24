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
public class VmiConfigDto {

    private Long vmiId;

    @NotNull(message = "Retailer org ID is required")
    private Long retailerOrgId;

    private String retailerOrgName;

    private Long warehouseId;

    private String warehouseCode;

    @NotNull(message = "Material ID is required")
    private Long materialId;

    private String materialName;

    private Double minLevel;

    private Double maxLevel;

    private Double reorderPoint;

    private Double reorderQuantity;

    private Boolean autoReplenish;

    private Timestamp lastReplenishedAt;

    private Boolean isActive;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
