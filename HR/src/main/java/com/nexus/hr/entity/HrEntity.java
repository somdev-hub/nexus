package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hr_entity_hr_id")
    private List<HrDocument> hrDocuments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hr_entity_hr_id")
    private List<TimeManagement> timeManagements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hr_entity_hr_id")
    private List<Position> positions = new ArrayList<>();
}
