package com.nexus.hr.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.model.enums.LeaveType;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HrResponseDto {
    private Long requestId;

    private HrRequestType requestType;

    private String remarks;

    private HrRequestStatus status;

    private String employeeName;

    private String employeeEmail;

    private Long empId;

    private String role;

    private String department;

    private Timestamp appliedOn;

    private Timestamp resolvedOn;

    private Date fromDate;

    private Date toDate;

    private Timestamp checkInHours;

    private Timestamp checkOutHours;

    private Boolean halfDay;

    private String resolutionRemarks;

    private LeaveType leaveType;

    private Double leaveBalanceUsed;
}
