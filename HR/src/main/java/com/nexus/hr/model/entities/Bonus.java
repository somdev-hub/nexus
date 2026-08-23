package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;


@Getter
@Setter
@EqualsAndHashCode(exclude = {"compensation"})
@Entity
@Table(name = "t_bonus", schema = "hr")
public class Bonus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bonusId;

    private String bonusType;

    private Double amount;

    private Double percentageOfSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_id")
    @JsonBackReference("compensation-bonuses")
    private Compensation compensation;

    private Timestamp issuedOn;

    private Timestamp updatedOn;

    private Timestamp expiresOn;
}