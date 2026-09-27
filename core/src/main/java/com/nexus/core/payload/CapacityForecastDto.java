package com.nexus.core.payload;

import com.nexus.core.entities.FleetAssetType;
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
    private String notes;
}
