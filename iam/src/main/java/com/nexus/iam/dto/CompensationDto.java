package com.nexus.iam.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompensationDto {
    private Double basePay;

    private Double hra;

    private List<BonusDto> bonuses;

    private List<DeductionDto> deductions;

    private Double netPay;

    private Double gratuity;

    private Double pf;

    private String annualPackage;

    private Double insurancePremium;

    private Double grossPay;

    private List<BankRecordsDto> bankRecords;
}
