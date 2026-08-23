package com.nexus.hr.payload.response;

import java.sql.Timestamp;
import java.time.LocalDate;

import lombok.Data;

@Data
public class AttendanceResponse {
    private LocalDate date;
    private Long employeeId;
    private Timestamp checkInTime;
    private Timestamp checkOutTime;
    private Double totalHoursWorked;
    private String status;
}
