package com.nexus.hr.model.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.nexus.hr.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "t_hr_applicants", schema = "hr")
public class Applicant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long applicantId;

	private String applicantFirstName;
	private String applicantLastName;
	private String applicantEmail;
	private String applicantPhone;
	private String applicantAddress;
	private String applicantCity;
	private String applicantState;
	private String applicantPinCode;
	private String applicantCountry;
	private Integer applicantAge;
	private LocalDate applicantDateOfBirth;
	private Character applicantGender;
	@Column(unique = true)
	private Long userId;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
	@JsonManagedReference("applicant-educations")
	@SQLRestriction("is_active = true")
	@ToString.Exclude
	private List<ApplicantEducation> applicantEducations = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
	@JsonManagedReference("applicant-documents")
	@SQLRestriction("is_active = true")
	@ToString.Exclude
	private List<HrDocument> applicantDocuments = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
	@JsonManagedReference("applicant-experiences")
	@SQLRestriction("is_active = true")
	@ToString.Exclude
	private List<ApplicantExperience> applicantExperiences = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
	@JsonManagedReference("applicant-skills")
	@SQLRestriction("is_active = true")
	@ToString.Exclude
	private List<ApplicantSkill> applicantSkills = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
	@JsonManagedReference("applicant-recruitment-mappings")
	@SQLRestriction("is_active = true")
	@ToString.Exclude
	private List<ApplicantRecruitmentMapping> applicantRecruitmentMappings = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "applicant")
	@JsonManagedReference("applicant-bookmark-recruitments")
	@SQLRestriction("is_active = true")
	@ToString.Exclude
	private List<ApplicantBookmarkRecruitment> applicantBookmarkRecruitments = new ArrayList<>();

	@CreationTimestamp
	private Timestamp createdOn;
	@UpdateTimestamp
	private Timestamp updatedOn;
	private Boolean isActive;

	@PrePersist
	public void prePersist() {
		isActive = Boolean.TRUE;
	}
}