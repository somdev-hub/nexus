package com.nexus.iam.controller;

import com.nexus.iam.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam/analytics")
@RequiredArgsConstructor
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

    @GetMapping("/payroll/role-wise")
    public ResponseEntity<?> getPayrollRoleWise(@RequestParam Long orgId) {
        return analyticsService.getPayrollRoleWise(orgId);
    }

    @GetMapping("/overtime/anomaly")
    public ResponseEntity<?> getOvertimeAnomaly(@RequestParam Long orgId) {
        return analyticsService.getOvertimeAnomaly(orgId);
    }

    @GetMapping("/leaves/department-wise")
    public ResponseEntity<?> getLeavesDepartmentWise(@RequestParam Long orgId, @RequestParam String monthYear) {
        return analyticsService.getLeavesDepartmentWise(orgId, monthYear);
    }

    @GetMapping("/leaves/role-wise")
    public ResponseEntity<?> getLeavesRoleWise(@RequestParam Long orgId, @RequestParam String monthYear) {
        return analyticsService.getLeavesRoleWise(orgId, monthYear);
    }

    // Weekly analytics endpoints (last 7 days)
    @GetMapping("/employee/weekly-strength")
    public ResponseEntity<?> getWeeklyEmployeeStrength(@RequestParam Long orgId) {
        return analyticsService.getWeeklyEmployeeStrength(orgId);
    }

    @GetMapping("/employee/weekly-working-hours")
    public ResponseEntity<?> getWeeklyWorkingHours(@RequestParam Long orgId) {
        return analyticsService.getWeeklyWorkingHours(orgId);
    }

    @GetMapping("/employee/weekly-check-in-check-out")
    public ResponseEntity<?> getWeeklyCheckInCheckOut(@RequestParam Long orgId) {
        return analyticsService.getWeeklyCheckInCheckOut(orgId);
    }
}
