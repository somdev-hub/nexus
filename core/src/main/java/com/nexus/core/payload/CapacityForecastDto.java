package com.nexus.core.payload;

import com.nexus.core.model.enums.CapacityUnit;
import com.nexus.core.model.enums.FleetAssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityForecastDto {
    private Long forecastId;
    private String originLane;
    private String destinationLane;
    private FleetAssetType equipmentType;
    private Date periodStart;
    private Date periodEnd;
    private Double availableCapacity;
    private Double bookedCapacity;
    private CapacityUnit capacityUnit;
    private Double unitPrice;
    private String currency;
    private Long partnershipId;
    // Per-unit specs, required for unitized units (pallets/containers).
    private Double unitLength;
    private Double unitWidth;
    private Double unitHeight;
    private String dimensionUom;
    private Double unitVolume;
    private String volumeUom;
    private String notes;
}
