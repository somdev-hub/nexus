package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    @OneToMany(cascade = CascadeType.ALL)
    private List<Bonus> bonuses;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Deduction> deductions;

    private Double netPay;

    private Double gratuity;

    private Double pf;

    private String annualPackage;

    private Double total;

    private Double netMonthlyPay;
}
