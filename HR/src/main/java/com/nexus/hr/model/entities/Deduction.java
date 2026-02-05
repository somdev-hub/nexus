package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"compensation"})
@Table(name = "t_deductions", schema = "hr")
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
    @JsonBackReference("compensation-deductions")
    private Compensation compensation;

    private Timestamp issuedOn;

    private Timestamp updatedOn;

    private Timestamp expiresOn;
}