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

    private Long materialId;

    private Long catalogId;

    private String catalogName;

    private Long variantId;

    private String variantValue;

    private Long priceTierId;

    private String priceTierName;

    private String materialName;

    private Double minLevel;

    private Double maxLevel;

    private Double reorderPoint;

    private Double reorderQuantity;

    private Boolean autoReplenish;

    private Timestamp lastReplenishedAt;

    private Long lastShipmentId;

    private Boolean isActive;

    private Long supplierOrgId;

    private String supplierOrgName;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
