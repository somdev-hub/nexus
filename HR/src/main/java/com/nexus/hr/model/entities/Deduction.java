package com.nexus.hr.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Table(name = "t_deductions", schema = "hr")
@Data
public class Deduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deductionId;

    private String deductionType;

    private Double amount;

    private String description;

    private Double percentageOfSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_id")
    private Compensation compensation;

    private Timestamp issuedOn;

    private Timestamp updatedOn;

    private Timestamp expiresOn;
}