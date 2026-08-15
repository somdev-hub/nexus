package com.nexus.hr.repository;

import com.nexus.hr.model.entities.RecruitmentInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecruitmentInterviewRepo extends JpaRepository<RecruitmentInterview, Long> {

	List<RecruitmentInterview> findByApplicantRecruitmentMapping_ApplicantRecruitmentMappingId(Long mappingId);

	List<RecruitmentInterview> findByInterviewer_HrId(Long hrId);
}