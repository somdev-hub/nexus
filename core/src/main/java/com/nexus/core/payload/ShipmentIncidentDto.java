package com.nexus.core.payload;

import com.nexus.core.model.enums.IncidentStatus;
import com.nexus.core.model.enums.IncidentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentIncidentDto {
    private Long incidentId;
    private String incidentNumber;
    @NotNull(message = "Shipment id is required")
    private Long shipmentId;
    @NotNull(message = "Incident type is required")
    private IncidentType incidentType;
    private IncidentStatus status;
    private String description;
    private Timestamp reportedAt;
    private Timestamp resolvedAt;
    private BigDecimal claimAmount;
    private String dmsDocumentId;
    private String notes;
}
