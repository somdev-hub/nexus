package com.nexus.hr.repository;

import com.nexus.hr.model.entities.ApplicantBookmarkRecruitment;
import com.nexus.hr.model.entities.Recruitment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicantBookmarkRecruitmentRepo extends JpaRepository<ApplicantBookmarkRecruitment, Long> {

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.applicant.userId = :userId AND abr.recruitment.recruitmentId = :recruitmentId")
    Optional<ApplicantBookmarkRecruitment> findByApplicantUserIdAndRecruitmentRecruitmentId(@Param("userId") Long userId, @Param("recruitmentId") Long recruitmentId);

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.applicant.userId = :userId AND abr.recruitment.recruitmentId = :recruitmentId AND abr.isActive = true")
    Optional<ApplicantBookmarkRecruitment> findActiveByApplicantUserIdAndRecruitmentRecruitmentId(@Param("userId") Long userId, @Param("recruitmentId") Long recruitmentId);

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.applicant.userId = :userId AND abr.isActive = true")
    List<ApplicantBookmarkRecruitment> findByApplicantUserIdAndIsActiveTrue(@Param("userId") Long userId);

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.applicant.userId = :userId AND abr.recruitment.recruitmentId = :recruitmentId")
    Optional<ApplicantBookmarkRecruitment> findByApplicantUserIdAndRecruitmentRecruitmentIdAndIsActiveTrue(@Param("userId") Long userId, @Param("recruitmentId") Long recruitmentId);

    @Query("SELECT COUNT(abr) FROM ApplicantBookmarkRecruitment abr WHERE abr.recruitment.recruitmentId = :recruitmentId AND abr.isActive = true")
    Long countByRecruitmentRecruitmentIdAndIsActiveTrue(@Param("recruitmentId") Long recruitmentId);

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.applicant.userId = :userId AND abr.isActive = true")
    Page<ApplicantBookmarkRecruitment> findByApplicantUserIdAndIsActiveTrue(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.recruitment.recruitmentId = :recruitmentId AND abr.isActive = true")
    Page<ApplicantBookmarkRecruitment> findByRecruitmentRecruitmentIdAndIsActiveTrue(@Param("recruitmentId") Long recruitmentId, Pageable pageable);

    @Query("SELECT abr FROM ApplicantBookmarkRecruitment abr WHERE abr.applicant.userId = :userId")
    Optional<ApplicantBookmarkRecruitment> findByApplicantUserId(@Param("userId") Long userId);
}