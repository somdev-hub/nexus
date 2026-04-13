package com.nexus.hr.service.implementations;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.payload.RestPayload;
import com.nexus.hr.repository.PayrollRepo;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper service for transactional operations during payment completion
 * This separate component ensures @Transactional actually works via Spring
 * proxy
 * (avoids self-invocation issue when called from async methods)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletionHelper {

    private final PayrollRepo payrollRepo;
    private final CommonUtils commonUtils;
    private final WebConstants webConstants;
    private final RestServices restServices;
    private final ObjectMapper objectMapper;

    /**
         * Helper class to hold payment completion data
         * Extracted from entities to avoid passing proxies across transaction
         * boundaries
         */
        public record PaymentCompletionData(Long payrollId, Long employeeId, Long hrId, Double basePay, Double hra,
                                            Double totalBonuses, Double totalDeductions, Double grossPay, Double netPay,
                                            String month, Integer year, Map<String, Double> bonuses,
                                            Map<String, Double> deductions, String bankName, String accountHolderName,
                                            String ifscCode, String maskedAccountNumber, String paymentReferenceId,
                                            String paymentDate, String department, String position, String organization,
                                            String orgAddress) {
    }

    /**
     * TRANSACTIONAL: Fetch payment completion data in a dedicated transaction
     * Extracts only the data we need, avoiding passing entity proxies across
     * boundaries
     * 
     * This is in a separate @Service component so Spring proxy correctly
     * applies @Transactional
     * Calling from async context will properly establish a new transaction
     */
    @Transactional
    public PaymentCompletionData fetchPaymentCompletionData(Long payrollId) {
        try {
            Payroll payroll = payrollRepo.findById(payrollId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payroll", "payrollId", payrollId));

            Compensation compensation = payroll.getCompensation();
            if (ObjectUtils.isEmpty(compensation)) {
                log.warn("Compensation not found for payroll ID: {}", payrollId);
                return null;
            }

            HrEntity hrEntity = compensation.getHrEntity();
            if (ObjectUtils.isEmpty(hrEntity)) {
                log.warn("HR Entity not found for payroll ID: {}", payrollId);
                return null;
            }

            //fetch org details from iam via http call
            RestPayload restPayload = commonUtils.buildRestPayload(
                    webConstants.getOrgDetailsUrl(),
                    null,
                    Map.of(1, hrEntity.getOrg().toString()),
                    MediaType.APPLICATION_JSON_VALUE
            );

            ResponseEntity<?> response = restServices.hrRestCall(
                    restPayload.getBuilder().toUriString(),
                    null,
                    restPayload.getHeaders(),
                    HttpMethod.GET,
                    hrEntity.getHrId()
            );
            String orgName="";
            String orgAddress="";
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                JSONObject jsonObject = new JSONObject(response.getBody());
                String line1 = jsonObject.optString("addressLine1");
                String line2 = jsonObject.optString("addressLine2");
                String city = jsonObject.optString("city");
                String state = jsonObject.optString("state");
                String pinCode = jsonObject.optString("pinCode");
                String country = jsonObject.optString("country");
                orgName = jsonObject.optString("orgName");
                orgAddress = (line1 != null ? line1 : "") +
                             (line2 != null ? line2 : "") +
                             (city != null ? city : "") +
                             (state != null ? state : "") +
                             (country != null ? country : "") +
                             (pinCode != null ? pinCode : "");
            } else {
                log.warn("Failed to fetch organization details for HR ID: {} - Status Code: {}", hrEntity.getHrId(), response.getStatusCode());
            }

            // Extract bonuses
            Map<String, Double> bonusesMap = new HashMap<>();
            if (!ObjectUtils.isEmpty(payroll.getPayrollBonuses())) {
                payroll.getPayrollBonuses().forEach(bonus -> bonusesMap.put(bonus.getBonusType(), bonus.getAmount()));
            }

            // Extract deductions
            Map<String, Double> deductionsMap = new HashMap<>();
            if (!ObjectUtils.isEmpty(payroll.getPayrollDeductions())) {
                payroll.getPayrollDeductions()
                        .forEach(deduction -> deductionsMap.put(deduction.getDeductionType(), deduction.getAmount()));
            }

            // Extract bank details
            String bankName = "";
            String accountHolderName = "";
            String ifscCode = "";
            String maskedAccountNumber = "****";
            if (!ObjectUtils.isEmpty(compensation.getBankRecords()) && !compensation.getBankRecords().isEmpty()) {
                BankRecord bankRecord = compensation.getBankRecords().getFirst();
                bankName = bankRecord.getBankName() != null ? bankRecord.getBankName() : "";
                accountHolderName = bankRecord.getAccountHolderName() != null ? bankRecord.getAccountHolderName() : "";
                ifscCode = bankRecord.getIfscCode() != null ? bankRecord.getIfscCode() : "";
                String accountNumber = bankRecord.getAccountNumber();
                if (accountNumber != null && accountNumber.length() >= 4) {
                    maskedAccountNumber = "****" + accountNumber.substring(accountNumber.length() - 4);
                }
            }

            // Extract position (if available)
            String position = "";
            if (hrEntity.getPositions() != null && !hrEntity.getPositions().isEmpty()) {
                position = hrEntity.getPositions().getLast().getTitle();
            }

            // Extract payment date
            String paymentDate = "";
            if (payroll.getPaidOn() != null) {
                paymentDate = payroll.getPaidOn().toString();
            }

            log.info("Successfully extracted payment completion data for payroll ID: {}", payrollId);

            return new PaymentCompletionData(
                    payroll.getPayrollId(),
                    hrEntity.getEmployeeId(),
                    hrEntity.getHrId(),
                    payroll.getBasePay(),
                    payroll.getHra(),
                    payroll.getTotalBonuses(),
                    payroll.getTotalDeductions(),
                    payroll.getGrossPay(),
                    payroll.getNetPay(),
                    payroll.getMonth(),
                    payroll.getYear(),
                    bonusesMap,
                    deductionsMap,
                    bankName,
                    accountHolderName,
                    ifscCode,
                    maskedAccountNumber,
                    payroll.getPaymentReferenceId(),
                    paymentDate,
                    hrEntity.getDepartment(),
                    position,
                    orgName,
                    orgAddress
                    );

        } catch (Exception e) {
            log.error("Error fetching payment completion data for payroll ID: {}", payrollId, e);
            return null;
        }
    }

    /**
     * TRANSACTIONAL: Link payslip document to payroll record
     * Called from async method; ensures DB update happens in proper transaction
     * 
     * This is in a separate @Service component so Spring proxy correctly
     * applies @Transactional
     */
    @Transactional
    public void linkPayslipToPayroll(Long payrollId, AsyncDocumentService.DocumentResult payslipResult) {
        try {
            // Validate that the document URL is not null or empty
            if (ObjectUtils.isEmpty(payslipResult.getDocumentUrl())) {
                log.warn("Cannot link payslip to payroll ID: {} - Document URL is empty/null", payrollId);
                return;
            }

            Payroll payroll = payrollRepo.findById(payrollId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payroll", "payrollId", payrollId));

            HrDocument payslipDocument = new HrDocument();
            payslipDocument.setDocumentName(payslipResult.getDocumentName());
            payslipDocument.setDocumentUrl(payslipResult.getDocumentUrl());
            payslipDocument.setHrDocumentType("PAYSLIP");

            payroll.setSalarySlip(payslipDocument);
            payrollRepo.save(payroll);
            log.info("Payslip linked to payroll ID: {} with URL: {}", payrollId, payslipResult.getDocumentUrl());
        } catch (Exception e) {
            log.error("Error linking payslip to payroll ID: {}", payrollId, e);
            // Don't rethrow - email notification should not fail if payslip link fails
        }
    }
}
