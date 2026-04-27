package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrRequest;
import com.nexus.hr.model.enums.HrRequestStatus;
import com.nexus.hr.model.enums.HrRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HrRequestRepo extends JpaRepository<HrRequest, Long> {


    @Query("""
                    SELECT hrRequest
                    FROM HrRequest hrRequest
                    JOIN hrRequest.appliedBy hrEntity
                    WHERE hrEntity.org = :orgId
                    ORDER BY hrRequest.appliedOn DESC
            """)
    Page<HrRequest> findAllWithOrgId(Long orgId, Pageable pageable);

    @Query("""
                                SELECT hrRequest
                                FROM HrRequest hrRequest
                                JOIN hrRequest.appliedBy hrEntity
                                WHERE hrEntity.org = :orgId AND hrRequest.requestType = :requestType
                                ORDER BY hrRequest.appliedOn DESC
            
            """)
    Page<HrRequest> findAllByHrRequestTypeAndOrgId(Long orgId, HrRequestType requestType, Pageable pageable);

    @Query("""
                                            SELECT hrRequest
                                            FROM HrRequest hrRequest
                                            JOIN hrRequest.appliedBy hrEntity
                                            WHERE hrEntity.org = :orgId AND hrRequest.status = :status
                                            ORDER BY hrRequest.appliedOn DESC
            """)
    Page<HrRequest> findAllByStatusAndOrgId(Long orgId, HrRequestStatus status, Pageable pageable);

    @Query(
            """
                                                                SELECT hrRequest
                                                                FROM HrRequest hrRequest
                                                                JOIN hrRequest.appliedBy hrEntity
                                                                WHERE hrEntity.org = :orgId AND hrRequest.requestType = :requestType AND hrRequest.status = :status
                                                                ORDER BY hrRequest.appliedOn DESC
                    
                    """
    )
    Page<HrRequest> findAllByHrRequestTypeAndStatusAndOrgId(Long orgId, HrRequestType requestType, HrRequestStatus status, Pageable pageable);

    @Query("""
                                SELECT hrRequest
                                FROM HrRequest hrRequest
                                JOIN hrRequest.appliedBy hrEntity
                                WHERE hrEntity.org = :orgId AND hrRequest.status IN :closed
                                ORDER BY hrRequest.appliedOn DESC
            """)
    Page<HrRequest> findAllByOrgIdAndStatusIn(Long orgId, List<HrRequestStatus> closed, PageRequest of);

    @Query("""
                                            SELECT COUNT(hrRequest)
                                            FROM HrRequest hrRequest
                                            JOIN hrRequest.appliedBy hrEntity
                                            WHERE hrEntity.org = :orgId AND hrRequest.status = :hrRequestStatus
            """)
    Long findCountByOrgIdAndStatus(Long orgId, HrRequestStatus hrRequestStatus);

    @Query("""
                                                        SELECT COUNT(hrRequest)
                                                        FROM HrRequest hrRequest
                                                        JOIN hrRequest.appliedBy hrEntity
                                                        WHERE hrEntity.org = :orgId AND hrRequest.status IN :statuses
            """)
    Long findCountByOrgIdAndStatusIn(Long orgId, List<HrRequestStatus> statuses);

    @Query(value = "select count(*) from hr.t_hr_entity t, hr.t_hr_requests tr where t.org =:orgId and t.hr_id =tr.hr_entity_hr_id and tr.status = 'OPEN'", nativeQuery = true)
    Integer countOpenRequestsByOrgId(Long orgId);

    @Query(value = """
                SELECT COUNT(*)
                FROM hr.t_hr_entity t
                JOIN hr.t_hr_requests tr ON t.hr_id = tr.hr_entity_hr_id
                WHERE t.org = :orgId
                  AND tr.status = 'OPEN'
                  AND tr.applied_on >= NOW() - INTERVAL '7 days'
            """, nativeQuery = true)
    Integer countDiffInPrevWeekAndThisWeekHrRequests(Long orgId);

    @Query(value = " SELECT t.* FROM hr.t_hr_requests t JOIN hr.t_hr_entity te on te.hr_id =t.hr_entity_hr_id WHERE te.org = :orgId AND t.applied_on >= :startOfDay AND t.applied_on < :endOfDay", countQuery = "SELECT COUNT(*) FROM hr.t_hr_requests t JOIN hr.t_hr_entity te on te.hr_id =t.hr_entity_hr_id WHERE te.org = :orgId AND t.applied_on >= :startOfDay AND t.applied_on < :endOfDay", nativeQuery = true)
    List<HrRequest> findByOrgIdAndDateToday(
            @Param("orgId") Long orgId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable of
    );

    @Query(value = "SELECT t.* FROM hr.t_hr_requests t JOIN hr.t_hr_entity te on te.hr_id =t.hr_entity_hr_id WHERE te.org = :orgId AND t.applied_on >= :startOfDay AND t.applied_on < :endOfDay AND t.status = CAST(:status AS VARCHAR)", countQuery = "SELECT COUNT(*) FROM hr.t_hr_requests t JOIN hr.t_hr_entity te on te.hr_id =t.hr_entity_hr_id WHERE te.org = :orgId AND t.applied_on >= :startOfDay AND t.applied_on < :endOfDay AND t.status = CAST(:status AS VARCHAR)", nativeQuery = true)
    List<HrRequest> findByOrgIdAndDateTodayAndStatus(@Param("orgId") Long orgId, @Param("status") String status, @Param("startOfDay") LocalDateTime startOfDay,
                                                     @Param("endOfDay") LocalDateTime endOfDay,
                                                     Pageable of);

    @Query(value = "SELECT t.* FROM hr.t_hr_requests t JOIN hr.t_hr_entity te on te.hr_id =t.hr_entity_hr_id WHERE te.org = :orgId AND t.applied_on >= :startOfDay AND t.applied_on < :endOfDay AND te.employee_id = :empId", countQuery = "SELECT COUNT(*) FROM hr.t_hr_requests t JOIN hr.t_hr_entity te on te.hr_id =t.hr_entity_hr_id WHERE te.org = :orgId AND t.applied_on >= :startOfDay AND t.applied_on < :endOfDay AND te.employee_id = :empId", nativeQuery = true)
    List<HrRequest> findByOrgIdAndDateTodayAndEmpId(Long orgId, Long empId, LocalDateTime startOfDay, LocalDateTime endOfDay, Pageable of);
}
