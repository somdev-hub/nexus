package com.nexus.core.payload;

import com.nexus.core.entities.MaintenanceStatus;
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
public class MaintenanceRecordDto {
    private Long maintenanceId;
    private String maintenanceNumber;
    @NotNull(message = "Asset id is required")
    private Long assetId;
    @NotBlank(message = "Maintenance type is required")
    private String maintenanceType;
    private String description;
    private MaintenanceStatus status;
    private Date scheduledDate;
    private Date completedDate;
    private Double odometerReading;
    private BigDecimal cost;
    private String serviceProvider;
    private Boolean isBreakdown;
    private String notes;
}
