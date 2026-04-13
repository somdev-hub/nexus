package com.nexus.hr.controller;

import com.nexus.hr.annotation.LogActivity;
import com.nexus.hr.exception.UnauthorizedException;
import com.nexus.hr.payload.InitiatePayrollDto;
import com.nexus.hr.payload.PayrollCallbackDto;
import com.nexus.hr.service.interfaces.PayrollService;
import com.nexus.hr.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hr/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final CommonUtils commonUtils;

    @LogActivity("Initiate Monthly Payment")
    @PostMapping("/initiate/monthly")
    public ResponseEntity<?> initiateMonthlyPayment(@RequestBody InitiatePayrollDto initiatePayrollDto, @RequestHeader("Authorization") String authorization) {
        if (ObjectUtils.isEmpty(authorization) || !commonUtils.validateToken(authorization)) {
            throw new UnauthorizedException(
                    "Unauthorized: Invalid or missing token",
                    "Please provide a valid token in the Authorization header to access this resource."
            );
        }
        return payrollService.initiatePayrollForThisMonth(initiatePayrollDto);
    }

    @LogActivity("Payroll callback")
    @PostMapping("/callback")
    public ResponseEntity<?> payrollCallback(@RequestBody List<PayrollCallbackDto> body, @RequestHeader("Authorization") String authorization) {
        if (ObjectUtils.isEmpty(authorization) || !commonUtils.validateToken(authorization)) {
            throw new UnauthorizedException(
                    "Unauthorized: Invalid or missing token",
                    "Please provide a valid token in the Authorization header to access this resource."
            );
        }
        return payrollService.handlePayrollCallback(body);
    }



}
