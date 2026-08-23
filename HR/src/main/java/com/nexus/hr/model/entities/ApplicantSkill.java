package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "t_hr_applicant_skills", schema = "hr")
public class ApplicantSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantSkillId;
    private String skillName;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    @JsonBackReference("applicant-skills")
    private Applicant applicant;

    @CreationTimestamp
    private Timestamp createdAt;
    @UpdateTimestamp
    private Timestamp updatedAt;
    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        isActive = Boolean.TRUE;
    }
}