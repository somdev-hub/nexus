package com.nexus.iam.dto.response;

import com.nexus.iam.dto.CompensationDto;
import com.nexus.iam.entities.Gender;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmployeeProfileResponse {
    private String coverImageUrl;
    private String profileImageUrl;
    private String fullName;
    private String jobTitle;
    private String department;
    private String email;
    private String phone;
    private String address;
    private Date joiningDate;
    private Double annualSalary;
    private Integer age;
    private Gender gender;
    private Long empId;
    private List<PositionsHeld> positionsHeld;
    private List<HrDocuments> hrDocuments;
    private List<AttendanceRecord> attendanceRecords;
    private CompensationDto compensation;
    private List<LeaveRecord> leaveRecords;

    public record LeaveRecord(String leaveType, Double totalLeaves, Double leavesTaken, Double remainingLeaves) {
    }

    public record AttendanceRecord(LocalDateTime date, String status, LocalDateTime checkInTime, LocalDateTime checkOutTime,
                                   Double hoursWorked, Double breakHours, Double overtimeHours) {
    }

    public record HrDocuments(String documentName, String documentUrl, Timestamp uploadedOn, String documentType) {
    }

    public record PositionsHeld(String title, String department, LocalDateTime fromDate, LocalDateTime toDate, Double duration) {
    }
}
