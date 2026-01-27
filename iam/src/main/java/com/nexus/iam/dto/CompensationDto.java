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

    private Double total;

    private Double netMonthlyPay;

    private List<BankRecordsDto> bankRecords;
}
