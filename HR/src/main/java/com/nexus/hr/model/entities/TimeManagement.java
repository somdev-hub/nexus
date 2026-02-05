package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"hrEntity"})
@Table(name = "t_time_management", schema = "hr")
public class TimeManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long timeManagementId;

    private Timestamp createdOn;

    private Integer month;

    private Integer day;

    private Integer year;

    private Timestamp checkInTime;

    private Timestamp checkOutTime;

    private Timestamp breakStartTime;

    private Timestamp breakEndTime;

    private Double totalHoursWorked;

    private Double effectiveHours;

    private Double overtimeHours;

    private Boolean isPresent;

    private Boolean isOnLeave;

    private Boolean isHalfDay;

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    @JsonBackReference("hrEntity-timeManagements")
    private HrEntity hrEntity;
}


