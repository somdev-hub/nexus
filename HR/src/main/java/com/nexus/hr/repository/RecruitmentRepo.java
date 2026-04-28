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

    // Query by isActive only
    Page<Recruitment> findByIsActive(Boolean isActive, Pageable pageable);

    // Query by empId (createdBy.employeeId) only
    @Query("SELECT r FROM Recruitment r WHERE r.createdBy.employeeId = :empId")
    Page<Recruitment> findByCreatedByEmployeeId(@Param("empId") Long empId, Pageable pageable);

    // Query by hiringType only
    Page<Recruitment> findByHiringType(HiringType hiringType, Pageable pageable);

    // Query by hiringStatus only
    Page<Recruitment> findByHiringStatus(HiringStatus hiringStatus, Pageable pageable);

    // Query by isActive and empId
    @Query("SELECT r FROM Recruitment r WHERE r.isActive = :isActive AND r.createdBy.employeeId = :empId")
    Page<Recruitment> findByIsActiveAndCreatedByEmployeeId(@Param("isActive") Boolean isActive, @Param("empId") Long empId, Pageable pageable);

    // Query by isActive and hiringType
    Page<Recruitment> findByIsActiveAndHiringType(Boolean isActive, HiringType hiringType, Pageable pageable);

    // Query by isActive and hiringStatus
    Page<Recruitment> findByIsActiveAndHiringStatus(Boolean isActive, HiringStatus hiringStatus, Pageable pageable);

    // Query by empId and hiringType
    @Query("SELECT r FROM Recruitment r WHERE r.createdBy.employeeId = :empId AND r.hiringType = :hiringType")
    Page<Recruitment> findByCreatedByEmployeeIdAndHiringType(@Param("empId") Long empId, @Param("hiringType") HiringType hiringType, Pageable pageable);

    // Query by empId and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.createdBy.employeeId = :empId AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByCreatedByEmployeeIdAndHiringStatus(@Param("empId") Long empId, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by hiringType and hiringStatus
    Page<Recruitment> findByHiringTypeAndHiringStatus(HiringType hiringType, HiringStatus hiringStatus, Pageable pageable);

    // Query by isActive, empId, and hiringType
    @Query("SELECT r FROM Recruitment r WHERE r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType")
    Page<Recruitment> findByIsActiveAndCreatedByEmployeeIdAndHiringType(@Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, Pageable pageable);

    // Query by isActive, empId, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByIsActiveAndCreatedByEmployeeIdAndHiringStatus(@Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by isActive, hiringType, and hiringStatus
    Page<Recruitment> findByIsActiveAndHiringTypeAndHiringStatus(Boolean isActive, HiringType hiringType, HiringStatus hiringStatus, Pageable pageable);

    // Query by empId, hiringType, and hiringStatus
    @Query("SELECT r FROM Recruitment r WHERE r.createdBy.employeeId = :empId AND r.hiringType = :hiringType AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByCreatedByEmployeeIdAndHiringTypeAndHiringStatus(@Param("empId") Long empId, @Param("hiringType") HiringType hiringType, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

    // Query by all four filters
    @Query("SELECT r FROM Recruitment r WHERE r.isActive = :isActive AND r.createdBy.employeeId = :empId AND r.hiringType = :hiringType AND r.hiringStatus = :hiringStatus")
    Page<Recruitment> findByAllFilters(@Param("isActive") Boolean isActive, @Param("empId") Long empId, @Param("hiringType") HiringType hiringType, @Param("hiringStatus") HiringStatus hiringStatus, Pageable pageable);

}
