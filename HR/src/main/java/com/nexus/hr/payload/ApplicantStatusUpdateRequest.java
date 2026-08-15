package com.nexus.hr.payload;

import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.model.enums.InterviewMode;
import com.nexus.hr.model.enums.InterviewType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ApplicantStatusUpdateRequest {

	private ApplicationStatus status;

	// Interview details (required when status is INTERVIEW_SCHEDULED)
	private InterviewType interviewType;
	private LocalDate interviewDate;
	private LocalDateTime interviewTime;
	private String interviewDuration;
	private InterviewMode interviewMode;
	private String interviewerEmail;
}