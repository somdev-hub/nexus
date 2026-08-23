package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "t_hr_applicant_experience", schema = "hr")
public class ApplicantExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantExperienceId;

    private String previousCompany;
    private String jobTitle;
    private Double yearsOfExperience;
    private String jobDescription;
    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    @JsonBackReference("applicant-experiences")
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