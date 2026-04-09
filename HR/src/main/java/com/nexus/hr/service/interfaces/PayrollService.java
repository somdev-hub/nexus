package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.InitiatePayrollDto;
import org.springframework.http.ResponseEntity;

public interface PayrollService {

    ResponseEntity<?> initiatePayrollForThisMonth(InitiatePayrollDto initiatePayrollDto);
}
