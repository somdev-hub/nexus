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
public class ProofOfDeliveryDto {
    private Long podId;
    @NotNull(message = "Shipment id is required")
    private Long shipmentId;
    private String receivedBy;
    private String signature;
    private String photoUrls;
    private Timestamp deliveredAt;
    private Double latitude;
    private Double longitude;
    private String dmsDocumentId;
    private String conditionNotes;
    private String notes;
}
