package com.nexus.hr.controller;

import com.nexus.hr.annotation.LogActivity;
import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.model.entities.EmployeePaycheck;
import com.nexus.hr.service.interfaces.EmployeePaycheckService;
import com.nexus.hr.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hr/employee/paycheck")
public class EmployeePaycheckController {
    private final EmployeePaycheckService employeePaycheckService;
    private final CommonUtils commonUtils;

    @LogActivity("Add Employee Paycheck")
    @PostMapping("/add")
    public ResponseEntity<?> addEmployeePaycheck(@RequestBody EmployeePaycheck employeePaycheck, @RequestHeader("Authorization") String authHeader) {
        if (ObjectUtils.isEmpty(authHeader) || !commonUtils.validateToken(authHeader)) {
            throw new UnauthorizedException(
                    "Unauthorized! Please use credentials",
                    "Unable to validate token"
            );
        }
        return employeePaycheckService.addEmployeePaycheck(employeePaycheck);
    }
}
