package com.nexus.core.payload;

import com.nexus.core.model.enums.FleetAssetType;
import com.nexus.core.model.enums.RateType;
import com.nexus.core.model.enums.ShipmentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreightRateDto {
    private Long rateId;
    @NotBlank(message = "Rate code is required")
    private String rateCode;
    @NotNull(message = "Rate type is required")
    private RateType rateType;
    private String originLane;
    private String destinationLane;
    private FleetAssetType equipmentType;
    private ShipmentMode shipmentMode;
    private BigDecimal baseRate;
    private String fuelSurchargeFormula;
    private BigDecimal fuelSurchargePct;
    private String accessorialTable;
    private String currency;
    private Date effectiveFrom;
    private Date effectiveTo;
    private Boolean isActiveRate;
    private String notes;
}
