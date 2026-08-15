package com.nexus.hr.model.entities;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@EqualsAndHashCode(exclude = { "compensation", "hrDocuments", "timeManagements", "positions", "leaveAllocations",
		"employeeLeaves", "interviews" })
@ToString(exclude = { "compensation", "hrDocuments", "timeManagements", "positions", "leaveAllocations",
		"employeeLeaves", "interviews" })
@Table(name = "t_hr_entity", schema = "hr")
public class HrEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long hrId;

	private Long employeeId;

	private String employeeName;

	private String employeeEmail;

	private String employeePersonalEmail;

	private String department;

	private Long org;

	private String orgName;

	private Date dateOfJoining;

	private Date dateOfLeaving;

	private Boolean isActive;

	private Boolean onNoticePeriod = Boolean.FALSE;

	private Date noticePeriodStartDate;

	private Date noticePeriodEndDate;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "hr_compensation_id")
	@JsonManagedReference("hrEntity-compensation")
	private Compensation compensation;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
	@JsonManagedReference("hrEntity-documents")
	private List<HrDocument> hrDocuments = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
	@JsonManagedReference("hrEntity-timeManagements")
	private List<TimeManagement> timeManagements = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
	@JsonManagedReference("hrEntity-positions")
	private List<Position> positions = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
	@JsonManagedReference("hrEntity-leaveAllocations")
	private List<EmployeeLeaveAllocation> leaveAllocations = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "hrEntity")
	@JsonManagedReference("hrEntity-employeeLeaves")
	private List<EmployeeLeaves> employeeLeaves = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "createdBy")
	@JsonManagedReference("hrEntity-recruitments")
	private List<Recruitment> recruitments = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "interviewer")
	@JsonManagedReference("hrEntity-interviews")
	private List<RecruitmentInterview> interviews = new ArrayList<>();

	@CreationTimestamp
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	@PrePersist
	protected void onCreate() {
		if (isActive == null) {
			isActive = true;
		}
	}
}
