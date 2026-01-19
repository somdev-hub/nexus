package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "t_hr_positions", schema = "hr")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionId;

    private String title;

    private String remarks;

    private Boolean isActive;

    private Timestamp effectiveFrom;

    private Timestamp lastEffectiveDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "hr_position_id")
    private List<HrDocument> positionDocuments = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    private HrEntity hrEntity;
}
