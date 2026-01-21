package com.nexus.hr.payload;

import com.nexus.hr.model.entities.Compensation;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class BonusDto {
    private Long bonusId;

    private String bonusType;

    private Double amount;

    private Double percentageOfSalary;

    private Compensation compensation;

    private Timestamp issuedOn;

    private Timestamp updatedOn;

    private Timestamp expiresOn;
}
