package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"salarySlip", "compensation"})
@Entity
@Table(name = "t_payrolls", schema = "hr")
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
    @JsonBackReference("payroll-salarySlip")
    private HrDocument salarySlip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_id")
    @JsonBackReference("compensation-payrolls")
    private Compensation compensation;
}