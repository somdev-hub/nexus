package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;

@Data
public class RecruitmentApplicantViewDto {
    private Long recruitmentId;
    private String title;
    private String shortDescription;
    private String description;
    private Long orgId;
    private String departmentName;
    private Long departmentId;
    private String roleName;
    private LocalDate openingTillDate;
    private String totalCompensation;
    private String location;
    private Integer minYearsOfExperience;
    private Integer maxYearsOfExperience;
    private String orgName;
    private HiringType hiringType;
    private HiringStatus hiringStatus;
    private Timestamp createdAt;
    private Boolean isActive;
}
