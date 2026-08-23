package com.nexus.hr.payload.response;

import com.nexus.hr.model.enums.InterviewMode;
import com.nexus.hr.model.enums.InterviewStatus;
import com.nexus.hr.model.enums.InterviewType;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduledInterviewResponse {
	private Long recruitmentInterviewId;
	private Long applicantRecruitmentMappingId;
	private Long applicantId;
	private String applicantName;
	private String applicantEmail;
	private Long recruitmentId;
	private String recruitmentTitle;
	private String roleName;
	private String departmentName;
	private InterviewType interviewType;
	private LocalDate interviewDate;
	private LocalTime interviewTime;
	private String interviewDuration;
	private InterviewMode interviewMode;
	private String interviewLocation;
	private String interviewUrl;
	private String interviewerName;
	private String interviewerEmail;
	private InterviewStatus interviewStatus;
	private Boolean isActive;
	private Timestamp createdAt;
	private Timestamp updatedAt;
}