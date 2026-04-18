package com.nexus.hr.service.interfaces;

import com.nexus.hr.payload.InitiatePayrollDto;
import com.nexus.hr.payload.PayrollCallbackDto;
import com.nexus.hr.payload.PayrollGraphRequestDto;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PayrollService {

    ResponseEntity<?> initiatePayrollForThisMonth(InitiatePayrollDto initiatePayrollDto);

    ResponseEntity<?> handlePayrollCallback(List<PayrollCallbackDto> body);

    ResponseEntity<?> getPayrollGraphs(PayrollGraphRequestDto requestBody);

    ResponseEntity<?> getPayrollInsights(Long orgId, String month, Integer year);
}
