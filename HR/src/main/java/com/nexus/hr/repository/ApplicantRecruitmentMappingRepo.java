package com.nexus.hr.repository;

import com.nexus.hr.model.entities.ApplicantRecruitmentMapping;
import com.nexus.hr.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ApplicantRecruitmentMappingRepo extends JpaRepository<ApplicantRecruitmentMapping, Long> {

    @Query("""
                    SELECT CASE WHEN COUNT(arm) > 0 THEN true ELSE false END
                            FROM ApplicantRecruitmentMapping arm
                            WHERE arm.recruitment.recruitmentId = :recruitmentId
                            AND arm.applicant.userId = :userId
            """)
    boolean existsByRecruitmentRecruitmentIdAndApplicantUserId(Long recruitmentId, Long userId);

    @Query("""
            SELECT arm FROM ApplicantRecruitmentMapping arm
                    JOIN FETCH arm.recruitment r
                    WHERE arm.applicant.userId = :userId
            """)
    Page<ApplicantRecruitmentMapping> findByApplicantUserId(Long userId, Pageable pageRequest);

    @Query("""
                    SELECT arm FROM ApplicantRecruitmentMapping arm
                            JOIN FETCH arm.recruitment r
                            WHERE arm.applicant.userId = :userId
                            AND arm.status = :status
            """)
    Page<ApplicantRecruitmentMapping> findByApplicantUserIsAndStatus(Long userId, ApplicationStatus status, Pageable pageable);

    @Query("""
                    SELECT arm FROM ApplicantRecruitmentMapping arm
                            JOIN FETCH arm.recruitment r
                            WHERE arm.applicant.userId = :userId
                            AND arm.recruitment.recruitmentId = :recruitmentId
    """)
    Optional<ApplicantRecruitmentMapping> findByUserIdAndRecruitmentId(Long userId, Long recruitmentId);
}
