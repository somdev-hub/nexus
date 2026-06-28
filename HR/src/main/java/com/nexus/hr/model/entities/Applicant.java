package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "t_hr_applicants", schema = "hr")
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantId;

    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantAddress;
    private String applicantCity;
    private String applicantState;
    private String applicantPinCode;
    private String applicantCountry;
    private Integer applicantAge;
    private LocalDate applicantDateOfBirth;
    private Character applicantGender;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
    @JsonManagedReference("applicant-educations")
    private List<ApplicantEducation> applicantEducations = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
    @JsonManagedReference("applicant-documents")
    private List<HrDocument> applicantDocuments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
    @JsonManagedReference("applicant-experiences")
    private List<ApplicantExperience> applicantExperiences = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
    @JsonManagedReference("applicant-skills")
    private List<ApplicantSkill> applicantSkills = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
    @JsonManagedReference("applicant-recruitment-mappings")
    private List<ApplicantRecruitmentMapping> applicantRecruitmentMappings = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdOn;
    @UpdateTimestamp
    private Timestamp updatedOn;
    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        isActive = Boolean.TRUE;
    }
}