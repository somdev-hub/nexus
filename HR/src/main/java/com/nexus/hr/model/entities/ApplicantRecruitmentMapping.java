package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Data
@Table(name = "t_hr_applicant_recruitment_mapping", schema = "hr")
@Entity
public class ApplicantRecruitmentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantRecruitmentMappingId;

    @ManyToOne
    @JoinColumn(name = "applicant_applicant_id")
    @JsonBackReference("applicant-recruitment-mappings")
    private Applicant applicant;

    @ManyToOne
    @JoinColumn(name = "recruitment_recruitment_id")
    @JsonBackReference("recruitment-applicantRecruitmentMappings")
    private Recruitment recruitment;

    @CreationTimestamp
    private Timestamp appliedOn;

    @UpdateTimestamp
    private Timestamp updatedOn;

    private Boolean isActive;

    @OneToMany(orphanRemoval = true, mappedBy = "applicantRecruitmentMapping", cascade = CascadeType.ALL)
    @JsonManagedReference("applicantRecruitmentMapping-applicationDocuments")
    private List<HrDocument> applicationDocuments;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }
}
