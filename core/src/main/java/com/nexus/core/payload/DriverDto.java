package com.nexus.core.payload;

import com.nexus.core.entities.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {
    private Long driverId;
    @NotBlank(message = "Driver code is required")
    private String driverCode;
    @NotBlank(message = "Full name is required")
    private String fullName;
    private String phone;
    private String email;
    @NotBlank(message = "License number is required")
    private String licenseNumber;
    private String licenseClass;
    private Date licenseExpiry;
    private DriverStatus status;
    private String hrEmployeeId;
    private Double onTimeRate;
    private Double safetyScore;
    private Integer totalTrips;
    private String notes;
}
