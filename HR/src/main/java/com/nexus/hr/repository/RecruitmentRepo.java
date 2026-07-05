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
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeId(@Param("orgId") Long orgId, @Param("empId") Long empId, Pageable pageable);

    // Query by orgId and hiringType
    Page<Recruitment> findByOrgIdAndHiringType(Long orgId, HiringType hiringType, Pageable pageable);

    // Query by orgId and hiringStatus
    Page<Recruitment> findByOrgIdAndHiringStatus(Long orgId, HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, isActive, and empId
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId")
    Page<Recruitment> findByOrgIdAndIsActiveAndCreatedByEmployeeId(@Param("orgId") Long orgId, @Param("isActive") Boolean isActive, @Param("empId") Long empId, Pageable pageable);

    // Query by orgId, isActive, and hiringType
    Page<Recruitment> findByOrgIdAndIsActiveAndHiringType(Long orgId, Boolean isActive, HiringType hiringType, Pageable pageable);

    // Query by orgId, isActive, and hiringStatus
    Page<Recruitment> findByOrgIdAndIsActiveAndHiringStatus(Long orgId, Boolean isActive, HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, empId, and hiringType
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeIdAndHiringType(@Param("orgId") Long orgId, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, Pageable pageable);

    // Query by orgId, empId, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeIdAndHiringStatus(@Param("orgId") Long orgId, @Param("empId") Long empId, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, hiringType, and hiringStatus
    Page<Recruitment> findByOrgIdAndHiringTypeAndHiringStatus(Long orgId, HiringType hiringType, HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, isActive, empId, and hiringType
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType")
    Page<Recruitment> findByOrgIdAndIsActiveAndCreatedByEmployeeIdAndHiringType(@Param("orgId") Long orgId, @Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, Pageable pageable);

    // Query by orgId, isActive, empId, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByOrgIdAndIsActiveAndCreatedByEmployeeIdAndHiringStatus(@Param("orgId") Long orgId, @Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, isActive, hiringType, and hiringStatus
    Page<Recruitment> findByOrgIdAndIsActiveAndHiringTypeAndHiringStatus(Long orgId, Boolean isActive, HiringType hiringType, HiringStatus hiringStatus, Pageable pageable);

    // Query by orgId, empId, hiringType, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByOrgIdAndCreatedByEmployeeIdAndHiringTypeAndHiringStatus(@Param("orgId") Long orgId, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by all five filters (orgId, isActive, empId, hiringType, hiringStatus)
    @Query("SELECT r FROM Recruitment r WHERE r.orgId = :orgId AND r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByAllFilters(@Param("orgId") Long orgId, @Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);


    @Query("""
                        SELECT r FROM Recruitment r
                        WHERE r.orgId = :orgId
                        AND r.hiringStatus IN :statuses
            """)
    Page<Recruitment> findByOrgIdAndHiringStatusIn(Long orgId, List<HiringStatus> statuses, Pageable of);

    // Count open recruitments for an organization
    @Query("SELECT COUNT(r) FROM Recruitment r WHERE r.orgId = :orgId AND r.hiringStatus = 'OPEN'")
    Long countOpenRecruitments(@Param("orgId") Long orgId);

    // Count open recruitments from previous month
    @Query(value = "SELECT COUNT(r) FROM hr.t_hr_recruitments r WHERE r.org_id = :orgId AND r.hiring_status = 'OPEN' AND EXTRACT(MONTH FROM r.created_at) = EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month')) AND EXTRACT(YEAR FROM r.created_at) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '1 month'))", nativeQuery = true)
    Long countOpenRecruitmentsPreviousMonth(@Param("orgId") Long orgId);

    // Count open recruitments from previous quarter
    @Query(value = "SELECT COUNT(r) FROM hr.t_hr_recruitments r WHERE r.org_id = :orgId AND r.hiring_status = 'OPEN' AND EXTRACT(QUARTER FROM r.created_at) = EXTRACT(QUARTER FROM (CURRENT_DATE - INTERVAL '3 months')) AND EXTRACT(YEAR FROM r.created_at) = EXTRACT(YEAR FROM (CURRENT_DATE - INTERVAL '3 months'))", nativeQuery = true)
    Long countOpenRecruitmentsPreviousQuarter(@Param("orgId") Long orgId);

    @Query(value = """
            SELECT * FROM t_hr_recruitments r
            WHERE r.created_at >= CURRENT_DATE
              AND r.created_at < CURRENT_DATE + INTERVAL '1 day'
              AND (:status IS NULL OR r.hiring_status = :status)
              AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
              AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
            """,
            countQuery = """
                    SELECT COUNT(*) FROM t_hr_recruitments r
                    WHERE r.created_at >= CURRENT_DATE
                      AND r.created_at < CURRENT_DATE + INTERVAL '1 day'
                      AND (:status IS NULL OR r.hiring_status = :status)
                      AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
                      AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
                    """,
            nativeQuery = true)
    Page<Recruitment> findOpeningsToday(
            Pageable pageable,
            @Param("status") String status,
            @Param("orgName") String orgName,
            @Param("location") String location);

    @Query(value = """
            SELECT * FROM t_hr_recruitments r
                        WHERE r.created_at < CURRENT_DATE
                        AND (:status IS NULL OR r.hiring_status = :status)
                        AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
                        AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
            """, countQuery = """
            SELECT COUNT(*) FROM t_hr_recruitments r
            WHERE r.created_at < CURRENT_DATE
                AND (:status IS NULL OR r.hiring_status = :status)
                            AND (:orgName IS NULL OR r.org_name ILIKE CONCAT('%', :orgName, '%'))
                            AND (:location IS NULL OR r.location ILIKE CONCAT('%', :location, '%'))
            """,
            nativeQuery = true)
    Page<Recruitment> findOpeningsBeforeToday(Pageable pageable,
                                              @Param("status") String status,
                                              @Param("orgName") String orgName,
                                              @Param("location") String location);

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
        """,
            countQuery = """
        SELECT COUNT(DISTINCT r.orgId)
        FROM Recruitment r
        WHERE r.isActive = true AND r.hiringStatus = :status
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
}
