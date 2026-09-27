package com.nexus.core.payload;

import com.nexus.core.entities.FleetAssetStatus;
import com.nexus.core.entities.FleetAssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetAssetDto {
    private Long assetId;
    @NotBlank(message = "Asset number is required")
    private String assetNumber;
    @NotNull(message = "Asset type is required")
    private FleetAssetType assetType;
    private String make;
    private String model;
    private Integer manufactureYear;
    private String licensePlate;
    private String vin;
    private Double capacityWeight;
    private Double capacityVolume;
    private FleetAssetStatus status;
    private Double currentMileage;
    private Double currentHours;
    private Date lastMaintenanceDate;
    private Double nextMaintenanceDueMileage;
    private Date nextMaintenanceDueDate;
    private Date insuranceExpiry;
    private Date permitExpiry;
    private String dmsDocumentId;
    private String notes;
    private Double currentLatitude;
    private Double currentLongitude;
}
