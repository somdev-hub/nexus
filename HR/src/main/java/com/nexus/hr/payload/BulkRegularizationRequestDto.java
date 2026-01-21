package com.nexus.hr.payload;

import lombok.Data;

import java.sql.Date;

@Data
public class BulkRegularizationRequestDto {

    private Long hrId;

    private Date date;

    private Double checkInHours;

    private Double checkOutHours;

    private Boolean halfDay;

    private String reason;

}
