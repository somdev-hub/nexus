package com.nexus.hr.repository;

import com.nexus.hr.model.entities.Payroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
@Repository
public interface PayrollRepo extends JpaRepository<Payroll, Long> {
    @Query("SELECT p FROM Payroll p WHERE p.compensation.compensationId = :compensationId " +
            "AND p.month = :month AND p.year = :year order by p.paidOn desc")
    List<Payroll> findByCompensationIdAndMonthAndYear(
            @Param("compensationId") Long compensationId,
            @Param("month") String month,
            @Param("year") Integer year
    );

    @Query("SELECT p FROM Payroll p " +
            "WHERE p.compensation.hrEntity.org = :orgId " +
            "AND p.month = :month " +
            "AND p.year = :year " +
            "AND p.paymentStatus IN ('COMPLETED') " +
            "ORDER BY p.paidOn DESC")
    Page<Payroll> findPayrollsByOrgIdAndMonthAndYearAndProcessedStatus(
            @Param("orgId") Long orgId,
            @Param("month") String month,
            @Param("year") Integer year,
            Pageable pageable
    );

    /**
     * Aggregate payroll data by employee IDs and month
     * Returns sum of basePay and totalBonuses for given employees and month
     * Returns as Map with keys: baseSalary, bonus, employeeCount
     */
    @Query(value = "SELECT " +
            "CAST(COALESCE(SUM(p.base_pay), 0.0) AS DECIMAL(20,2)) as baseSalary, " +
            "CAST(COALESCE(SUM(p.total_bonuses), 0.0) AS DECIMAL(20,2)) as bonus, " +
            "COUNT(DISTINCT he.employee_id) as employeeCount " +
            "FROM hr.t_payrolls p " +
            "JOIN hr.t_compensations c ON p.compensation_id = c.compensation_id " +
            "JOIN hr.t_hr_entity he ON c.compensation_id = he.hr_compensation_id " +
            "WHERE he.employee_id IN :empIds " +
            "AND he.org = :orgId " +
            "AND p.month = :month " +
            "AND p.year = :year",
            nativeQuery = true)
    Map<String, Object> aggregatePayrollByEmpIdsAndMonthAndOrgId(
            @Param("empIds") List<Long> empIds,
            @Param("month") String month,
            @Param("year") Integer year,
            @Param("orgId") Long orgId
    );

    /**
     * Get last 6 months salary vs overtime data for an organization
     * Calculates: Total Salary (base + bonus - deductions) and Overtime Pay
     * Overtime pay is obtained from PayrollBonuses table where salaryType = 'OVERTIME'
     * Results ordered by year DESC, then month DESC (most recent first)
     * Uses ARRAY_POSITION for PostgreSQL compatibility to convert month names to numbers
     */
   @Query(value = "SELECT " +
            "p.month as month, " +
            "p.year as year, " +
            "CAST(COALESCE(SUM(p.base_pay + p.total_bonuses - p.total_deductions), 0.0) AS DECIMAL(20,2)) as totalSalary, " +
            "CAST(COALESCE(SUM(CASE WHEN pb.bonus_type = 'OVERTIME' THEN pb.amount ELSE 0.0 END), 0.0) AS DECIMAL(20,2)) as overtimePay, " +
            "COUNT(DISTINCT he.employee_id) as employeeCount " +
            "FROM hr.t_payrolls p " +
            "JOIN hr.t_compensations c ON p.compensation_id = c.compensation_id " +
            "JOIN hr.t_hr_entity he ON c.compensation_id = he.hr_compensation_id " +
            "LEFT JOIN hr.t_payroll_bonuses pb ON p.payroll_id = pb.payroll_id " +
            "WHERE he.org = :orgId " +
            "AND (p.year * 12 + ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], p.month)) >= " +
            "(:currentYear * 12 + ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], :currentMonth)) " +
            "AND (p.year * 12 + ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], p.month)) < " +
            "(:currentYear * 12 + ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], :currentMonth)) + 6 " +
            "GROUP BY p.year, p.month " +
            "ORDER BY p.year DESC, ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], p.month) DESC " +
            "LIMIT 6",
            nativeQuery = true)
    List<Map<String, Object>> getLast6MonthsSalaryVsOvertimeRaw(
            @Param("orgId") Long orgId,
            @Param("currentYear") Integer currentYear,
            @Param("currentMonth") String currentMonth
    );

