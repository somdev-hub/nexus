package com.nexus.core.payload;

import com.nexus.core.model.enums.ShipmentStatus;
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
public class AssetShipmentDto {
    private Long shipmentId;
    private String shipmentNumber;
    private ShipmentStatus status;
    private Date pickupDate;
    private Date deliveryDate;
    private Timestamp actualDeparture;
    private Timestamp actualArrival;
    private Long driverId;
    private String driverName;
    private Double freightCost;
}
