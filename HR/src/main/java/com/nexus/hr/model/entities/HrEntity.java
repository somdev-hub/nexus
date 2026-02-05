package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"compensation", "hrDocuments", "timeManagements", "positions"})
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
    @JsonManagedReference("hrEntity-compensation")
    private Compensation compensation;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
    @JsonManagedReference("hrEntity-documents")
    private List<HrDocument> hrDocuments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
    @JsonManagedReference("hrEntity-timeManagements")
    private List<TimeManagement> timeManagements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
    @JsonManagedReference("hrEntity-positions")
    private List<Position> positions = new ArrayList<>();
}
