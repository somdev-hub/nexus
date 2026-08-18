package com.nexus.hr.payload.response;

import com.nexus.hr.model.entities.ApplicantRecruitmentMappingStatusHist;
import com.nexus.hr.model.entities.RecruitmentInterview;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class ApplicationWithStatusResponseDto {

    private Long applicantRecruitmentMappingId;
    private Long recruitmentId;
    private String roleName;
    private String orgName;
    private String location;
    private Timestamp appliedOn;
    private String resumeSubmitted;
    private List<ApplicantRecruitmentMappingStatusHist> statusHistList;
    private List<RecruitmentInterview> interviews;
}