    /**
     * Get department-wise payroll aggregation for current month
     * Returns: department, sum of basePay, sum of totalBonuses
     * Even if no payrolls exist for the month, all departments are returned with 0 values
     */
    @Query(value = "SELECT " +
            "he.department as dept, " +
            "CAST(COALESCE(SUM(p.base_pay), 0.0) AS DECIMAL(20,2)) as baseSalary, " +
            "CAST(COALESCE(SUM(p.total_bonuses), 0.0) AS DECIMAL(20,2)) as bonus " +
            "FROM hr.t_hr_entity he " +
            "JOIN hr.t_compensations c ON he.hr_compensation_id = c.compensation_id " +
            "LEFT JOIN hr.t_payrolls p ON c.compensation_id = p.compensation_id " +
            "    AND p.month = :month " +
            "    AND p.year = :year " +
            "WHERE he.org = :orgId " +
            "GROUP BY he.department " +
            "ORDER BY he.department ",
            nativeQuery = true)
    List<Map<String, Object>> getDeptWisePayrollRaw(
            @Param("orgId") Long orgId,
            @Param("month") String month,
            @Param("year") Integer year
    );

    /**
     * Get status-wise payroll count for provided org, month and year
     * Includes NOT PROCESSED status for employees with no payroll created
     * Returns: payment status and count of payrolls
     */
    @Query(value = "SELECT " +
            "COALESCE(p.payment_status, 'NOT PROCESSED') as status, " +
            "COUNT(*) as noOfPayrolls " +
            "FROM hr.t_hr_entity he " +
            "LEFT JOIN hr.t_compensations c ON he.hr_compensation_id = c.compensation_id " +
            "LEFT JOIN hr.t_payrolls p ON c.compensation_id = p.compensation_id " +
            "    AND p.month = :month " +
            "    AND p.year = :year " +
            "WHERE he.org = :orgId " +
            "GROUP BY COALESCE(p.payment_status, 'NOT PROCESSED') " +
            "ORDER BY status ",
            nativeQuery = true)
    List<Map<String, Object>> getStatusWisePayrollCountRaw(
            @Param("orgId") Long orgId,
            @Param("month") String month,
            @Param("year") Integer year
    );

    /**
     * Get salary component breakdown for provided org, month and year
     * Returns: total base salary, total bonuses, and total deductions
     */
    @Query(value = "SELECT " +
            "CAST(COALESCE(SUM(p.base_pay), 0.0) AS DECIMAL(20,2)) as baseSalary, " +
            "CAST(COALESCE(SUM(p.total_bonuses), 0.0) AS DECIMAL(20,2)) as bonus, " +
            "CAST(COALESCE(SUM(p.total_deductions), 0.0) AS DECIMAL(20,2)) as deduction " +
            "FROM hr.t_payrolls p " +
            "JOIN hr.t_compensations c ON p.compensation_id = c.compensation_id " +
            "JOIN hr.t_hr_entity he ON c.compensation_id = he.hr_compensation_id " +
            "WHERE he.org = :orgId " +
            "AND p.month = :month " +
            "AND p.year = :year",
            nativeQuery = true)
    Map<String, Object> getSalaryComponentBreakdownRaw(
            @Param("orgId") Long orgId,
            @Param("month") String month,
            @Param("year") Integer year
    );

    /**
     * Get payroll insights aggregation data for provided org, month and year
     * Returns:
     *   - totalNetSalaries: Sum of net_pay for all payrolls
     *   - totalProcessedSalaries: Sum of net_pay where payment_status = 'COMPLETED'
     *   - totalPendingSalaries: Sum of net_pay where payment_status = 'PENDING'
     *   - totalPayrollCost: Sum of gross_pay for all payrolls
     *   - averageNetSalaryPerEmployee: AVG of net_pay for all payrolls
     *   - totalDeductions: Sum of total_deductions for all payrolls
     *   - totalOvertimeCost: Sum of pb.amount where pb.bonus_type = 'OVERTIME'
     */
    @Query(value = "SELECT " +
            "CAST(COALESCE(SUM(p.net_pay), 0.0) AS DECIMAL(20,2)) as totalNetSalaries, " +
            "CAST(COALESCE(SUM(CASE WHEN p.payment_status = 'COMPLETED' THEN p.net_pay ELSE 0.0 END), 0.0) AS DECIMAL(20,2)) as totalProcessedSalaries, " +
            "CAST(COALESCE(SUM(CASE WHEN p.payment_status = 'PENDING' THEN p.net_pay ELSE 0.0 END), 0.0) AS DECIMAL(20,2)) as totalPendingSalaries, " +
            "CAST(COALESCE(SUM(p.gross_pay), 0.0) AS DECIMAL(20,2)) as totalPayrollCost, " +
            "CAST(COALESCE(AVG(p.net_pay), 0.0) AS DECIMAL(20,2)) as averageNetSalaryPerEmployee, " +
            "CAST(COALESCE(SUM(p.total_deductions), 0.0) AS DECIMAL(20,2)) as totalDeductions, " +
            "CAST(COALESCE(SUM(CASE WHEN pb.bonus_type = 'OVERTIME' THEN pb.amount ELSE 0.0 END), 0.0) AS DECIMAL(20,2)) as totalOvertimeCost " +
            "FROM hr.t_payrolls p " +
            "JOIN hr.t_compensations c ON p.compensation_id = c.compensation_id " +
            "JOIN hr.t_hr_entity he ON c.compensation_id = he.hr_compensation_id " +
            "LEFT JOIN hr.t_payroll_bonuses pb ON p.payroll_id = pb.payroll_id " +
            "WHERE he.org = :orgId " +
            "AND p.month = :month " +
            "AND p.year = :year",
            nativeQuery = true)
    Map<String, Object> getPayrollInsightsRaw(
            @Param("orgId") Long orgId,
            @Param("month") String month,
            @Param("year") Integer year
    );

