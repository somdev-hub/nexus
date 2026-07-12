package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "t_hr_applicant_recruitment_mapping_status_hist", schema = "hr")
public class ApplicantRecruitmentMappingStatusHist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantRecruitmentMappingStatusHistId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @CreationTimestamp
    private Timestamp createdAt;

    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "applicant_recruitment_mapping_id")
    private ApplicantRecruitmentMapping applicantRecruitmentMapping;
}
