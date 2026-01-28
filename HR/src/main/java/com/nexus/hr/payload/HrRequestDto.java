package com.nexus.hr.payload;

import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class HrRequestDto {
    private Long requestId;

    private HrRequestType requestType;

    private String remarks;

    private HrRequestStatus status;

    private String employeeName;

    private String employeeEmail;

    private Timestamp appliedOn;

    private Timestamp resolvedOn;

    private Date fromDate;

    private Date toDate;

    private Timestamp checkInHours;

    private Timestamp checkOutHours;

    private Boolean halfDay;

    private String resolutionRemarks;
}
