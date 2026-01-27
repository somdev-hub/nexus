package com.nexus.hr.model.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
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

    private Date dateOfJoining;

    private Date dateOfLeaving;

    private Boolean isActive;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "hr_compensation_id")
    private Compensation compensation;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
    private List<HrDocument> hrDocuments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
    private List<TimeManagement> timeManagements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
    private List<Position> positions = new ArrayList<>();
}
