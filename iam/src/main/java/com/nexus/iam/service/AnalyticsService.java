package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

public interface AnalyticsService {
    ResponseEntity<?> getEmployeeAvgStrength(Long orgId);

    ResponseEntity<?> getLeaveTypeDistribution(Long orgId, String monthYear);

    ResponseEntity<?> getEmployeeCheckInCheckOut(Long orgId, String monthYear);

    ResponseEntity<?> getEmployeeBreakStartEnd(Long orgId, String monthYear);

    ResponseEntity<?> getPayrollYearly(Long orgId);

    ResponseEntity<?> getPayrollRoleWise(Long orgId);

    ResponseEntity<?> getOvertimeAnomaly(Long orgId);

    ResponseEntity<?> getLeavesDepartmentWise(Long orgId, String monthYear);

    ResponseEntity<?> getLeavesRoleWise(Long orgId, String monthYear);
}
