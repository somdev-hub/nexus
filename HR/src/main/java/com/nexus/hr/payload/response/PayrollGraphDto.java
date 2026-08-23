package com.nexus.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class PayrollGraphDto {
    private List<SalaryVsRoleAggregationDto> salaryVsRole; // payrolls vs roles
    private List<SalaryVsOvertimeDto> salaryVsOvertime; // payrolls vs overtime components (last 6 months)
    private List<SalaryVsDeptDto> salaryVsDept; // payrolls vs dept
    private List<SalaryVsStatusDto> salaryVsStatus; // payrolls vs statuses
    private SalaryVsComponentDto salaryVsComponent;  // base vs bonus vs deductions

    /**
     * DTO to hold aggregated payroll data by role
     * Represents summed values from database queries
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryVsRoleAggregationDto {
        private String role;
        private Double baseSalary;      // Sum of basePay for all employees in role
        private Double bonus;           // Sum of totalBonuses for all employees in role
        private Long employeeCount;     // Count of employees in role for this month
    }

    /**
     * DTO to hold monthly salary vs overtime aggregation data
     * Used for last 6 months analysis
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryVsOvertimeDto {
        private String month;              // Month name (e.g., "APRIL")
        private Integer year;              // Year (e.g., 2026)
        private Double totalSalary;        // Base + Bonus - Deductions
        private Double overtimePay;        // Total overtime pay from PayrollBonuses where salaryType='OVERTIME'
        private Long employeeCount;        // Number of unique employees with payroll in this month
    }

    /**
     * DTO to hold department-wise payroll aggregation data
     * Represents summed values for each department
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryVsDeptDto {
        private String dept;               // Department name
        private Double baseSalary;         // Sum of basePay for all employees in department
        private Double bonus;              // Sum of totalBonuses for all employees in department
    }

    /**
     * DTO to hold status-wise payroll count
     * Represents number of payrolls by payment status
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryVsStatusDto {
        private String status;             // Payment status (PENDING, COMPLETED, FAILED, NOT PROCESSED)
        private Long noOfPayrolls;         // Count of payrolls with this status
    }

    /**
     * DTO to hold salary component breakdown
     * Represents total base salary, bonuses, and deductions for a period
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryVsComponentDto {
        private Double baseSalary;         // Total base salary paid
        private Double bonus;              // Total bonuses paid
        private Double deduction;          // Total deductions deducted
    }
}
