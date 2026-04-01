package com.nexus.iam.dto.response;

import java.sql.Timestamp;
import java.time.LocalDate;

import lombok.Data;

@Data
public class EmployeesAttendanceDto {
    private LocalDate date;
    private Long employeeId;
    private String employeeName;
    private Timestamp checkInTime;
    private Timestamp checkOutTime;
    private Double totalHoursWorked;
    private String status;
}
