package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = {"positionDocuments", "hrEntity"})
@Table(name = "t_positions", schema = "hr")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionId;

    private String title;

    private String remarks;

    private Boolean isActive;

    private Timestamp effectiveFrom;

    private Timestamp lastEffectiveDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "position")
    @JsonManagedReference("position-hrDocuments")
    private List<HrDocument> positionDocuments = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    @JsonBackReference("hrEntity-positions")
    private HrEntity hrEntity;
}


