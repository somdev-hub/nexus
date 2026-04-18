package com.nexus.hr.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO to hold payroll insights aggregation data
 * Contains aggregated payroll statistics for a given organization, month, and year
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollInsightsDto {
    private Double totalNetSalaries;             // Sum of net salaries for all payrolls in given month/year
    private Double totalProcessedSalaries;       // Sum where paymentStatus=COMPLETED
    private Double totalPendingSalaries;         // Sum where paymentStatus=PENDING
    private Double totalNotProcessedSalaries;    // Sum for hrEntities with no payroll entry for month/year
    private Double totalPayrollCost;             // Total cost of all payrolls (gross pay)
    private Double averageNetSalaryPerEmployee;  // Average net salary per employee
    private Double totalDeductions;              // Sum of all deductions for the period
    private Double totalOvertimeCost;            // Sum of OVERTIME bonus payments
}

