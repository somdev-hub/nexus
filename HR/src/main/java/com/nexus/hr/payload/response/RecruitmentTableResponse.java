package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class RecruitmentTableResponse {
    private Long recruitmentId;
    private String title;
    private String roleName;
    private String departmentName;
    private Long hiringManager;
    private Long totalApplicants;
    private HiringType hiringType;
    private HiringStatus hiringStatus;
    private Timestamp createdAt;
}
