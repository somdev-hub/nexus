package com.nexus.core.payload;

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
public class ProductionCapacityDto {

    private Long capacityId;

    private String productLine;

    @NotNull(message = "Period start is required")
    private Date periodStart;

    @NotNull(message = "Period end is required")
    private Date periodEnd;

    private String shift;

    private Double availableCapacity;

    private Double allocatedCapacity;

    private Double remainingCapacity;

    private String unit;

    private String notes;

    private Long supplierOrgId;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