    /**
     * Get total salary cost for hrEntities without any payroll in given month/year
     * This calculates the cost of employees who haven't been processed yet
     * Returns: Sum of compensation base_pay / 12 for employees with no payroll entry
     */
    @Query(value = "SELECT " +
            "CAST(COALESCE(SUM(c.base_pay / 12.0), 0.0) AS DECIMAL(20,2)) as totalNotProcessedSalaries " +
            "FROM hr.t_hr_entity he " +
            "JOIN hr.t_compensations c ON he.hr_compensation_id = c.compensation_id " +
            "LEFT JOIN hr.t_payrolls p ON c.compensation_id = p.compensation_id " +
            "    AND p.month = :month " +
            "    AND p.year = :year " +
            "WHERE he.org = :orgId " +
            "AND p.payroll_id IS NULL",
            nativeQuery = true)
    Map<String, Object> getTotalNotProcessedSalariesRaw(
            @Param("orgId") Long orgId,
            @Param("month") String month,
            @Param("year") Integer year
    );

    /**
     * Get monthly average net payroll for last 12 months
     */
    @Query(value = """
            SELECT
                   ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month)) AS month,
                   p.year AS year,
                   AVG(p.net_pay) AS avgNetPay
            FROM hr.t_payrolls p
            JOIN hr.t_compensations c ON c.compensation_id = p.compensation_id
            JOIN hr.t_hr_entity he ON he.hr_compensation_id = c.compensation_id
            WHERE he.org = :orgId
            AND (p.year > :startYear OR (p.year = :startYear AND ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month)) >= :startMonth))
            AND (p.year < :endYear OR (p.year = :endYear AND ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month)) <= :endMonth))
            GROUP BY p.year, ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month))
            ORDER BY p.year ASC, ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month)) ASC
            """, nativeQuery = true)
    List<Object[]> getMonthlyAverageNetPayroll(@Param("orgId") Long orgId,
                                               @Param("startYear") Integer startYear,
                                               @Param("startMonth") Integer startMonth,
                                               @Param("endYear") Integer endYear,
                                               @Param("endMonth") Integer endMonth);

    /**
     * Get quarterly average net payroll by employee
     */
    @Query(value = """
            SELECT
                CEIL(ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month))::FLOAT / 3) AS quarter,
                AVG(p.net_pay) AS avgNetPay
            FROM hr.t_payrolls p
            JOIN hr.t_compensations c ON c.compensation_id = p.compensation_id
            JOIN hr.t_hr_entity he ON he.hr_compensation_id = c.compensation_id
            WHERE he.employee_id IN :empIds
            AND P.payment_status = 'COMPLETED'
            AND (p.year > :startYear OR (p.year = :startYear AND ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month)) >= :startMonth))
            AND (p.year < :endYear OR (p.year = :endYear AND ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month)) <= :endMonth))
            GROUP BY CEIL(ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month))::FLOAT / 3)
            ORDER BY CEIL(ARRAY_POSITION(ARRAY['JANUARY','FEBRUARY','MARCH','APRIL','MAY','JUNE','JULY','AUGUST','SEPTEMBER','OCTOBER','NOVEMBER','DECEMBER'], UPPER(p.month))::FLOAT / 3)
            """, nativeQuery = true)
    List<Object[]> getQuarterlyAverageNetPayrollByEmployees(@Param("empIds") List<Long> empIds,
                                                            @Param("startYear") Integer startYear,
                                                            @Param("startMonth") Integer startMonth,
                                                            @Param("endYear") Integer endYear,
                                                            @Param("endMonth") Integer endMonth);
}
