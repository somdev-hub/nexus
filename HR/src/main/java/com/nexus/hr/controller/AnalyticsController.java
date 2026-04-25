package com.nexus.hr.controller;

import com.nexus.hr.service.interfaces.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("hr/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/employee/avg-strength")
    public ResponseEntity<?> getEmployeeAvgStrength(@RequestParam Long orgId) {
        return analyticsService.getEmployeeAvgStrength(orgId);
    }

    @GetMapping("/leave/type-distribution")
    public ResponseEntity<?> getLeaveTypeDistribution(@RequestParam Long orgId, @RequestParam String monthYear) {
        return analyticsService.getLeaveTypeDistribution(orgId, monthYear);
    }

    @GetMapping("/employee/check-in-check-out")
    public ResponseEntity<?> getEmployeeCheckInCheckOut(@RequestParam Long orgId, @RequestParam String monthYear) {
        return analyticsService.getEmployeeCheckInCheckOut(orgId, monthYear);
    }

    @GetMapping("/employee/break-start-end")
    public ResponseEntity<?> getEmployeeBreakStartEnd(@RequestParam Long orgId, @RequestParam String monthYear) {
        return analyticsService.getEmployeeBreakStartEnd(orgId, monthYear);
    }

    @GetMapping("/payroll/yearly")
    public ResponseEntity<?> getPayrollYearly(@RequestParam Long orgId) {
        return analyticsService.getPayrollYearly(orgId);
    }

    @PostMapping("/payroll/role-wise")
    public ResponseEntity<?> getPayrollRoleWise(@RequestParam Long orgId, @RequestBody Map<String, List<Long>> roleEmpIdMap) {
        return analyticsService.getPayrollRoleWise(orgId, roleEmpIdMap);
    }

    @GetMapping("/overtime/anomaly")
    public ResponseEntity<?> getOvertimeAnomaly(@RequestParam Long orgId) {
        return analyticsService.getOvertimeAnomaly(orgId);
    }

    @GetMapping("/leaves/department-wise")
    public ResponseEntity<?> getLeavesDepartmentWise(@RequestParam Long orgId, @RequestParam String monthYear) {
        return analyticsService.getLeavesDepartmentWise(orgId, monthYear);
    }

    @PostMapping("/leaves/role-wise")
    public ResponseEntity<?> getLeavesRoleWise(@RequestParam Long orgId, @RequestBody Map<String, List<Long>> roleEmpIdMap, @RequestParam String monthYear) {
        return analyticsService.getLeavesRoleWise(orgId, roleEmpIdMap, monthYear);
    }
}
