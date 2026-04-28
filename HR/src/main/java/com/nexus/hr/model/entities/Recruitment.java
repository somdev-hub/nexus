package com.nexus.hr.model.entities;

import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.sql.Timestamp;
import java.time.LocalDate;

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

    @ManyToOne
    @JoinColumn(name = "created_by_hr_id")
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
