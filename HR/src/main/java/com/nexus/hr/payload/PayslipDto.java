package com.nexus.hr.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for payslip generation containing all required data
 * Used to generate payslip PDF through DMS service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayslipDto {

    // Employee Information
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private String position;
    private String department;
    private String organizationName;
    private String organizationAddress;

    // Payroll Information
    private Long payrollId;
    private String month;
    private Integer year;
    private LocalDateTime generatedDate;

    // Payment Details
    private Double basePay;
    private Double hra;
    private Double totalBonuses;
    private Double totalDeductions;
    private Double grossPay;
    private Double netPay;

    // Bonus Details (key: bonus type, value: amount)
    private Map<String, Double> bonuses;

    // Deduction Details (key: deduction type, value: amount)
    private Map<String, Double> deductions;

    // Bank Information (masked for security)
    private String bankName;
    private String accountHolderName;
    private String maskedAccountNumber;
    private String ifscCode;

    // Payment Reference
    private String paymentReferenceId;
    private String transactionReference;

    // Additional Details
    private String remarks;
    private Integer attendanceDays;
    private Integer totalWorkingDays;
}

