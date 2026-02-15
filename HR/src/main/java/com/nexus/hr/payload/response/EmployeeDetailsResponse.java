package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.AttendanceStatus;
import com.nexus.hr.payload.CompensationDto;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmployeeDetailsResponse {
    private Date joiningDate;
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

    public record AttendanceRecord(Date date, AttendanceStatus status, Timestamp checkInTime, Timestamp checkOutTime,
                                   Double hoursWorked, Double breakHours, Double overtimeHours) {
    }

    public record HrDocuments(String documentName, String documentUrl, Timestamp uploadedOn, String documentType) {
    }


    public record PositionsHeld
            (String title,
             String department,
             Timestamp fromDate,
             Timestamp toDate,
             Double duration
            ) {
    }
}
