package com.nexus.iam.service.impl;

import com.nexus.iam.dto.InitiateEmployeePayrollDto;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.service.EmployeePayrollService;
import com.nexus.iam.service.OrganizationService;
import com.nexus.iam.service.UserService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmployeePayrollServiceImpl implements EmployeePayrollService {

    private final WebConstants webConstants;
    private final RestService restService;
    private final OrganizationService organizationService;
    private final UserService userService;

    @Override
    public ResponseEntity<?> initiatePayroll(InitiateEmployeePayrollDto initiateEmployeePayrollDto, String auth) {
        if (ObjectUtils.isEmpty(initiateEmployeePayrollDto) || ObjectUtils.isEmpty(auth)) {
            throw new ServiceLevelException(
                    "EmployeePayrollService",
                    "Initiate Employee Payroll",
                    "initiatePayroll",
                    "InvalidInputException",
                    "InitiateEmployeePayrollDto and Authorization token cannot be null or empty"
            );
        }
        ResponseEntity<?> response;
        try {
            Map<String, Object> payrollRequestMap = new HashMap<>();
            ResponseEntity<?> organizationDetailsById = organizationService.getOrganizationDetailsById(initiateEmployeePayrollDto.getOrgId());
            if (!organizationDetailsById.getStatusCode().is2xxSuccessful()) {
                throw new ServiceLevelException(
                        "EmployeePayrollService",
                        "Initiate Employee Payroll",
                        "initiatePayroll",
                        "OrganizationNotFoundException",
                        "Organization with ID " + initiateEmployeePayrollDto.getOrgId() + " not found"
                );
            }
            payrollRequestMap.put("org", organizationDetailsById.getBody());
            List<Object> employeeDetails = new ArrayList<>();
            for (Long employeeId : initiateEmployeePayrollDto.getEmployeeIds()) {
                ResponseEntity<?> userDetails = userService.getUserDetails(employeeId);
                if (userDetails.getStatusCode().is2xxSuccessful()) {
                    employeeDetails.add(userDetails.getBody());
                }
            }
            payrollRequestMap.put("employees", employeeDetails);

            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put(HttpHeaders.AUTHORIZATION, auth);
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            response = restService.iamRestCall(webConstants.getInitiatePayrollUrl(), payrollRequestMap, headers, HttpMethod.POST, null);

        } catch (Exception e) {
            throw new ServiceLevelException(
                    "EmployeePayrollService",
                    "Initiate Employee Payroll",
                    "initiatePayroll",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
        return response;
    }
}
