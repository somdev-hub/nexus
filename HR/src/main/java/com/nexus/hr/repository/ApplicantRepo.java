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
}

