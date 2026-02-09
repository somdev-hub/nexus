package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.LeaveStatus;
import com.nexus.hr.model.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_employee_leaves", schema = "hr")
public class EmployeeLeaves {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long employeeLeaveId;

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    private HrEntity hrEntity;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private Date startDate;

    private Date endDate;

    private Integer numberOfDays;

    @Enumerated(EnumType.STRING)
    private LeaveStatus leaveStatus;

    private String reason;

    private Timestamp appliedDate;

    private Timestamp approvedOrRevokedDate;

    private Long approvedBy;
}
