package com.nexus.iam.service.impl;

import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.AnalyticsService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final WebConstants webConstants;
    private final RestService restService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<?> getEmployeeAvgStrength(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeAveStrengthUrl()).queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch employee average strength");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getEmployeeAvgStrength",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getLeaveTypeDistribution(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getLeaveTypeDistributionUrl())
                    .queryParam("orgId", orgId).queryParam("monthYear", monthYear);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch leave type distribution");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getLeaveTypeDistribution",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }

    }

    @Override
    public ResponseEntity<?> getEmployeeCheckInCheckOut(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeCheckInCheckOutUrl())
                    .queryParam("orgId", orgId).queryParam("monthYear", monthYear);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch employee check-in/check-out data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getEmployeeCheckInCheckOut",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }

    }

    @Override
    public ResponseEntity<?> getEmployeeBreakStartEnd(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getEmployeeBreakStartEndUrl())
                    .queryParam("orgId", orgId).queryParam("monthYear", monthYear);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch employee break start/end data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getEmployeeBreakStartEnd",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getPayrollYearly(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try{
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getPayrollYearlyUrl()).queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch payroll yearly data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getPayrollYearly",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getPayrollRoleWise(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try{
            List<Map<String, Object>> rolesWithUserIds = userRepository.getRolesWithUserIds(orgId);
            Map<String, List<Long>> roleEmpIdMap = new HashMap<>();
            for (Map<String, Object> record : rolesWithUserIds) {
                String roleName = (String) record.get("roleName");
                Object userIdObj = record.get("userId");

                // Only add users that have a userId (skip roles without users)
                if (userIdObj != null) {
                    Long userId = ((Number) userIdObj).longValue();
                    roleEmpIdMap.computeIfAbsent(roleName, k -> new ArrayList<>()).add(userId);
                }
            }
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getPayrollRoleWiseUrl()).queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), roleEmpIdMap, null, HttpMethod.POST, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch payroll role-wise data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getPayrollRoleWise",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getOvertimeAnomaly(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try{
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getOvertimeAnomalyUrl()).queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch overtime anomaly data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getOvertimeAnomaly",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getLeavesDepartmentWise(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getLeavesDepartmentWiseUrl())
                    .queryParam("orgId", orgId).queryParam("monthYear", monthYear);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch leaves department-wise data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getLeavesDepartmentWise",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getLeavesRoleWise(Long orgId, String monthYear) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            List<Map<String, Object>> rolesWithUserIds = userRepository.getRolesWithUserIds(orgId);
            Map<String, List<Long>> roleEmpIdMap = new HashMap<>();
            for (Map<String, Object> record : rolesWithUserIds) {
                String roleName = (String) record.get("roleName");
                Object userIdObj = record.get("userId");

                // Only add users that have a userId (skip roles without users)
                if (userIdObj != null) {
                    Long userId = ((Number) userIdObj).longValue();
                    roleEmpIdMap.computeIfAbsent(roleName, k -> new ArrayList<>()).add(userId);
                }
            }
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getLeavesRoleWiseUrl())
                    .queryParam("orgId", orgId).queryParam("monthYear", monthYear);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), roleEmpIdMap, null, HttpMethod.POST, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch leaves role-wise data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getLeavesRoleWise",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyEmployeeStrength(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getWeeklyEmployeeStrengthUrl())
                    .queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch weekly employee strength data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getWeeklyEmployeeStrength",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyWorkingHours(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getWeeklyWorkingHoursUrl())
                    .queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch weekly working hours data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getWeeklyWorkingHours",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyCheckInCheckOut(Long orgId) {
        if (ObjectUtils.isEmpty(orgId)) {
            return ResponseEntity.badRequest().body("Organization ID is required");
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getWeeklyCheckInCheckOutUrl())
                    .queryParam("orgId", orgId);
            ResponseEntity<String> response = restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody() != null ? response.getBody() : "Failed to fetch weekly check-in/check-out data");
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "AnalyticsService",
                    "Method execution failed",
                    "getWeeklyCheckInCheckOut",
                    e.getClass().getName(),
                    e.getMessage()
            );
        }
    }
}