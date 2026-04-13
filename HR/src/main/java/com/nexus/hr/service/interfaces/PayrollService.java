package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.InitiatePayrollDto;
import com.nexus.hr.payload.PayrollCallbackDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PayrollService {

    ResponseEntity<?> initiatePayrollForThisMonth(InitiatePayrollDto initiatePayrollDto);

    ResponseEntity<?> handlePayrollCallback(List<PayrollCallbackDto> body);
}
