package com.nexus.hr.payload;

import lombok.Data;

import java.util.Map;

@Data
public class PayComponentDto {

    private Double basePay;
    private Double hra;
    private Map<String, Double> bonuses; // e.g., {"performanceBonus": 5000.0, "festivalBonus": 2000.0}
    private Map<String, Double> deductions; // e.g., {"tax": 3000.0, "loanRepayment": 1500.0}
}
