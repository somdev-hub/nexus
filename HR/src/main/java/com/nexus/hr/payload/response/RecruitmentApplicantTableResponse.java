package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.HiringStatus;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class RecruitmentApplicantTableResponse {
    private Long recruitmentId;
    private String roleName;
    private String orgName;
    private String location;
    private Timestamp createdAt;
    private HiringStatus status;
    private String department;
}
