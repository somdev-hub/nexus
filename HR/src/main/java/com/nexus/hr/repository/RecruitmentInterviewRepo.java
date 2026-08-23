package com.nexus.hr.repository;

import com.nexus.hr.model.entities.RecruitmentInterview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecruitmentInterviewRepo extends JpaRepository<RecruitmentInterview, Long> {

	List<RecruitmentInterview> findByApplicantRecruitmentMapping_ApplicantRecruitmentMappingId(Long mappingId);

	List<RecruitmentInterview> findByInterviewer_HrId(Long hrId);

	@Query("""
			SELECT ri FROM RecruitmentInterview ri
			JOIN ri.applicantRecruitmentMapping arm
			JOIN arm.recruitment r
			WHERE r.orgId = :orgId
			AND ri.isActive = true
			AND (:interviewType IS NULL OR ri.interviewType = :interviewType)
			AND (:interviewMode IS NULL OR ri.interviewMode = :interviewMode)
			AND (:startDate IS NULL OR ri.interviewDate >= :startDate)
			AND (:endDate IS NULL OR ri.interviewDate <= :endDate)
			ORDER BY ri.interviewDate ASC, ri.interviewTime ASC
			""")
	Page<RecruitmentInterview> findAllScheduledInterviewsByOrg(
			@Param("orgId") Long orgId,
			@Param("interviewType") String interviewType,
			@Param("interviewMode") String interviewMode,
			@Param("startDate") String startDate,
			@Param("endDate") String endDate,
			Pageable pageable);

	@Query("""
			SELECT ri FROM RecruitmentInterview ri
			JOIN ri.applicantRecruitmentMapping arm
			JOIN arm.recruitment r
			JOIN ri.interviewer i
			WHERE r.orgId = :orgId
			AND i.employeeEmail = :interviewerEmail
			AND ri.isActive = true
			AND (:interviewType IS NULL OR ri.interviewType = :interviewType)
			AND (:interviewMode IS NULL OR ri.interviewMode = :interviewMode)
			AND (:startDate IS NULL OR ri.interviewDate >= :startDate)
			AND (:endDate IS NULL OR ri.interviewDate <= :endDate)
			ORDER BY ri.interviewDate ASC, ri.interviewTime ASC
			""")
	Page<RecruitmentInterview> findMyInterviewsByOrgAndInterviewer(
			@Param("orgId") Long orgId,
			@Param("interviewerEmail") String interviewerEmail,
			@Param("interviewType") String interviewType,
			@Param("interviewMode") String interviewMode,
			@Param("startDate") String startDate,
			@Param("endDate") String endDate,
			Pageable pageable);
}