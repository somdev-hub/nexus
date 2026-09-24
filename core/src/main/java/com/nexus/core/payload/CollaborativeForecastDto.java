package com.nexus.core.payload;

import com.nexus.core.entities.CollaborativeForecast;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaborativeForecastDto {

    private Long forecastId;

    private Long retailerOrgId;

    private String retailerOrgName;

    private Long catalogId;

    private String catalogName;

    @NotNull(message = "Period start is required")
    private Date periodStart;

    @NotNull(message = "Period end is required")
    private Date periodEnd;

    @NotNull(message = "Forecast quantity is required")
    private Double forecastQuantity;

    private Double confidencePct;

    private CollaborativeForecast.ForecastStatus status;

    private String notes;

    private String createdBy;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
