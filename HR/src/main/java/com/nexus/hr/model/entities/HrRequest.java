package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.model.enums.LeaveType;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_hr_requests", schema = "hr")
public class HrRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    private HrRequestType requestType;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    private HrRequestStatus status;

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    @JsonIgnore
    private HrEntity appliedBy;

    private Timestamp appliedOn;

    private Timestamp resolvedOn;

    private Date fromDate;

    private Date toDate;

    private Timestamp checkInHours;

    private Timestamp checkOutHours;

    private Boolean halfDay;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private Double leaveBalanceUsed;

    @Column(columnDefinition = "TEXT")
    private String resolutionRemarks;
}
