package com.nexus.hr.entity;

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

    @OneToOne(mappedBy = "compensation")
    private HrEntity hrEntity;

    private Double basePay;

    private Double hra;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compensation_id")
    private List<Bonus> bonuses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compensation_id")
    private List<Deduction> deductions = new ArrayList<>();

    private Double netPay;

    private Double gratuity;

    private Double pf;

    private String annualPackage;

    private Double total;

    private Double netMonthlyPay;

    @OneToOne
    @JoinColumn(name = "compensation_card_hr_document_id")
    private HrDocument compensationCard;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "compensation_id")
    private List<Payroll> payrolls = new ArrayList<>();
}
