package com.nexus.iam.dto.response;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EmployeesAttendanceDto {
    private LocalDate date;
    private Long employeeId;
    private String employeeName;
    private String checkInTime;
    private String checkOutTime;
    private Double totalHoursWorked;
    private String status;
}
