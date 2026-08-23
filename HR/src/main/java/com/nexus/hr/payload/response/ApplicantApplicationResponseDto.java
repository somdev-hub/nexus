package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.ApplicationStatus;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ApplicantApplicationResponseDto {
    private Long userId;
    private Long applicantId;
    private Long recruitmentId;
    private String roleName;
    private String orgName;
    private Timestamp appliedOn;
    private String location;
    private ApplicationStatus status;
}
