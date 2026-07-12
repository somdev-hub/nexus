package com.nexus.hr.repository;

import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ApplicantRepo extends JpaRepository<Applicant, Long>, JpaSpecificationExecutor<Applicant> {

    // ===== QUERIES WITH RECRUITMENT ID FILTER =====

    // Base query with recruitment ID only
    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId")
    Page<Applicant> findByRecruitment_RecruitmentId(Long recruitmentId, Pageable pageable);

    // Single filter + recruitment ID
    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status")
    Page<Applicant> findByRecruitment_RecruitmentIdAndApplicationStatus(@Param("recruitmentId") Long recruitmentId,
                                                                        @Param("status") ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Applicant> findByRecruitment_RecruitmentIdAndNameContaining(@Param("recruitmentId") Long recruitmentId,
                                                                     @Param("name") String name, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender")
    Page<Applicant> findByRecruitment_RecruitmentIdAndApplicantGender(@Param("recruitmentId") Long recruitmentId,
                                                                      @Param("gender") Character gender, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRange(@Param("recruitmentId") Long recruitmentId,
                                                               @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId,
                                                                          @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    // @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b
    // ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId =
    // :recruitmentId AND a.applicantExperiences.yearsOfExperience >=
    // :yearsOfExperience")
    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId,
                                                                        @Param("yearsOfExperience") Double yearsOfExperience, Pageable pageable);

    // Two filter combinations + recruitment ID
    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndName(@Param("recruitmentId") Long recruitmentId,
                                                                    @Param("status") ApplicationStatus status, @Param("name") String name, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND a.applicantGender = :gender")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGender(@Param("recruitmentId") Long recruitmentId,
                                                                      @Param("status") ApplicationStatus status, @Param("gender") Character gender, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndAgeRange(@Param("recruitmentId") Long recruitmentId,
                                                                        @Param("status") ApplicationStatus status, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge,
                                                                        Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndAppliedBetweenDates(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND b.status = :status
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndYearsOfExperience(
            @Param("recruitmentId") Long recruitmentId,
            @Param("status") ApplicationStatus status,
            @Param("yearsOfExperience") Double yearsOfExperience,
            Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantGender = :gender")
    Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndGender(@Param("recruitmentId") Long recruitmentId,
                                                                    @Param("name") String name, @Param("gender") Character gender, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndAgeRange(@Param("recruitmentId") Long recruitmentId,
                                                                      @Param("name") String name, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge,
                                                                      Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndAppliedBetweenDates(
            @Param("recruitmentId") Long recruitmentId, @Param("name") String name,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND b.status = :status")
    Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndStatus(@Param("recruitmentId") Long recruitmentId,
                                                                    @Param("name") String name, @Param("status") ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndAgeRange(@Param("recruitmentId") Long recruitmentId,
                                                                        @Param("gender") Character gender, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge,
                                                                        Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndAppliedBetweenDates(
            @Param("recruitmentId") Long recruitmentId, @Param("gender") Character gender,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND b.status = :status")
    Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndStatus(@Param("recruitmentId") Long recruitmentId,
                                                                      @Param("gender") Character gender, @Param("status") ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRangeAndAppliedBetweenDates(
            @Param("recruitmentId") Long recruitmentId, @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge AND b.status = :status")
    Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRangeAndStatus(@Param("recruitmentId") Long recruitmentId,
                                                                        @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, @Param("status") ApplicationStatus status,
                                                                        Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate AND b.status = :status")
    Page<Applicant> findByRecruitment_RecruitmentIdAndAppliedBetweenDatesAndStatus(
            @Param("recruitmentId") Long recruitmentId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("status") ApplicationStatus status, Pageable pageable);

    // Three filter combinations + recruitment ID
    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantGender = :gender")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndGender(@Param("recruitmentId") Long recruitmentId,
                                                                             @Param("status") ApplicationStatus status, @Param("name") String name, @Param("gender") Character gender,
                                                                             Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndAgeRange(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("name") String name, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge,
            Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndAppliedBetweenDates(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("name") String name, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b
    // ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId =
    // :recruitmentId AND b.status = :status AND (LOWER(a.applicantFirstName) LIKE
    // LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE
    // LOWER(CONCAT('%', :name, '%'))) AND a.yearsOfExperience >=
    // :yearsOfExperience")
    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND b.status = :status
                AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndYearsOfExperience(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("name") String name, @Param("yearsOfExperience") Double yearsOfExperience, Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND a.applicantGender = :gender AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGenderAndAgeRange(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("gender") Character gender, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge,
            Pageable pageable);

    @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND b.status = :status AND a.applicantGender = :gender AND DATE(b.appliedOn) >= :startDate AND DATE(b.appliedOn) <= :endDate")
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGenderAndAppliedBetweenDates(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("gender") Character gender, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);

    // @Query("SELECT DISTINCT a FROM Applicant a JOIN ApplicantRecruitmentMapping b
    // ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId =
    // :recruitmentId AND b.status = :status AND a.applicantGender = :gender AND
    // b.yearsOfExperience >= :yearsOfExperience")
    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND b.status = :status
                AND a.applicantGender = :gender
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGenderAndYearsOfExperience(
            @Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status,
            @Param("gender") Character gender, @Param("yearsOfExperience") Double yearsOfExperience,
            Pageable pageable);

    // ===== CUSTOM QUERIES FOR ANALYTICS =====

    // Count applications by status
    @Query("SELECT COUNT(a) FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.orgId = :orgId AND b.status = :status")
    Long countApplicationsByStatus(@Param("orgId") Long orgId, @Param("status") ApplicationStatus status);

    // Count total applications for organization
    @Query("SELECT COUNT(a) FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.orgId = :orgId")
    Long countTotalApplications(@Param("orgId") Long orgId);

    // Count applications from previous week
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.applied_on::date >= (CURRENT_DATE - INTERVAL '1 week') AND a.applied_on::date < CURRENT_DATE", nativeQuery = true)
    Long countApplicationsPreviousWeek(@Param("orgId") Long orgId);

    // Count applications from previous month
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND EXTRACT(MONTH FROM a.applied_on) = EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '1 month'))", nativeQuery = true)
    Long countApplicationsPreviousMonth(@Param("orgId") Long orgId);

    // Count applications from previous quarter
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
    Long countApplicationsPreviousQuarter(@Param("orgId") Long orgId);

    // Average time to hire (in days) - from applied to offer accepted
    @Query(value = "SELECT AVG(EXTRACT(DAY FROM (a.updated_on - a.applied_on))) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.status = 'OFFER_ACCEPTED'", nativeQuery = true)
    Double averageTimeToHire(@Param("orgId") Long orgId);

    // Average time to hire from previous quarter
    @Query(value = "SELECT AVG(EXTRACT(DAY FROM (a.updated_on - a.applied_on))) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.status = 'OFFER_ACCEPTED' AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
    Double averageTimeToHirePreviousQuarter(@Param("orgId") Long orgId);

    // Offer acceptance rate - count of offer accepted
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.status = 'OFFER_ACCEPTED'", nativeQuery = true)
    Long countOfferAccepted(@Param("orgId") Long orgId);

    // Count selected candidates
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.status = 'SELECTED'", nativeQuery = true)
    Long countSelected(@Param("orgId") Long orgId);

    // Count offer accepted from previous quarter
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.status = 'OFFER_ACCEPTED' AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
    Long countOfferAcceptedPreviousQuarter(@Param("orgId") Long orgId);

    // Count selected from previous quarter
    @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicant_recruitment_mapping a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.status = 'SELECTED' AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
    Long countSelectedPreviousQuarter(@Param("orgId") Long orgId);

    // @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND e.yearsOfExperience >= :yearsOfExperience")
    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndYearsOfExperience(Long recruitmentId, String name,
                                                                               Double yearsOfExperience, Pageable pageRequest);

    // @Query("SELECT a FROM Applicant a JOIN ApplicantRecruitmentMapping b ON a.applicantId=b.applicant.applicantId WHERE b.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND e.yearsOfExperience >= :yearsOfExperience")
    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND a.applicantGender = :gender
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndYearsOfExperience(Long recruitmentId, Character gender,
                                                                                 Double yearsOfExperience, Pageable pageRequest);

    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND a.applicantAge >= :minAge
                AND a.applicantAge <= :maxAge
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRangeAndYearsOfExperience(Long recruitmentId, Integer minAge,
                                                                                   Integer maxAge, Double yearsOfExperience, Pageable pageRequest);

    @Query("""
            SELECT DISTINCT a FROM Applicant a
                JOIN ApplicantRecruitmentMapping b ON a.applicantId = b.applicant.applicantId
                JOIN a.applicantExperiences e
                WHERE b.recruitment.recruitmentId = :recruitmentId
                AND DATE(b.appliedOn) >= :startDate
                AND DATE(b.appliedOn) <= :endDate
                AND e.yearsOfExperience >= :yearsOfExperience
            """)
    Page<Applicant> findByRecruitment_RecruitmentIdAndAppliedBetweenDatesAndYearsOfExperience(Long recruitmentId,
                                                                                              LocalDate appliedFromDate, LocalDate appliedToDate, Double yearsOfExperience, Pageable pageRequest);

    Optional<Applicant> findByUserId(Long userId);

    @Query("""
            SELECT e FROM ApplicantEducation e
                WHERE e.applicant.applicantId = :applicantId
                AND e.isActive = true
    """)
    List<ApplicantEducation> findApplicantEducationByApplicant_ApplicantIdAndIsActiveTrue(Long applicantId);
    @Query("""
            SELECT e FROM ApplicantExperience e
                WHERE e.applicant.applicantId = :applicantId
                AND e.isActive = true
    """)
    List<ApplicantExperience> findApplicantExperienceByApplicant_ApplicantIdAndIsActiveTrue(Long applicantId);
    @Query("""
            SELECT e FROM ApplicantSkill e
                WHERE e.applicant.applicantId = :applicantId
                AND e.isActive = true
    """)
    List<ApplicantSkill> findApplicantSkillByApplicant_ApplicantIdAndIsActiveTrue(Long applicantId);

    @Query("""
            SELECT d FROM HrDocument d
                WHERE d.applicant.applicantId = :applicantId
                AND d.isActive = true
    """)
    List<HrDocument> findApplicantDocumentsByApplicant_ApplicantIdAndIsActiveTrue(Long applicantId);
}
