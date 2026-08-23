package com.nexus.hr.repository;

import com.nexus.hr.model.entities.EmployeeLeaves;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeLeavesRepo extends JpaRepository<EmployeeLeaves, Long> {

    @Query("""
            SELECT el.leaveType, COUNT(DISTINCT el.hrEntity.hrId) 
            FROM EmployeeLeaves el 
            WHERE el.hrEntity.org = :orgId 
            AND (
                (YEAR(el.startDate) = :year AND MONTH(el.startDate) = :month) OR
                (YEAR(el.endDate) = :year AND MONTH(el.endDate) = :month) OR
                (YEAR(el.startDate) < :year AND YEAR(el.endDate) > :year) OR
                (YEAR(el.startDate) = :year AND MONTH(el.startDate) < :month AND YEAR(el.endDate) = :year AND MONTH(el.endDate) > :month) OR
                (YEAR(el.startDate) = :year AND MONTH(el.startDate) < :month AND YEAR(el.endDate) > :year) OR
                (YEAR(el.startDate) < :year AND YEAR(el.endDate) = :year AND MONTH(el.endDate) > :month)
            )
            GROUP BY el.leaveType
            """)
    List<Object[]> getLeaveDistributionByMonthYear(@Param("orgId") Long orgId, @Param("month") Integer month, @Param("year") Integer year);

    @Query(value = """
            SELECT he.department AS department,
                   CAST(COALESCE(AVG(el.number_of_days), 0.0) AS DECIMAL(20,2)) AS avgLeaves
            FROM hr.t_hr_entity he
            LEFT JOIN hr.t_employee_leaves el
                   ON el.hr_entity_hr_id = he.hr_id
                  AND COALESCE(el.start_date, el.end_date) < :endDateExclusive
                  AND COALESCE(el.end_date, el.start_date) >= :startDate
            WHERE he.org = :orgId
            GROUP BY he.department
            ORDER BY he.department
            """, nativeQuery = true)
    List<Object[]> getAverageLeavesByDepartment(@Param("orgId") Long orgId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDateExclusive") LocalDate endDateExclusive);

    @Query(value = """
            SELECT CAST(COALESCE(AVG(el.number_of_days), 0.0) AS DECIMAL(20,2)) AS avgLeaves
            FROM hr.t_employee_leaves el
            JOIN hr.t_hr_entity he ON he.hr_id = el.hr_entity_hr_id
            WHERE he.org = :orgId
              AND he.employee_id IN (:empIds)
              AND COALESCE(el.start_date, el.end_date) < :endDateExclusive
              AND COALESCE(el.end_date, el.start_date) >= :startDate
            """, nativeQuery = true)
    Double getAverageLeavesByEmployeeIds(@Param("orgId") Long orgId,
                                         @Param("empIds") List<Long> empIds,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDateExclusive") LocalDate endDateExclusive);
}
