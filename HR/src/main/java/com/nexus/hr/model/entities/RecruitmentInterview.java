package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.nexus.hr.model.enums.InterviewMode;
import com.nexus.hr.model.enums.InterviewStatus;
import com.nexus.hr.model.enums.InterviewType;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "t_recruitment_interview", schema = "hr")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false)
public class RecruitmentInterview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long recruitmentInterviewId;

	@Enumerated(EnumType.STRING)
	private InterviewType interviewType;

	private LocalDate interviewDate;

	private LocalTime interviewTime;

	private String interviewDuration;

	@Enumerated(EnumType.STRING)
	private InterviewMode interviewMode;

	private String interviewLocation;

	private String interviewUrl;

	private String interviewerName;

	private String interviewConfirmationLink;

	private LocalDateTime interviewConfirmationDeadline;

	@Column(columnDefinition = "TEXT")
	private String interviewerRemarks;

	@Enumerated(EnumType.STRING)
	private InterviewStatus interviewStatus;

	@ManyToOne
	@JoinColumn(name = "applicant_recruitment_mapping_id")
	@JsonBackReference("applicantRecruitmentMapping-interviews")
	private ApplicantRecruitmentMapping applicantRecruitmentMapping;

	@ManyToOne
	@JoinColumn(name = "interviewer_hr_id")
	@JsonBackReference("hrEntity-interviews")
	private HrEntity interviewer;

	@CreationTimestamp
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	private Boolean isActive;

	@PrePersist
	protected void onCreate() {
		if (isActive == null) {
			isActive = true;
		}
	}
}
