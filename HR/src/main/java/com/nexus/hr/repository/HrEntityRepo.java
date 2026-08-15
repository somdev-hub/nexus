package com.nexus.hr.repository;

import com.nexus.hr.model.entities.HrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrEntityRepo extends JpaRepository<HrEntity, Long> {

    @Query(value = "SELECT COUNT(*) FROM t_hr_entity WHERE org = :orgId AND on_notice_period=true", nativeQuery = true)
    Integer getAllWhoAreOnNoticePeriod(Long orgId);

    Optional<HrEntity> findByEmployeeId(Long id);

    @Query("""
                        SELECT COUNT(*) FROM HrEntity h
                        WHERE h.org = :orgId AND h.isActive = true
            """)
    Integer countAllByOrgAndIsActiveTrue(Long orgId);

    @Query(value = " SELECT (this_week.cnt - prev_week.cnt) AS diff FROM (SELECT COUNT(*) AS cnt FROM hr.t_hr_entity WHERE org = :orgId AND date_of_joining >= date_trunc('week', current_date) AND date_of_joining < date_trunc('week', current_date) + interval '7 days') this_week, (SELECT COUNT(*) AS cnt FROM hr.t_hr_entity WHERE org = :orgId AND date_of_joining >= date_trunc('week', current_date) - interval '7 days' AND date_of_joining < date_trunc('week', current_date)) prev_week ", nativeQuery = true)
    Integer getHrEntityCountDiffThisWeekVsPrevious(Long orgId);

    @Query(value = "select count(*) from hr.t_hr_entity t, hr.t_time_management tm where t.org =:orgId and t.hr_id = tm.hr_entity_hr_id and tm.is_present =true and tm.\"day\" =:day and tm.\"month\" = :month and tm.\"year\" =:year;", nativeQuery = true)
    Integer countPresentEmployees(Long orgId, Integer day, Integer month, Integer year);

    @Query(value = "SELECT (SELECT count(*) FROM hr.t_hr_entity t, hr.t_time_management tm WHERE t.org =:orgId AND t.hr_id = tm.hr_entity_hr_id AND tm.is_present =true AND tm.\"day\" =:day AND tm.\"month\" = :month AND tm.\"year\" =:year) - (SELECT count(*) FROM hr.t_hr_entity t, hr.t_time_management tm WHERE t.org =:orgId AND t.hr_id = tm.hr_entity_hr_id AND tm.is_present =true AND tm.\"day\" =:day-1 AND tm.\"month\" = :month AND tm.\"year\" =:year) AS diff", nativeQuery = true)
    Integer getPresentEmployeesCountDiffTodayVsYesterday(Long orgId, Integer day, Integer month, Integer year);

    @Query(value = "select count(*) from hr.t_hr_entity t, hr.t_time_management tm where t.org =:orgId and t.hr_id = tm.hr_entity_hr_id and tm.is_on_leave =true and tm.\"day\" =:day-1 and tm.\"month\" = :month and tm.\"year\" =:year;", nativeQuery = true)
    Integer getPreviousDayOnLeaveEmployeesCount(Long orgId, Integer day, Integer month, Integer year);

    @Query("SELECT h FROM HrEntity h LEFT JOIN FETCH h.leaveAllocations WHERE h.hrId = :hrId")
    Optional<HrEntity> findByHrIdWithLeaveAllocations(@Param("hrId") Long hrId);

	Optional<HrEntity> findByEmployeeEmail(String employeeEmail);
}
