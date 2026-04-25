package com.nexus.hr.service.interfaces;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    ResponseEntity<?> getEmployeeAvgStrength(Long orgId);

    ResponseEntity<?> getLeaveTypeDistribution(Long orgId, String monthYear);

    ResponseEntity<?> getEmployeeCheckInCheckOut(Long orgId, String monthYear);

    ResponseEntity<?> getEmployeeBreakStartEnd(Long orgId, String monthYear);

    ResponseEntity<?> getPayrollYearly(Long orgId);

    ResponseEntity<?> getPayrollRoleWise(Long orgId, Map<String, List<Long>> roleEmpIdsMappings);

    ResponseEntity<?> getOvertimeAnomaly(Long orgId);

    ResponseEntity<?> getLeavesDepartmentWise(Long orgId, String monthYear);

    ResponseEntity<?> getLeavesRoleWise(Long orgId, Map<String, List<Long>> roleEmpIdMap, String monthYear);
}
