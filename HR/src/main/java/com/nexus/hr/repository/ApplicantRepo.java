package com.nexus.hr.repository;

import com.nexus.hr.model.entities.Applicant;
import com.nexus.hr.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ApplicantRepo extends JpaRepository<Applicant, Long>, JpaSpecificationExecutor<Applicant> {

    // Query to find applicants by status
    Page<Applicant> findByApplicationStatus(ApplicationStatus status, Pageable pageable);

    // Query to find applicants by name (first or last name)
    @Query("SELECT a FROM Applicant a WHERE LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Applicant> findByNameContaining(@Param("name") String name, Pageable pageable);

    // Query to find applicants by gender
    Page<Applicant> findByApplicantGender(Character gender, Pageable pageable);

    // Query to find applicants by age
    Page<Applicant> findByApplicantAge(Integer age, Pageable pageable);

    // Query to find applicants by age range
    @Query("SELECT a FROM Applicant a WHERE a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    // Query to find applicants applied between dates
    @Query("SELECT a FROM Applicant a WHERE DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findAppliedBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    // Query to find applicants by years of experience
    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByYearsOfExperience(@Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    // Combined queries for multiple filters
    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Applicant> findByStatusAndName(@Param("status") ApplicationStatus status, @Param("name") String name, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND a.applicantGender = :gender")
    Page<Applicant> findByStatusAndGender(@Param("status") ApplicationStatus status, @Param("gender") Character gender, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByStatusAndAgeRange(@Param("status") ApplicationStatus status, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findByStatusAndAppliedBetweenDates(@Param("status") ApplicationStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.applicationStatus = :status AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByStatusAndYearsOfExperience(@Param("status") ApplicationStatus status, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantGender = :gender")
    Page<Applicant> findByNameAndGender(@Param("name") String name, @Param("gender") Character gender, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByNameAndAgeRange(@Param("name") String name, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findByNameAndAppliedBetweenDates(@Param("name") String name, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByNameAndYearsOfExperience(@Param("name") String name, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicantGender = :gender AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByGenderAndAgeRange(@Param("gender") Character gender, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicantGender = :gender AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findByGenderAndAppliedBetweenDates(@Param("gender") Character gender, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.applicantGender = :gender AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByGenderAndYearsOfExperience(@Param("gender") Character gender, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicantAge >= :minAge AND a.applicantAge <= :maxAge AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findByAgeRangeAndAppliedBetweenDates(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.applicantAge >= :minAge AND a.applicantAge <= :maxAge AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByAgeRangeAndYearsOfExperience(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByAppliedBetweenDatesAndYearsOfExperience(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    // Three filter combinations
    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantGender = :gender")
    Page<Applicant> findByStatusAndNameAndGender(@Param("status") ApplicationStatus status, @Param("name") String name, @Param("gender") Character gender, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByStatusAndNameAndAgeRange(@Param("status") ApplicationStatus status, @Param("name") String name, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findByStatusAndNameAndAppliedBetweenDates(@Param("status") ApplicationStatus status, @Param("name") String name, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByStatusAndNameAndYearsOfExperience(@Param("status") ApplicationStatus status, @Param("name") String name, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND a.applicantGender = :gender AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
    Page<Applicant> findByStatusAndGenderAndAgeRange(@Param("status") ApplicationStatus status, @Param("gender") Character gender, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

    @Query("SELECT a FROM Applicant a WHERE a.applicationStatus = :status AND a.applicantGender = :gender AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
    Page<Applicant> findByStatusAndGenderAndAppliedBetweenDates(@Param("status") ApplicationStatus status, @Param("gender") Character gender, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.applicationStatus = :status AND a.applicantGender = :gender AND exp.yearsOfExperience >= :yearsOfExperience")
    Page<Applicant> findByStatusAndGenderAndYearsOfExperience(@Param("status") ApplicationStatus status, @Param("gender") Character gender, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     // Complex 4+ filter combinations using Specification (handled in service)

     // ===== QUERIES WITH RECRUITMENT ID FILTER =====

     // Base query with recruitment ID only
     Page<Applicant> findByRecruitment_RecruitmentId(Long recruitmentId, Pageable pageable);

     // Single filter + recruitment ID
     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status")
     Page<Applicant> findByRecruitment_RecruitmentIdAndApplicationStatus(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))")
     Page<Applicant> findByRecruitment_RecruitmentIdAndNameContaining(@Param("recruitmentId") Long recruitmentId, @Param("name") String name, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender")
     Page<Applicant> findByRecruitment_RecruitmentIdAndApplicantGender(@Param("recruitmentId") Long recruitmentId, @Param("gender") Character gender, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
     Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRange(@Param("recruitmentId") Long recruitmentId, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     // Two filter combinations + recruitment ID
     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%')))")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndName(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("name") String name, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND a.applicantGender = :gender")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGender(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("gender") Character gender, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndAgeRange(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantGender = :gender")
     Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndGender(@Param("recruitmentId") Long recruitmentId, @Param("name") String name, @Param("gender") Character gender, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
     Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndAgeRange(@Param("recruitmentId") Long recruitmentId, @Param("name") String name, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("name") String name, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndNameAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("name") String name, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
     Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndAgeRange(@Param("recruitmentId") Long recruitmentId, @Param("gender") Character gender, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("gender") Character gender, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantGender = :gender AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndGenderAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("gender") Character gender, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRangeAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndAgeRangeAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndAppliedBetweenDatesAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     // Three filter combinations + recruitment ID
     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantGender = :gender")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndGender(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("name") String name, @Param("gender") Character gender, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndAgeRange(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("name") String name, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("name") String name, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

     @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND (LOWER(a.applicantFirstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.applicantLastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND exp.yearsOfExperience >= :yearsOfExperience")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndNameAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("name") String name, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND a.applicantGender = :gender AND a.applicantAge >= :minAge AND a.applicantAge <= :maxAge")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGenderAndAgeRange(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("gender") Character gender, @Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge, Pageable pageable);

     @Query("SELECT a FROM Applicant a WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND a.applicantGender = :gender AND DATE(a.appliedOn) >= :startDate AND DATE(a.appliedOn) <= :endDate")
     Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGenderAndAppliedBetweenDates(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("gender") Character gender, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

      @Query("SELECT DISTINCT a FROM Applicant a JOIN a.applicantExperiences exp WHERE a.recruitment.recruitmentId = :recruitmentId AND a.applicationStatus = :status AND a.applicantGender = :gender AND exp.yearsOfExperience >= :yearsOfExperience")
      Page<Applicant> findByRecruitment_RecruitmentIdAndStatusAndGenderAndYearsOfExperience(@Param("recruitmentId") Long recruitmentId, @Param("status") ApplicationStatus status, @Param("gender") Character gender, @Param("yearsOfExperience") Integer yearsOfExperience, Pageable pageable);

     // ===== CUSTOM QUERIES FOR ANALYTICS =====

     // Count applications by status
     @Query("SELECT COUNT(a) FROM Applicant a WHERE a.recruitment.orgId = :orgId AND a.applicationStatus = :status")
     Long countApplicationsByStatus(@Param("orgId") Long orgId, @Param("status") ApplicationStatus status);

     // Count total applications for organization
     @Query("SELECT COUNT(a) FROM Applicant a WHERE a.recruitment.orgId = :orgId")
     Long countTotalApplications(@Param("orgId") Long orgId);

     // Count applications from previous week
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.applied_on::date >= (CURRENT_DATE - INTERVAL '1 week') AND a.applied_on::date < CURRENT_DATE", nativeQuery = true)
     Long countApplicationsPreviousWeek(@Param("orgId") Long orgId);

     // Count applications from previous month
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND EXTRACT(MONTH FROM a.applied_on) = EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '1 month'))", nativeQuery = true)
     Long countApplicationsPreviousMonth(@Param("orgId") Long orgId);

     // Count applications from previous quarter
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
     Long countApplicationsPreviousQuarter(@Param("orgId") Long orgId);

     // Average time to hire (in days) - from applied to offer accepted
     @Query(value = "SELECT AVG(EXTRACT(DAY FROM (a.updated_on - a.applied_on))) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.application_status = 'OFFER_ACCEPTED'", nativeQuery = true)
     Double averageTimeToHire(@Param("orgId") Long orgId);

     // Average time to hire from previous quarter
     @Query(value = "SELECT AVG(EXTRACT(DAY FROM (a.updated_on - a.applied_on))) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.application_status = 'OFFER_ACCEPTED' AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
     Double averageTimeToHirePreviousQuarter(@Param("orgId") Long orgId);

     // Offer acceptance rate - count of offer accepted
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.application_status = 'OFFER_ACCEPTED'", nativeQuery = true)
     Long countOfferAccepted(@Param("orgId") Long orgId);

     // Count selected candidates
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.application_status = 'SELECTED'", nativeQuery = true)
     Long countSelected(@Param("orgId") Long orgId);

     // Count offer accepted from previous quarter
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.application_status = 'OFFER_ACCEPTED' AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
     Long countOfferAcceptedPreviousQuarter(@Param("orgId") Long orgId);

     // Count selected from previous quarter
     @Query(value = "SELECT COUNT(a) FROM hr.t_hr_applicants a JOIN hr.t_hr_recruitments r ON a.recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND a.application_status = 'SELECTED' AND EXTRACT(QUARTER FROM a.applied_on) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM a.applied_on) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
     Long countSelectedPreviousQuarter(@Param("orgId") Long orgId);
}

