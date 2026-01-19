package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "t_hr_payrolls", schema = "hr")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payrollId;

    private String month;

    private Integer year;

    private Double basePay;

    private Double hra;

    private Double totalBonuses;

    private Double totalDeductions;

    private Double pf;

    private Double others;

    private Double netPay;

    private Double grossPay;

    private Timestamp paidOn;

    @OneToOne
    @JoinColumn(name = "salary_slip_hr_document_id")
    private HrDocument salarySlip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_id")
    private Compensation compensation;
}