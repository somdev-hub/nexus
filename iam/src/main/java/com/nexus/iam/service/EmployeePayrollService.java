package com.nexus.iam.service;

import com.nexus.iam.dto.InitiateEmployeePayrollDto;
import org.springframework.http.ResponseEntity;

public interface EmployeePayrollService {
    ResponseEntity<?> initiatePayroll(InitiateEmployeePayrollDto initiateEmployeePayrollDto, String auth);
}
