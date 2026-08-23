package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Data
@Table(name = "t_hr_applicant_bookmark_recruitment", schema = "hr")
@Entity
public class ApplicantBookmarkRecruitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicantBookmarkRecruitmentId;

    @ManyToOne
    @JoinColumn(name = "applicant_applicant_id", nullable = false)
    @JsonBackReference("applicant-bookmark-recruitments")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Applicant applicant;

    @ManyToOne
    @JoinColumn(name = "recruitment_recruitment_id", nullable = false)
    @JsonBackReference("recruitment-applicantBookmarkRecruitments")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Recruitment recruitment;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        isActive = true;
    }
}