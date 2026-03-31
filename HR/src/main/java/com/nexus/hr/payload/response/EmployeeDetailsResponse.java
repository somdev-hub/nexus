package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.AttendanceStatus;
import com.nexus.hr.payload.CompensationDto;
import com.nexus.hr.utils.LocalDateTimeSerializer;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmployeeDetailsResponse {
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime joiningDate;
    private Double annualSalary;
    private String jobTitle;
    private String department;
    private List<PositionsHeld> positionsHeld;
    private List<HrDocuments> hrDocuments;
    private List<AttendanceRecord> attendanceRecords;
    private CompensationDto compensation;
    private List<LeaveRecord> leaveRecords;

    public record LeaveRecord(String leaveType, Double totalLeaves, Double leavesTaken, Double remainingLeaves) {
    }

    public record AttendanceRecord(@JsonSerialize(using = LocalDateTimeSerializer.class) LocalDateTime date,
            AttendanceStatus status, Timestamp checkInTime, Timestamp checkOutTime,
            Double hoursWorked, Double breakHours, Double overtimeHours) {
    }

    public record HrDocuments(String documentName, String documentUrl, Timestamp uploadedOn, String documentType) {
    }

    public record PositionsHeld(String title,
            String department,
            Timestamp fromDate,
            Timestamp toDate,
            Double duration) {
    }
}
