package com.nexus.core.payload;

import com.nexus.core.entities.DeliveryAppointmentStatus;
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
public class DeliveryAppointmentDto {

    private Long appointmentId;

    private String appointmentNumber;

    private Long shipmentId;

    private String shipmentNumber;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    private String warehouseCode;

    private DeliveryAppointmentStatus status;

    @NotNull(message = "Scheduled start is required")
    private Timestamp scheduledStart;

    @NotNull(message = "Scheduled end is required")
    private Timestamp scheduledEnd;

    private Timestamp actualArrival;
    private Timestamp actualDeparture;

    private String dockNumber;
    private String contactName;
    private String contactPhone;
    private String specialInstructions;
    private String notes;

    private Timestamp createdAt;
    private Timestamp updatedAt;
}
