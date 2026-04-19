package com.nexus.hr.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nexus.hr.model.enums.HrRequestType;
import com.nexus.hr.model.enums.LeaveType;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class HrRequestDto {
    private Long empId;
    private HrRequestType hrRequestType;
    private String remarks;
    private Date fromDate;
    private Date toDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Timestamp checkInHours;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Timestamp checkOutHours;

    private Boolean halfDay;
    private LeaveType leaveType;
}