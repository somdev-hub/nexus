package com.nexus.iam.controller;

import com.nexus.iam.dto.InitiateEmployeePayrollDto;
import com.nexus.iam.exception.UnauthorizedException;
import com.nexus.iam.service.EmployeePayrollService;
import com.nexus.iam.utils.KeycloakTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/iam/employee-payroll")
public class EmployeePayrollController {
    private final EmployeePayrollService employeePayrollService;
    private final KeycloakTokenUtil keycloakTokenUtil;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateEmployeePayroll(@RequestBody InitiateEmployeePayrollDto initiateEmployeePayrollDto, @RequestHeader("Authorization") String auth) {
        if (ObjectUtils.isEmpty(auth) || !keycloakTokenUtil.validateToken(auth)) {
            throw new UnauthorizedException(
                    "Unauthorized: Invalid or missing token",
                    "Please provide a valid token in the Authorization header to access this resource."
            );
        }
        return employeePayrollService.initiatePayroll(initiateEmployeePayrollDto, auth);
    }
}
