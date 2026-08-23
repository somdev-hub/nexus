package com.nexus.hr.payload;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DeductionDto {
    private Long deductionId;

    private String deductionType;

    private Double amount;

    private String description;

    private Double percentageOfSalary;

    private Timestamp issuedOn;

    private Timestamp updatedOn;

    private Timestamp expiresOn;
}
