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
@Table(name = "t_hr_applicant_education", schema = "hr")
public class ApplicantEducation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantEducationId;
    
    private String institute;
    private String degree;
    private String city;
    private String state;
    private String country;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @ManyToOne
    @JoinColumn(name = "applicant_id")
    @JsonBackReference("applicant-educations")
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