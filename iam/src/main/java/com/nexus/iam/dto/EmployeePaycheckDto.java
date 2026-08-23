package com.nexus.iam.dto;

import lombok.Data;

@Data
public class EmployeePaycheckDto {

    private Long orgId;

    private String role;

    private Long deptId;

    private Double minBasePay;

    private Double maxBasePay;

    private Double minTotalBonuses;

    private Double maxTotalBonuses;

    private Double minTotalDeductions;

    private Double maxTotalDeductions;

    private String minAnnualSalary;

    private String maxAnnualSalary;
}
