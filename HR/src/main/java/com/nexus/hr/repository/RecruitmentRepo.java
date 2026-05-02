package com.nexus.hr.repository;

import com.nexus.hr.model.entities.Recruitment;
import com.nexus.hr.model.enums.HiringStatus;
import com.nexus.hr.model.enums.HiringType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
