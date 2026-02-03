package com.nexus.hr.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "t_compensations", schema = "hr")
public class Compensation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long compensationId;

    private Double basePay;

    private Double hra;

    private Double gratuity;

    private Double pf;

    private Double insurancePremium;

    private Double netPay;

    private Double grossPay;

    private String annualPackage;

    @OneToOne(mappedBy = "compensation")
    private HrEntity hrEntity;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compensation")
    private List<Bonus> bonuses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compensation")
    private List<Deduction> deductions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation")
    private List<HrDocument> compensationCard = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation")
    private List<Payroll> payrolls = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation")
    private List<BankRecord> bankRecords = new ArrayList<>();
}
