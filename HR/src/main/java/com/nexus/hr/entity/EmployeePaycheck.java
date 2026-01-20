package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "t_employee_paychecks", schema = "hr")
public class EmployeePaycheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeePaycheckId;

    private Long orgId;

    private String employeePosition;

    @Enumerated(EnumType.STRING)
    private EmployeeLevel employeeLevel;

    private Double minBasePay;

    private Double maxBasePay;

    private Double minHra;

    private Double maxHra;

    private Double minTotalBonuses;

    private Double maxTotalBonuses;

    private Double minTotalDeductions;

    private Double maxTotalDeductions;

    private Double minNetPay;

    private Double maxNetPay;

    private Double minGrossPay;

    private Double maxGrossPay;

    private String minAnnualSalary;

    private String maxAnnualSalary;
}
