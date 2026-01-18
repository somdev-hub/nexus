package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class HrEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hrId;

    private Long employeeId;

    private String department;

    private Long org;

    @OneToOne
    @JoinColumn(name = "hr_compensation_id")
    private Compensation compensation;
}
