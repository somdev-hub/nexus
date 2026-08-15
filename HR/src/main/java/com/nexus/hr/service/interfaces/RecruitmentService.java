package com.nexus.hr.service.interfaces;

import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.ApplicationStatus;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import com.nexus.hr.payload.ApplicantApplication;
import com.nexus.hr.payload.ApplicantStatusUpdateRequest;
import com.nexus.hr.payload.response.CompanyInsightDto;
import com.nexus.hr.payload.response.DashboardStatsDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RecruitmentService {
	ResponseEntity<?> createRecruitment(Recruitment recruitment, Long empId);

	ResponseEntity<?> getRecruitment(Long id);

	ResponseEntity<?> getAllRecruitments(Long orgId, Boolean isActive, Long empId, HiringType hiringType,
			HiringStatus hiringStatus, Pageable pageRequest);

	ResponseEntity<?> updateRecruitment(@Valid Recruitment recruitment, Long empId);

	ResponseEntity<?> getClosedRecruitments(Long orgId, Pageable of);

	ResponseEntity<?> getRecruitmentAnalytics(Long orgId);

	ResponseEntity<?> getOpeningsToday(Integer pageNo, Integer pageOffset, HiringStatus status, String orgName,
			String location, String query);

	ResponseEntity<?> getOpeningsBeforeToday(Integer pageNo, Integer pageOffset, HiringStatus status, String orgName,
			String location, String query);

	ResponseEntity<?> getPositionPieGraph();

	ResponseEntity<?> getExperienceWiseOpenings();

	ResponseEntity<?> getCompanyWiseOpeningCount(Integer pageNo, Integer pageOffset);

	ResponseEntity<?> getRecruitmentApplicantView(Long id);

	ResponseEntity<?> getRecruitmentFilters();

	ResponseEntity<?> getRecruitmentByName(String name, Integer pageNo, Integer pageOffset);

	ResponseEntity<?> applyApplicantRecruitment(ApplicantApplication application);

	ResponseEntity<?> hasApplicantApplied(Long recruitmentId, Long userId);

	ResponseEntity<?> getApplicantApplications(Long userId, Integer pageNo, Integer pageOffset,
			ApplicationStatus status);

	ResponseEntity<?> getApplicantApplicationWithStatus(Long userId, Long recruitmentId);

	// New dashboard APIs
	ResponseEntity<?> getCompanyInsights();

	ResponseEntity<?> getDashboardStats();

	// Applicant recruitment mapping APIs
	ResponseEntity<?> getApplicantByRecruitmentMapping(Long applicantId, Long recruitmentId);

	ResponseEntity<?> updateApplicantRecruitmentStatus(Long applicantId, Long recruitmentId,
			ApplicantStatusUpdateRequest request);
}
