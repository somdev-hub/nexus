package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_employee_leave_allocations", schema = "hr")
public class EmployeeLeaveAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    private HrEntity hrEntity;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private Double allocatedDays;

    private Double usedDays;

    private Double remainingDays;

    private Integer year;

    private Timestamp allocationDate;

    private Timestamp updationDate;

    private Timestamp lastUsedDate;

    private Boolean isActive;

    private Boolean isCarryForwardable;
}
