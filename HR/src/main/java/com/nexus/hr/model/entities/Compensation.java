package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"hrEntity", "bonuses", "deductions", "compensationCard", "payrolls", "bankRecords"})
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
    @JsonBackReference("hrEntity-compensation")
    private HrEntity hrEntity;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compensation")
    @JsonManagedReference("compensation-bonuses")
    private List<Bonus> bonuses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compensation")
    @JsonManagedReference("compensation-deductions")
    private List<Deduction> deductions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation")
    @JsonManagedReference("compensation-compensationCard")
    private List<HrDocument> compensationCard = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation")
    @JsonManagedReference("compensation-payrolls")
    private List<Payroll> payrolls = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation")
    @JsonManagedReference("compensation-bankRecords")
    private List<BankRecord> bankRecords = new ArrayList<>();
}
