package com.nexus.hr.repository;

import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import com.nexus.hr.payload.reflections.ExperienceBucketCount;
import com.nexus.hr.payload.reflections.OrgOpeningCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface RecruitmentRepo extends JpaRepository<Recruitment, Long>, JpaSpecificationExecutor<Recruitment> {

    // Query by orgId only
    Page<Recruitment> findByOrgId(Long orgId, Pageable pageable);

    // Query by orgId and isActive
    Page<Recruitment> findByOrgIdAndIsActive(Long orgId, Boolean isActive, Pageable pageable);

    // Query by orgId and empId (createdBy.employeeId)
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeId(@Param("orgId") Long orgId, @Param("empId") Long empId,
            Pageable pageable);

    // Query by orgId and hiringType
    Page<Recruitment> findByOrgIdAndHiringType(Long orgId, HiringType hiringType, Pageable pageable);

    // Query by orgId and hiringStatus
    Page<Recruitment> findByOrgIdAndHiringStatus(Long orgId, HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, isActive, and empId
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId")
    Page<Recruitment> findByOrgIdAndIsActiveAndCreatedByEmployeeId(@Param("orgId") Long orgId,
            @Param("isActive") Boolean isActive, @Param("empId") Long empId, Pageable pageable);

    // Query by orgId, isActive, and hiringType
    Page<Recruitment> findByOrgIdAndIsActiveAndHiringType(Long orgId, Boolean isActive, HiringType hiringType,
            Pageable pageable);

    // Query by orgId, isActive, and hiringStatus
    Page<Recruitment> findByOrgIdAndIsActiveAndHiringStatus(Long orgId, Boolean isActive, HiringStatus hiringStatus,
            Pageable pageable);

    // Query by orgId, empId, and hiringType
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeIdAndHiringType(@Param("orgId") Long orgId,
            @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, Pageable pageable);

    // ===== NEW QUERIES FOR DASHBOARD =====

    // Count open recruitments for org
    @Query("SELECT COUNT(r) FROM Recruitment r WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN' AND r.isActive = true")
    Long countOpenRecruitments(@Param("orgId") Long orgId);

    // Count open recruitments from previous month
    @Query(value = "SELECT COUNT(*) FROM hr.t_hr_recruitments WHERE org_id = :orgId AND hiring_status = 'OPEN' AND is_active = true AND EXTRACT(MONTH FROM created_at) = EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month')) AND EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '1 month'))", nativeQuery = true)
    Long countOpenRecruitmentsPreviousMonth(@Param("orgId") Long orgId);

    // Find top openings by application count
    @Query("SELECT r.recruitmentId, r.title, r.departmentName, COUNT(arm) as appCount, r.createdAt FROM Recruitment r LEFT JOIN r.applicantRecruitmentMappings arm WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN' AND r.isActive = true GROUP BY r.recruitmentId, r.title, r.departmentName, r.createdAt ORDER BY appCount DESC")
    List<Object[]> findTopOpeningsByApplications(@Param("orgId") Long orgId, Pageable pageable);

    // Find top roles by opening count
    @Query("SELECT r.roleName, COUNT(r) as openingCount FROM Recruitment r WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN' AND r.isActive = true GROUP BY r.roleName ORDER BY openingCount DESC")
    List<Object[]> findTopRolesByOpenings(@Param("orgId") Long orgId, Pageable pageable);

    // Status breakdown for open recruitments
    @Query("SELECT arm.status, COUNT(arm) FROM ApplicantRecruitmentMapping arm JOIN arm.recruitment r WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN' AND r.isActive = true GROUP BY arm.status")
    List<Object[]> findStatusBreakdown(@Param("orgId") Long orgId);

    // Query by orgId, empId, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeIdAndHiringStatus(@Param("orgId") Long orgId,
            @Param("empId") Long empId, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, hiringType, and hiringStatus
    Page<Recruitment> findByOrgIdAndHiringTypeAndHiringStatus(Long orgId, HiringType hiringType,
            HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, isActive, empId, and hiringType
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType")
    Page<Recruitment> findByOrgIdAndIsActiveAndCreatedByEmployeeIdAndHiringType(@Param("orgId") Long orgId,
            @Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType,
            Pageable pageable);

    // Query by orgId, isActive, empId, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByOrgIdAndIsActiveAndCreatedByEmployeeIdAndHiringStatus(@Param("orgId") Long orgId,
            @Param("isActive") Boolean isActive, @Param("empId") Long empId,
            @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, isActive, hiringType, and hiringStatus
    Page<Recruitment> findByOrgIdAndIsActiveAndHiringTypeAndHiringStatus(Long orgId, Boolean isActive,
            HiringType hiringType, HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, empId, hiringType, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeIdAndHiringTypeAndHiringStatus(@Param("orgId") Long orgId,
            @Param("empId") Long empId, @Param("hiringType") HiringType hiringType,
            @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by all five filters (orgId, isActive, empId, hiringType, hiringStatus)
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByAllFilters(@Param("orgId") Long orgId, @Param("isActive") Boolean isActive,
            @Param("empId") Long empId, @Param("hiringType") HiringType hiringType,
            @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    @Query("""
                        SELECT r FROM Recruitment r
                        WHERE r.orgId = :orgId
                        AND r.hiringStatus IN :statuses
            """)
    Page<Recruitment> findByOrgIdAndHiringStatusIn(Long orgId, List<HiringStatus> statuses, Pageable of);

    @Query(value = """
            SELECT * FROM hr.t_hr_recruitments r
            WHERE r.created_at >= CURRENT_DATE
              AND r.created_at < CURRENT_DATE + INTERVAL '1 day'
              AND (:status IS NULL OR r.hiring_status = :status)
              AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
              AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
              AND (:query IS NULL OR r.role_name ILIKE CONCAT('%', :query, '%'))
            OR (:query IS NULL OR r.title ILIKE CONCAT('%', :query, '%'))
            """, countQuery = """
            SELECT COUNT(*) FROM t_hr_recruitments r
            WHERE r.created_at >= CURRENT_DATE
              AND r.created_at < CURRENT_DATE + INTERVAL '1 day'
              AND (:status IS NULL OR r.hiring_status = :status)
              AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
              AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
              AND (:query IS NULL OR r.role_name ILIKE CONCAT('%', :query, '%'))
                OR (:query IS NULL OR r.title ILIKE CONCAT('%', :query, '%'))
            """, nativeQuery = true)
    Page<Recruitment> findOpeningsToday(
            Pageable pageable,
            @Param("status") String status,
            @Param("orgName") String orgName,
            @Param("location") String location, @Param("query") String query);

    @Query(value = """
            SELECT * FROM hr.t_hr_recruitments r
            WHERE r.created_at < CURRENT_DATE
              AND (:status IS NULL OR r.hiring_status = :status)
              AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
              AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
              AND (:query IS NULL OR r.role_name ILIKE CONCAT('%', :query, '%')
                                  OR r.title ILIKE CONCAT('%', :query, '%'))
            """, countQuery = """
            SELECT COUNT(*) FROM hr.t_hr_recruitments r
            WHERE r.created_at < CURRENT_DATE
              AND (:status IS NULL OR r.hiring_status = :status)
              AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
              AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
              AND (:query IS NULL OR r.role_name ILIKE CONCAT('%', :query, '%')
                                  OR r.title ILIKE CONCAT('%', :query, '%'))
            """, nativeQuery = true)
    Page<Recruitment> findOpeningsBeforeToday(Pageable pageable,
            @Param("status") String status,
            @Param("orgName") String orgName,
            @Param("location") String location,
            @Param("query") String query);

    @Query("""
                SELECT r.departmentName, COUNT(r)
                FROM Recruitment r
                WHERE r.isActive = true
                  AND r.hiringStatus = com.nexus.hr.model.enums.HiringStatus.OPEN
                GROUP BY r.departmentName
            """)
    List<Object[]> countOpeningsByDepartmentRaw();

    @Query("""
                SELECT
                    CASE
                        WHEN r.minYearsOfExperience BETWEEN 0 AND 2 THEN 'Junior Roles'
                        WHEN r.minYearsOfExperience BETWEEN 3 AND 5 THEN 'Mid-Level Roles'
                        WHEN r.minYearsOfExperience BETWEEN 6 AND 9 THEN 'Senior Roles'
                        WHEN r.minYearsOfExperience >= 10 THEN 'Executive Roles'
                        ELSE 'Unspecified'
                    END AS bucket,
                    COUNT(r) AS openingCount
                FROM Recruitment r
                WHERE r.isActive = true
                  AND r.hiringStatus = :status
                GROUP BY
                    CASE
                        WHEN r.minYearsOfExperience BETWEEN 0 AND 2 THEN 'Junior Roles'
                        WHEN r.minYearsOfExperience BETWEEN 3 AND 5 THEN 'Mid-Level Roles'
                        WHEN r.minYearsOfExperience BETWEEN 6 AND 9 THEN 'Senior Roles'
                        WHEN r.minYearsOfExperience >= 10 THEN 'Executive Roles'
                        ELSE 'Unspecified'
                    END
            """)
    List<ExperienceBucketCount> countOpeningsByExperienceBucket(@Param("status") HiringStatus status);

    @Query(value = """
            SELECT r.orgId AS orgId, r.orgName AS orgName, COUNT(r) AS openingCount
            FROM Recruitment r
            WHERE r.isActive = true AND r.hiringStatus = :status
            GROUP BY r.orgId, r.orgName
            ORDER BY r.orgName
            """)
    Page<OrgOpeningCount> findCurrentOpeningsGroupedByOrg(
            @Param("status") HiringStatus status,
            Pageable pageable);

    @Query("""
            SELECT r.orgId AS orgId, r.orgName AS orgName, COUNT(r) AS openingCount
            FROM Recruitment r
            WHERE r.isActive = true
              AND r.hiringStatus = :status
              AND r.createdAt < :monthStart
            GROUP BY r.orgId, r.orgName
            """)
    List<OrgOpeningCount> findOpeningsGroupedByOrgBefore(
            @Param("status") HiringStatus status,
            @Param("monthStart") Timestamp monthStart);

    @Query("""
                SELECT DISTINCT r.location
                FROM Recruitment r
                WHERE r.location IS NOT NULL AND r.location <> ''
            """)
    List<String> findAllLocations();

    @Query("""
                SELECT DISTINCT r.orgName
                FROM Recruitment r
                WHERE r.orgName IS NOT NULL AND r.orgName <> ''
            """)
    List<String> findAllOrgNames();

    @Query("""
                SELECT r
                FROM Recruitment r
                WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :name, '%'))
                   OR LOWER(r.roleName) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<Recruitment> findByTitleAndRoleNameContainingIgnoreCase(String name, Pageable pageRequest);

    // New methods for dashboard
    @Query("SELECT COUNT(DISTINCT r.orgId) FROM Recruitment r WHERE r.hiringStatus = 'OPEN'")
    Long countDistinctOrgsWithOpenRecruitments();

    @Query("SELECT COUNT(r) FROM Recruitment r WHERE r.hiringStatus = 'OPEN'")
    Long countAllOpenRecruitments();

    @Query("SELECT COUNT(r) FROM Recruitment r WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN' AND r.createdAt >= :monthStart")
    Long countOpenRecruitmentsCurrentMonth(@Param("orgId") Long orgId, @Param("monthStart") Timestamp monthStart);

    @Query("SELECT COUNT(r) FROM Recruitment r WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN' AND r.roleName = :roleName")
    Long countByOrgIdAndRoleNameAndHiringStatus(@Param("orgId") Long orgId, @Param("roleName") String roleName,
            @Param("hiringStatus") HiringStatus hiringStatus);

    @Query(value = "SELECT AVG(EXTRACT(DAY FROM (arm.updated_on - arm.applied_on))) FROM hr.t_hr_applicant_recruitment_mapping arm JOIN hr.t_hr_recruitments r ON arm.recruitment_recruitment_id = r.recruitment_id WHERE r.org_id = :orgId AND arm.status = 'OFFER_ACCEPTED'", nativeQuery = true)
    Double averageTimeToHireOverall(@Param("orgId") Long orgId);

    @Query(value = "SELECT AVG(EXTRACT(DAY FROM (arm.updated_on - arm.applied_on))) FROM hr.t_hr_applicant_recruitment_mapping arm WHERE arm.status = 'OFFER_ACCEPTED'", nativeQuery = true)
    Double averageTimeToHireOverall();
}
