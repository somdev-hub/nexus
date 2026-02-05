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
@EqualsAndHashCode(exclude = {"hrEntity", "position", "compensation"})
@Table(name = "t_hr_documents", schema = "hr")
public class HrDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long hrDocumentId;

    private String documentName;

    private String hrDocumentType;

    private String documentUrl;

    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());

    @ManyToOne
    @JoinColumn(name = "hr_entity_hr_id")
    @JsonBackReference("hrEntity-documents")
    private HrEntity hrEntity;

    @ManyToOne
    @JoinColumn(name = "hr_position_id")
    @JsonBackReference("position-hrDocuments")
    private Position position;

    @ManyToOne
    @JoinColumn(name = "compensation_id")
    @JsonBackReference("compensation-compensationCard")
    private Compensation compensation;
}
