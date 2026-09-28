package com.nexus.core.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDriverHistoryDto {
    private Long driverId;
    private String driverName;
    private Integer trips;
    private Timestamp firstTripAt;
    private Timestamp lastTripAt;
}
