package com.nexus.hr.payload;

import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class BulkRegularizationRequestDto {

    private Long hrId;

    private Date date;

    private Timestamp checkInHours;

    private Timestamp checkOutHours;

    private Boolean halfDay;

    private String reason;

}
