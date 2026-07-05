package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "t_hr_recruitments", schema = "hr")
public class Recruitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recruitmentId;

    @NotNull
    private String title;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    private Long orgId;

    @NotNull
    private String departmentName;

    @NotNull
    private Long departmentId;

    @NotNull
    private String roleName;

    @NotNull
    private LocalDate openingTillDate;

    @NotNull
    private String totalCompensation;

    private String location;

    private Integer minYearsOfExperience;

    private Integer maxYearsOfExperience;

    private String orgName;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "recruitment", orphanRemoval = true)
    @JsonManagedReference("recruitment-applicantRecruitmentMappings")
    private List<ApplicantRecruitmentMapping> applicantRecruitmentMappings= new ArrayList<>();

    private Long totalApplicants;

    @ManyToOne
    @JoinColumn(name = "created_by_hr_id")
    @JsonBackReference("hrEntity-recruitments")
    private HrEntity createdBy;

    @Enumerated(EnumType.STRING)
    private HiringType hiringType;

    @Enumerated(EnumType.STRING)
    private HiringStatus hiringStatus;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    private Boolean isActive;

    @PrePersist
    public void prePersist() {
        isActive = true;
    }
}
