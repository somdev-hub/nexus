package com.nexus.hr.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
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
    private HrEntity hrEntity;

    @ManyToOne
    @JoinColumn(name = "hr_position_id")
    private Position position;

    @ManyToOne
    @JoinColumn(name = "compensation_id")
    private Compensation compensation;
}
