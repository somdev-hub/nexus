package com.nexus.hr.service.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.model.enums.PaymentStatus;
import com.nexus.hr.payload.*;
import com.nexus.hr.payload.response.PayrollGraphDto;
import com.nexus.hr.payload.response.PayrollInsightsDto;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.OrgAccountInfoRepo;
import com.nexus.hr.repository.PayrollRepo;
import com.nexus.hr.service.interfaces.CommsService;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.service.interfaces.PayrollService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepo payrollRepo;
    private final HrEntityRepo hrEntityRepo;
    private final CommonUtils commonUtils;
    private final OrgAccountInfoRepo orgAccountInfoRepo;
    private final WebConstants webConstants;
    private final AsyncDocumentService asyncDocumentService;
    private final CommunicationService communicationService;
    private final ObjectMapper objectMapper;
    private final RestServices restServices;
    private final PaymentCompletionHelper paymentCompletionHelper;
    private final CommsService commsService;

    @Override
    @Transactional
    public ResponseEntity<?> initiatePayrollForThisMonth(InitiatePayrollDto initiatePayrollDto) {
        if (ObjectUtils.isEmpty(initiatePayrollDto) || ObjectUtils.isEmpty(initiatePayrollDto.getOrg())
                || ObjectUtils.isEmpty(initiatePayrollDto.getEmployees())) {
            log.warn("Invalid payroll initiation request: empty data");
            return new ResponseEntity<>("initiatePayrollDto is empty", HttpStatus.BAD_REQUEST);
        }

        try {
            log.info("Starting payroll initiation for {} employees from org: {}",
                    initiatePayrollDto.getEmployees().size(),
                    initiatePayrollDto.getOrg().get("orgId"));

            InitiatePaymentDto initiatePaymentDto = new InitiatePaymentDto();
            initiatePaymentDto.setDescription("Salary payment for this month");
            initiatePaymentDto.setPaymentType("SALARY");
            initiatePaymentDto.setCurrency("INR");
            initiatePaymentDto.setTransactionReference("SALARY_PAYMENT_" + generateTransactionReference());

            // Enrich payment details AND persist payrolls to get IDs
            enrichCustomerDetails(initiatePaymentDto, initiatePayrollDto);
            enrichPaymentMethodDetails(initiatePaymentDto, initiatePayrollDto);
            enrichMerchantDetails(initiatePaymentDto, initiatePayrollDto);
            List<Long> payrollIds = enrichPaymentComponent(initiatePaymentDto, initiatePayrollDto);

            log.info("Payment DTO enriched with {} payrolls, total amount: {}",
                    initiatePaymentDto.getMerchant().getMerchantMembers().size(),
                    initiatePaymentDto.getAmount());

            // Set callback with payroll IDs for Kafka publishing to HR
            InitiatePaymentDto.Callback callback = new InitiatePaymentDto.Callback();
            callback.setSourceSystemIds(payrollIds);
            // Kafka topic is hardcoded in PMS: payment-callback-topic
            // No HTTP callback URL needed (we're using Kafka now)
            initiatePaymentDto.setCallback(callback);

            log.info("Set callback with {} payroll IDs for Kafka publishing", payrollIds.size());

            // Build REST call to PMS endpoint
            RestPayload restPayloadForPayment = commonUtils.buildRestPayload(webConstants.getInitiatePaymentUrl(), null,
                    null, CommonConstants.APPLICATION_JSON);

            log.info("Initiating payment call to PMS endpoint: {}", restPayloadForPayment.getBuilder().toUriString());

            // Call PMS endpoint with callback containing payroll IDs
            ResponseEntity<?> responseForPayment = restServices.hrRestCall(
                    restPayloadForPayment.getBuilder().toUriString(),
                    initiatePaymentDto,
                    restPayloadForPayment.getHeaders(),
                    HttpMethod.POST,
                    null);

            // Check if PMS accepted the request
            if (!responseForPayment.getStatusCode().is2xxSuccessful()) {
                log.error("PMS rejected payment initiation with status: {}", responseForPayment.getStatusCode());
                log.error("PMS response body: {}", responseForPayment.getBody());

                // Return error details to client
                String errorMessage = "Failed to initiate payroll: PMS returned " + responseForPayment.getStatusCode();
                if (responseForPayment.getBody() != null) {
                    errorMessage = responseForPayment.getBody().toString();
                }

                return new ResponseEntity<>(errorMessage, responseForPayment.getStatusCode());
            }

            log.info("PMS accepted payment initiation with status: {}", responseForPayment.getStatusCode());

            if (payrollIds.isEmpty()) {
                log.error("No payroll IDs created during enrichment");
                return new ResponseEntity<>("Failed to save payroll records", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            log.info("Payroll initiation successful for {} payrolls: {}", payrollIds.size(), payrollIds);

            return new ResponseEntity<>(Map.of(
                    "message", "Payroll initiated successfully for this month",
                    "status", "PENDING",
                    "payrollIds", payrollIds,
                    "transactionReference", initiatePaymentDto.getTransactionReference()), HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Exception during payroll initiation", e);
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to initiate payroll for this month",
                    "initiatePayrollForThisMonth",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> handlePayrollCallback(List<PayrollCallbackDto> body) {
        if (ObjectUtils.isEmpty(body)) {
            return new ResponseEntity<>("Request body is empty", HttpStatus.BAD_REQUEST);
        }
        try {
            for (PayrollCallbackDto callbackDto : body) {
                Payroll payroll = payrollRepo.findById(callbackDto.getPayrollId())
                        .orElseThrow(() -> new ResourceNotFoundException("Payroll", "payrollId",
                                callbackDto.getPayrollId()));

                payroll.setPaymentReferenceId(callbackDto.getPaymentReferenceId());
                payroll.setPaidOn(new Timestamp(System.currentTimeMillis()));

                if (callbackDto.getSuccess()) {
                    payroll.setPaymentStatus(PaymentStatus.COMPLETED);
                    log.info("Payment completed for payroll ID: {}, reference: {}",
                            payroll.getPayrollId(), callbackDto.getPaymentReferenceId());
                } else {
                    payroll.setPaymentStatus(PaymentStatus.FAILED);
                    log.warn("Payment failed for payroll ID: {}", payroll.getPayrollId());
                }

                payrollRepo.save(payroll);

                // After payment is completed, generate payslip and send notification
                if (callbackDto.getSuccess()) {
                    // Fire async operations in background to avoid blocking callback response
                    // Spring's @Async handles thread pooling and transaction context properly
                    Long payrollId = payroll.getPayrollId();
                    processPaymentCompletionAsync(payrollId);
                }
            }
        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to handle payroll callback",
                    "handlePayrollCallback",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<?> getPayrollGraphs(PayrollGraphRequestDto requestBody) {
        if (ObjectUtils.isEmpty(requestBody)){
            return new ResponseEntity<>("Request body is empty", HttpStatus.BAD_REQUEST);
        }

        if (ObjectUtils.isEmpty(requestBody.getRoleEmpIdMap()) || requestBody.getRoleEmpIdMap().isEmpty()){
            return new ResponseEntity<>("roleEmpIdMap is empty", HttpStatus.BAD_REQUEST);
        }

        if (ObjectUtils.isEmpty(requestBody.getMonth())){
            return new ResponseEntity<>("Month is required", HttpStatus.BAD_REQUEST);
        }

        if (ObjectUtils.isEmpty(requestBody.getOrgId())){
            return new ResponseEntity<>("Organization ID is required", HttpStatus.BAD_REQUEST);
        }

        try{
            log.info("Starting payroll graphs generation for {} roles with month: {}",
                    requestBody.getRoleEmpIdMap().size(), requestBody.getMonth());

            PayrollGraphDto payrollGraphDto = new PayrollGraphDto();
            List<PayrollGraphDto.SalaryVsRoleAggregationDto> salaryVsRoleList = new ArrayList<>();

            String monthName = requestBody.getMonth();
            int year = requestBody.getYear() != null ? requestBody.getYear() : LocalDate.now().getYear();

            // Iterate through each role and aggregate payroll data
            for (PayrollGraphRequestDto.rolesWithEmpIds roleEmpIdMapping : requestBody.getRoleEmpIdMap()) {
                String role = roleEmpIdMapping.getRole();
                List<Long> empIds = roleEmpIdMapping.getEmpIds();

                if (ObjectUtils.isEmpty(empIds) || empIds.isEmpty()) {
                    log.warn("No employee IDs found for role: {}", role);
                    continue;
                }

                log.debug("Processing role: {} with {} employees", role, empIds.size());

                // Call repository to get aggregated payroll data for this role
                Map<String, Object> rawAggregation =
                    payrollRepo.aggregatePayrollByEmpIdsAndMonthAndOrgId(empIds, monthName, year, requestBody.getOrgId());

                if (rawAggregation != null) {
                    // Convert map to DTO
                    Double baseSalary = rawAggregation.get("baseSalary") != null
                        ? ((Number) rawAggregation.get("baseSalary")).doubleValue()
                        : 0.0;
                    Double bonus = rawAggregation.get("bonus") != null
                        ? ((Number) rawAggregation.get("bonus")).doubleValue()
                        : 0.0;
                    Long employeeCount = rawAggregation.get("employeeCount") != null
                        ? ((Number) rawAggregation.get("employeeCount")).longValue()
                        : 0L;

                    PayrollGraphDto.SalaryVsRoleAggregationDto aggregation =
                        new PayrollGraphDto.SalaryVsRoleAggregationDto(role, baseSalary, bonus, employeeCount);

                    log.info("Role: {}, BaseSalary: {}, Bonus: {}, EmployeeCount: {}",
                            role, baseSalary, bonus, employeeCount);

                    salaryVsRoleList.add(aggregation);
                } else {
                    log.warn("No payroll data found for role: {} in month: {} year: {}", role, monthName, year);
                    // Add empty aggregation for consistency
                    PayrollGraphDto.SalaryVsRoleAggregationDto emptyAggregation =
                        new PayrollGraphDto.SalaryVsRoleAggregationDto(role, 0.0, 0.0, 0L);
                    salaryVsRoleList.add(emptyAggregation);
                }
            }

            payrollGraphDto.setSalaryVsRole(salaryVsRoleList);

            // Fetch last 6 months salary vs overtime data
            log.info("Fetching last 6 months salary vs overtime data for orgId: {}", requestBody.getOrgId());
            int currentYear = LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
            String currentMonth = LocalDate.now(ZoneId.of("Asia/Kolkata")).getMonth().name();

            List<Map<String, Object>> rawOvertimeResults = payrollRepo.getLast6MonthsSalaryVsOvertimeRaw(
                    requestBody.getOrgId(),
                    currentYear,
                    currentMonth
            );

            if (!ObjectUtils.isEmpty(rawOvertimeResults)) {
                // Convert raw results to DTOs
                List<PayrollGraphDto.SalaryVsOvertimeDto> overtimeResults = rawOvertimeResults.stream()
                        .map(row -> new PayrollGraphDto.SalaryVsOvertimeDto(
                                (String) row.get("month"),
                                ((Number) row.get("year")).intValue(),
                                ((Number) row.get("totalSalary")).doubleValue(),
                                ((Number) row.get("overtimePay")).doubleValue(),
                                ((Number) row.get("employeeCount")).longValue()
                        ))
                        .toList();

                payrollGraphDto.setSalaryVsOvertime(overtimeResults);
                log.info("Added {} months of salary vs overtime data", overtimeResults.size());
            }

            // Fetch department-wise payroll data
            log.info("Fetching department-wise payroll data for orgId: {}, month: {}, year: {}",
                    requestBody.getOrgId(), monthName, year);

            List<Map<String, Object>> rawDeptResults = payrollRepo.getDeptWisePayrollRaw(
                    requestBody.getOrgId(),
                    monthName,
                    year
            );

            if (!ObjectUtils.isEmpty(rawDeptResults)) {
                // Convert raw results to DTOs
                List<PayrollGraphDto.SalaryVsDeptDto> deptResults = rawDeptResults.stream()
                        .map(row -> new PayrollGraphDto.SalaryVsDeptDto(
                                (String) row.get("dept"),
                                ((Number) row.get("baseSalary")).doubleValue(),
                                ((Number) row.get("bonus")).doubleValue()
                        ))
                        .toList();

                payrollGraphDto.setSalaryVsDept(deptResults);
                log.info("Added {} departments payroll data", deptResults.size());
            }

            // Fetch status-wise payroll count
            log.info("Fetching status-wise payroll count for orgId: {}, month: {}, year: {}",
                    requestBody.getOrgId(), monthName, year);

            List<Map<String, Object>> rawStatusResults = payrollRepo.getStatusWisePayrollCountRaw(
                    requestBody.getOrgId(),
                    monthName,
                    year
            );

            if (!ObjectUtils.isEmpty(rawStatusResults)) {
                // Convert raw results to DTOs
                List<PayrollGraphDto.SalaryVsStatusDto> statusResults = rawStatusResults.stream()
                        .map(row -> new PayrollGraphDto.SalaryVsStatusDto(
                                (String) row.get("status"),
                                ((Number) row.get("noOfPayrolls")).longValue()
                        ))
                        .toList();

                payrollGraphDto.setSalaryVsStatus(statusResults);
                log.info("Added {} status-wise payroll data", statusResults.size());
            }

            // Fetch salary component breakdown
            log.info("Fetching salary component breakdown for orgId: {}, month: {}, year: {}",
                    requestBody.getOrgId(), monthName, year);

            Map<String, Object> rawComponentResult = payrollRepo.getSalaryComponentBreakdownRaw(
                    requestBody.getOrgId(),
                    monthName,
                    year
            );

            if (!ObjectUtils.isEmpty(rawComponentResult)) {
                // Convert raw result to DTO
                PayrollGraphDto.SalaryVsComponentDto componentResult = new PayrollGraphDto.SalaryVsComponentDto(
                        ((Number) rawComponentResult.get("baseSalary")).doubleValue(),
                        ((Number) rawComponentResult.get("bonus")).doubleValue(),
                        ((Number) rawComponentResult.get("deduction")).doubleValue()
                );

                payrollGraphDto.setSalaryVsComponent(componentResult);
                log.info("Added salary component breakdown - BaseSalary: {}, Bonus: {}, Deduction: {}",
                        componentResult.getBaseSalary(),
                        componentResult.getBonus(),
                        componentResult.getDeduction());
            }

            log.info("Payroll graphs generated successfully for {} roles", salaryVsRoleList.size());

            return new ResponseEntity<>(payrollGraphDto, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error("Invalid month format in request: {}", requestBody.getMonth());
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to get payroll graphs",
                    "getPayrollGraphs",
                    "InvalidMonthFormat",
                    "Invalid month: " + requestBody.getMonth() + ". Expected format: JANUARY, FEBRUARY, etc.");
        } catch (RuntimeException e) {
            log.error("Error while processing payroll graphs", e);
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to get payroll graphs",
                    "getPayrollGraphs",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }

    }

    @Override
    public ResponseEntity<?> getPayrollInsights(Long orgId, String month, Integer year) {
        if (ObjectUtils.isEmpty(orgId) || ObjectUtils.isEmpty(month) || ObjectUtils.isEmpty(year)) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to get payroll insights due to missing parameters",
                    "getPayrollInsights",
                    "MissingParameters",
                    "Organization ID, month, and year are required parameters for fetching payroll insights."
            );
        }

        try {
            log.info("Fetching payroll insights for orgId: {}, month: {}, year: {}", orgId, month, year);

            PayrollInsightsDto payrollInsightsDto = new PayrollInsightsDto();

            // Fetch main insights data: totalNetSalaries, totalProcessedSalaries, totalPendingSalaries, etc.
            log.debug("Querying payroll insights data from repository");
            Map<String, Object> rawInsightsData = payrollRepo.getPayrollInsightsRaw(orgId, month, year);

            if (!ObjectUtils.isEmpty(rawInsightsData)) {
                // Extract and set all fields from raw data
                payrollInsightsDto.setTotalNetSalaries(
                    rawInsightsData.get("totalNetSalaries") != null
                        ? ((Number) rawInsightsData.get("totalNetSalaries")).doubleValue()
                        : 0.0
                );

                payrollInsightsDto.setTotalProcessedSalaries(
                    rawInsightsData.get("totalProcessedSalaries") != null
                        ? ((Number) rawInsightsData.get("totalProcessedSalaries")).doubleValue()
                        : 0.0
                );

                payrollInsightsDto.setTotalPendingSalaries(
                    rawInsightsData.get("totalPendingSalaries") != null
                        ? ((Number) rawInsightsData.get("totalPendingSalaries")).doubleValue()
                        : 0.0
                );

                payrollInsightsDto.setTotalPayrollCost(
                    rawInsightsData.get("totalPayrollCost") != null
                        ? ((Number) rawInsightsData.get("totalPayrollCost")).doubleValue()
                        : 0.0
                );

                payrollInsightsDto.setAverageNetSalaryPerEmployee(
                    rawInsightsData.get("averageNetSalaryPerEmployee") != null
                        ? ((Number) rawInsightsData.get("averageNetSalaryPerEmployee")).doubleValue()
                        : 0.0
                );

                payrollInsightsDto.setTotalDeductions(
                    rawInsightsData.get("totalDeductions") != null
                        ? ((Number) rawInsightsData.get("totalDeductions")).doubleValue()
                        : 0.0
                );

                payrollInsightsDto.setTotalOvertimeCost(
                    rawInsightsData.get("totalOvertimeCost") != null
                        ? ((Number) rawInsightsData.get("totalOvertimeCost")).doubleValue()
                        : 0.0
                );

                log.debug("Main insights data fetched - TotalNetSalaries: {}, TotalPayrollCost: {}",
                        payrollInsightsDto.getTotalNetSalaries(),
                        payrollInsightsDto.getTotalPayrollCost());
            }

            // Fetch total not processed salaries (for employees without payroll in this month)
            log.debug("Querying not processed salaries for org: {}, month: {}, year: {}", orgId, month, year);
            Map<String, Object> rawNotProcessedData = payrollRepo.getTotalNotProcessedSalariesRaw(orgId, month, year);

            if (!ObjectUtils.isEmpty(rawNotProcessedData)) {
                payrollInsightsDto.setTotalNotProcessedSalaries(
                    rawNotProcessedData.get("totalNotProcessedSalaries") != null
                        ? ((Number) rawNotProcessedData.get("totalNotProcessedSalaries")).doubleValue()
                        : 0.0
                );

                log.debug("Not processed salaries fetched: {}", payrollInsightsDto.getTotalNotProcessedSalaries());
            }

            log.info("Payroll insights generated successfully for orgId: {}, month: {}, year: {}",
                    orgId, month, year);

            return new ResponseEntity<>(payrollInsightsDto, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for payroll insights - month: {}, year: {}", month, year);
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to get payroll insights",
                    "getPayrollInsights",
                    "InvalidParameters",
                    "Invalid month or year format: " + e.getMessage()
            );
        } catch (RuntimeException e) {
            log.error("Error while fetching payroll insights for orgId: {}, month: {}, year: {}", orgId, month, year, e);
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to get payroll insights",
                    "getPayrollInsights",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    private String generateTransactionReference() {
        // APR_2026_uuid
        return LocalDate.now(ZoneId.of("Asia/Kolkata")).getMonth().name() + "_" + LocalDate.now().getYear() + "_" + UUID.randomUUID();
    }

    private void enrichPaymentMethodDetails(InitiatePaymentDto initiatePaymentDto,
            InitiatePayrollDto initiatePayrollDto) {
        try {
            InitiatePaymentDto.PaymentMethod paymentMethod = new InitiatePaymentDto.PaymentMethod();

            OrgAccountInfo orgAccountInfo = orgAccountInfoRepo
                    .findByOrgId(Long.valueOf(initiatePayrollDto.getOrg().get("orgId")))
                    .orElseThrow(() -> new ResourceNotFoundException("Org", "orgId",
                            initiatePayrollDto.getOrg().get("orgId")));
            paymentMethod.setBankName(orgAccountInfo.getBankName());
            paymentMethod.setBankAccountHolderName(orgAccountInfo.getBankAccountName());
            paymentMethod.setBankAccountNumber(orgAccountInfo.getBankAccountNumber());
            paymentMethod.setBankIfscCode(orgAccountInfo.getBankIfscCode());
            paymentMethod.setPaymentMethod(CommonConstants.PAYMENT_TYPE_NET_BANKING);

            initiatePaymentDto.setPaymentMethod(paymentMethod);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to enrich payment components for payroll initiation",
                    "enrichPaymentComponent",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    private void normalizePayWithAttendance(PayComponentDto payComponentsDto, HrEntity hrEntity) {
        List<TimeManagement> timeManagements = hrEntity.getTimeManagements();
        // calculate for last month all the absences and halfDays and deduct from the
        // base pay

        if (ObjectUtils.isEmpty(timeManagements)) {
            return;
        }

        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        int lastMonthValue = lastMonth.getMonthValue();
        int lastYearValue = lastMonth.getYear();

        // Filter time management records for last month
        List<TimeManagement> lastMonthRecords = timeManagements.stream()
                .filter(tm -> tm.getMonth() != null && tm.getMonth().equals(lastMonthValue) &&
                        tm.getYear() != null && tm.getYear().equals(lastYearValue))
                .toList();

        // Count absences (not present and not on leave)
        long absences = lastMonthRecords.stream()
                .filter(tm -> !Boolean.TRUE.equals(tm.getIsPresent()) && !Boolean.TRUE.equals(tm.getIsOnLeave()))
                .count();

        // Count half days
        long halfDays = lastMonthRecords.stream()
                .filter(tm -> Boolean.TRUE.equals(tm.getIsHalfDay()))
                .count();

        Double basePay = payComponentsDto.getBasePay();
        if (basePay != null && basePay > 0) {
            // Calculate daily rate (assuming 30 days in a month)
            double dailyRate = basePay / 30.0;

            // Calculate deductions for absences (full day)
            double absenceDeduction = absences * dailyRate;

            // Calculate deductions for half days (0.5 day)
            double halfDayDeduction = halfDays * (dailyRate / 2.0);

            // Update base pay with deductions
            double adjustedBasePay = basePay - absenceDeduction - halfDayDeduction;
            payComponentsDto.setBasePay(Math.max(adjustedBasePay, 0.0)); // Ensure base pay doesn't go negative
        }
    }

    private void enrichPaycomponentsForBonusesAndDeductions(PayComponentDto payComponents, Compensation compensation) {
        List<Bonus> bonuses = compensation.getBonuses();
        List<Deduction> deductions = compensation.getDeductions();

        // iterate and set all bonuses and deductions
        Map<String, Double> bonusesCalculated = new HashMap<>();
        bonuses.forEach(bonus -> {
            if (bonus.getExpiresOn() != null
                    && bonus.getExpiresOn().toLocalDateTime().toLocalDate().isBefore(LocalDate.now())) {
                // skip expired bonuses
                return;
            }
            bonusesCalculated.put(bonus.getBonusType(), calculateMonthlyPayForComponent(bonus.getAmount()));
        });
        payComponents.setBonuses(bonusesCalculated);

        Map<String, Double> deductionsCalculated = new HashMap<>();
        deductions.forEach(deduction -> {
            if (deduction.getExpiresOn() != null
                    && deduction.getExpiresOn().toLocalDateTime().toLocalDate().isBefore(LocalDate.now())) {
                // skip expired deductions
                return;
            }
            deductionsCalculated.put(deduction.getDeductionType(),
                    calculateMonthlyPayForComponent(deduction.getAmount()));
        });
        deductionsCalculated.put("pf", calculateMonthlyPayForComponent(compensation.getPf()));
        deductionsCalculated.put("insurancePremium",
                calculateMonthlyPayForComponent(compensation.getInsurancePremium()));
        deductionsCalculated.put("gratuity", calculateMonthlyPayForComponent(compensation.getGratuity()));
        payComponents.setDeductions(deductionsCalculated);
    }

    private void enrichCustomerDetails(InitiatePaymentDto initiatePaymentDto, InitiatePayrollDto initiatePayrollDto)
            throws JsonProcessingException {

        InitiatePaymentDto.Customer customer = new InitiatePaymentDto.Customer();
        customer.setCustomerName(initiatePayrollDto.getOrg().getOrDefault("orgName", ""));
        customer.setCustomerEmail(initiatePayrollDto.getOrg().getOrDefault("orgEmail", ""));
        customer.setCustomerPhone(initiatePayrollDto.getOrg().getOrDefault("orgPhone", ""));
        customer.setAddressLine1(initiatePayrollDto.getOrg().getOrDefault("addressLine1", ""));
        customer.setAddressLine2(initiatePayrollDto.getOrg().getOrDefault("addressLine2", ""));
        customer.setCity(initiatePayrollDto.getOrg().getOrDefault("city", ""));
        customer.setState(initiatePayrollDto.getOrg().getOrDefault("state", ""));
        customer.setPinCode(initiatePayrollDto.getOrg().getOrDefault("pinCode", ""));
        customer.setCountry(initiatePayrollDto.getOrg().getOrDefault("country", ""));
        customer.setClientMasterId(CommonConstants.CLIENT_MASTER_ID);
        customer.setSourceSystemId(Long.valueOf(initiatePayrollDto.getOrg().get("orgId")));

        initiatePaymentDto.setCustomer(customer);
    }

    private void enrichMerchantDetails(InitiatePaymentDto initiatePaymentDto, InitiatePayrollDto initiatePayrollDto)
            throws JsonProcessingException {
        try {

            InitiatePaymentDto.Merchant merchant = new InitiatePaymentDto.Merchant();
            merchant.setClientMasterId(CommonConstants.CLIENT_MASTER_ID);
            merchant.setSourceSystemId(Long.valueOf(initiatePayrollDto.getOrg().get("orgId")));
            merchant.setMerchantOfficialEmail(initiatePayrollDto.getOrg().getOrDefault("orgEmail", ""));
            merchant.setAddressLine1(initiatePayrollDto.getOrg().getOrDefault("addressLine1", ""));
            merchant.setCity(initiatePayrollDto.getOrg().getOrDefault("city", ""));
            merchant.setState(initiatePayrollDto.getOrg().getOrDefault("state", ""));
            merchant.setPinCode(initiatePayrollDto.getOrg().getOrDefault("pinCode", ""));
            merchant.setCountry(initiatePayrollDto.getOrg().getOrDefault("country", ""));

            List<InitiatePaymentDto.Merchant.MerchantMember> merchantMembers = new ArrayList<>();
            for (Map<String, String> employee : initiatePayrollDto.getEmployees()) {

                InitiatePaymentDto.Merchant.MerchantMember merchantMember = new InitiatePaymentDto.Merchant.MerchantMember();
                merchantMember.setSourceMemberId(Long.valueOf(employee.getOrDefault("id", "0L")));
                merchantMember.setName(employee.getOrDefault("name", ""));
                merchantMember.setEmail(employee.getOrDefault("email", ""));
                List<BankRecord> bankRecords = hrEntityRepo.findByEmployeeId(Long.valueOf(employee.get("id")))
                        .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "empId",
                                Long.valueOf(employee.get("id"))))
                        .getCompensation().getBankRecords();
                if (ObjectUtils.isEmpty(bankRecords) || bankRecords.isEmpty()) {
                    throw new ServiceLevelException(
                            "PayrollService",
                            "Bank details are empty for merchant member enrichment",
                            "enrichMerchantDetails",
                            "ResourceNotFoundException",
                            "No bank records found for employeeId: " + Long.valueOf(employee.get("id")));
                }
                merchantMember.setBankName(bankRecords.getFirst().getBankName());
                merchantMember.setBankAccountNumber(bankRecords.getFirst().getAccountNumber());
                merchantMember.setBankAccountName(bankRecords.getFirst().getAccountHolderName());
                merchantMember.setIfscCode(bankRecords.getFirst().getIfscCode());
                merchantMember.setBankAccountType(bankRecords.getFirst().getAccountType().name());

                merchantMembers.add(merchantMember);
            }

            merchant.setMerchantMembers(merchantMembers);
            initiatePaymentDto.setMerchant(merchant);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to enrich merchant details for payroll initiation",
                    "enrichMerchantDetails",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    private List<Long> enrichPaymentComponent(InitiatePaymentDto initiatePaymentDto,
            InitiatePayrollDto initiatePayrollDto) {
        try {
            List<InitiatePaymentDto.Merchant.MerchantMember> merchantMembers = initiatePaymentDto.getMerchant()
                    .getMerchantMembers();
            List<PayComponentDto> payComponentsList = new ArrayList<>();
            double totalAmount = 0.0D;

            log.info("Enriching payment components for {} employees", initiatePayrollDto.getEmployees().size());

            // Iterate through each employee and create pay components
            for (int i = 0; i < initiatePayrollDto.getEmployees().size(); i++) {
                Map<String, String> employee = initiatePayrollDto.getEmployees().get(i);
                Long empId = Long.valueOf(employee.getOrDefault("id", "0"));

                // Fetch HrEntity for the current employee
                HrEntity currentHrEntity = hrEntityRepo.findByEmployeeId(empId)
                        .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "empId", empId));

                // Create pay component for this employee
                PayComponentDto payComponentsDto = new PayComponentDto();
                Compensation compensation = currentHrEntity.getCompensation();

                if (ObjectUtils.isEmpty(compensation)) {
                    throw new ServiceLevelException(
                            "PayrollService",
                            "Compensation details are empty for payment component enrichment",
                            "enrichPaymentComponent",
                            "ResourceNotFoundException",
                            "No compensation details found for employeeId: " + empId);
                }

                payComponentsDto.setBasePay(calculateMonthlyPayForComponent(compensation.getBasePay()));
                payComponentsDto.setHra(calculateMonthlyPayForComponent(compensation.getHra()));

                enrichPaycomponentsForBonusesAndDeductions(payComponentsDto, compensation);

                if (commonUtils.isWiredOn("NORMALIZE-PAY-WITH-ATTENDANCE")) {
                    normalizePayWithAttendance(payComponentsDto, currentHrEntity);
                }

                // Calculate normalized amount for this employee
                double employeeAmount = calculateNormalizedAmount(payComponentsDto);
                totalAmount += employeeAmount;

                // Set pay components and total amount for this merchant member
                merchantMembers.get(i).setPayComponents(payComponentsDto);
                merchantMembers.get(i).setTotalAmountReceivable(employeeAmount);

                // Add to list for persistence
                payComponentsList.add(payComponentsDto);
            }

            log.info("Payment components enriched. Total amount: {}", totalAmount);

            // Persist payroll details with PENDING status AND collect payroll IDs
            List<Long> savedPayrollIds = persistPayrollDetails(initiatePayrollDto.getEmployees(), initiatePaymentDto,
                    payComponentsList);

            log.info("Payroll records persisted with IDs: {}", savedPayrollIds);

            // Set the total amount as the sum of all employee amounts
            initiatePaymentDto.setAmount(totalAmount);

            // NOTE: We are NOT setting callback URL here anymore - we're using Kafka
            // callbacks now
            // PMS will publish callbacks to Kafka topic "payment-callback-topic"
            // HR will consume messages from that topic via PaymentCallbackListener
            log.info("Callback will be handled via Kafka topic: payment-callback-topic");

            // Return the payroll IDs for the main method to use
            return savedPayrollIds;

        } catch (RuntimeException e) {
            log.error("Error enriching payment components", e);
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to enrich payment components for payroll initiation",
                    "enrichPaymentComponent",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    private Double calculateNormalizedAmount(PayComponentDto payComponentsDto) {
        double calculatedAmount = 0.0D;
        calculatedAmount += payComponentsDto.getBasePay() != null ? payComponentsDto.getBasePay() : 0.0D;
        calculatedAmount += payComponentsDto.getHra() != null ? payComponentsDto.getHra() : 0.0D;
        if (!ObjectUtils.isEmpty(payComponentsDto.getBonuses())) {
            calculatedAmount += payComponentsDto.getBonuses().values().stream().mapToDouble(Double::doubleValue).sum();
        }
        if (!ObjectUtils.isEmpty(payComponentsDto.getDeductions())) {
            calculatedAmount -= payComponentsDto.getDeductions().values().stream().mapToDouble(Double::doubleValue)
                    .sum();
        }
        return calculatedAmount;
    }

    private Double calculateMonthlyPayForComponent(Double component) {
        return component != null ? component / 12 : 0.0D;
    }

    private List<Long> persistPayrollDetails(List<Map<String, String>> employees, InitiatePaymentDto initiatePaymentDto,
            List<PayComponentDto> payComponentsList) {
        try {
            List<Payroll> payrollsToSave = new ArrayList<>();

            log.info("Starting payroll persistence for {} employees", employees.size());

            for (int i = 0; i < employees.size(); i++) {
                Map<String, String> employee = employees.get(i);
                Long empId = Long.valueOf(employee.getOrDefault("id", "0"));

                HrEntity hrEntity = hrEntityRepo.findByEmployeeId(empId)
                        .orElseThrow(() -> new ResourceNotFoundException("HrEntity", "empId", empId));

                PayComponentDto payComponentDto = payComponentsList.get(i);
                Compensation compensation = hrEntity.getCompensation();

                // Create Payroll entity
                Payroll payroll = new Payroll();
                payroll.setMonth(LocalDate.now().getMonth().name());
                payroll.setYear(LocalDate.now().getYear());
                payroll.setBasePay(payComponentDto.getBasePay());
                payroll.setHra(payComponentDto.getHra());
                payroll.setPaymentStatus(PaymentStatus.PENDING);
                payroll.setCompensation(compensation);

                // Calculate totals
                double totalBonuses = 0.0D;
                if (!ObjectUtils.isEmpty(payComponentDto.getBonuses())) {
                    totalBonuses = payComponentDto.getBonuses().values().stream().mapToDouble(Double::doubleValue)
                            .sum();
                }
                payroll.setTotalBonuses(totalBonuses);

                double totalDeductions = 0.0D;
                if (!ObjectUtils.isEmpty(payComponentDto.getDeductions())) {
                    totalDeductions = payComponentDto.getDeductions().values().stream().mapToDouble(Double::doubleValue)
                            .sum();
                }
                payroll.setTotalDeductions(totalDeductions);

                // Calculate net pay and gross pay
                double grossPay = payroll.getBasePay() + payroll.getHra() + totalBonuses;
                double netPay = grossPay - totalDeductions;

                payroll.setGrossPay(grossPay);
                payroll.setNetPay(netPay);

                // Create and associate PayrollBonuses
                List<PayrollBonuses> payrollBonusesList = new ArrayList<>();
                if (!ObjectUtils.isEmpty(payComponentDto.getBonuses())) {
                    payComponentDto.getBonuses().forEach((bonusType, amount) -> {
                        PayrollBonuses payrollBonus = new PayrollBonuses();
                        payrollBonus.setBonusType(bonusType);
                        payrollBonus.setAmount(amount);
                        payrollBonus.setIsActive(true);
                        payrollBonus.setPayroll(payroll);
                        payrollBonusesList.add(payrollBonus);
                    });
                }
                payroll.setPayrollBonuses(payrollBonusesList);

                // Create and associate PayrollDeductions
                List<PayrollDeductions> payrollDeductionsList = new ArrayList<>();
                if (!ObjectUtils.isEmpty(payComponentDto.getDeductions())) {
                    payComponentDto.getDeductions().forEach((deductionType, amount) -> {
                        PayrollDeductions payrollDeduction = new PayrollDeductions();
                        payrollDeduction.setDeductionType(deductionType);
                        payrollDeduction.setAmount(amount);
                        payrollDeduction.setDescription("Deduction for " + deductionType + " in " + payroll.getMonth()
                                + " " + payroll.getYear());
                        payrollDeduction.setIsActive(true);
                        payrollDeduction.setPayroll(payroll);
                        payrollDeductionsList.add(payrollDeduction);
                    });
                }
                payroll.setPayrollDeductions(payrollDeductionsList);

                payrollsToSave.add(payroll);
            }

            // Persist all payrolls
            List<Payroll> savedPayrolls = payrollRepo.saveAll(payrollsToSave);
            List<Long> payrollIds = savedPayrolls.stream().map(Payroll::getPayrollId).toList();

            log.info("Successfully persisted {} payroll records with IDs: {}", savedPayrolls.size(), payrollIds);

            // Extract source system IDs (payroll IDs) for Kafka callback
            List<Long> sourceSystemIds = savedPayrolls.stream().map(Payroll::getPayrollId).toList();

            // Note: Callback configuration removed - we're using Kafka now
            // PMS will publish to "payment-callback-topic" with sourceSystemIds in the
            // message
            log.info("Payroll records ready for Kafka callback via topic: payment-callback-topic");

            // Return the payroll IDs for reference
            return payrollIds;

        } catch (RuntimeException e) {
            log.error("Error persisting payroll details", e);
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to persist payroll details",
                    "persistPayrollDetails",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    /**
     * Build PayslipDto from completion data (not entities)
     * This avoids lazy-loading issues since we're working with POJOs
     */
    private PayslipDto buildPayslipDtoFromData(PaymentCompletionHelper.PaymentCompletionData data) {
        PayslipDto payslipDto = PayslipDto.builder()
                .employeeId(data.employeeId())
                .employeeName(data.employeeId().toString())
                .position(data.position())
                .department(data.department())
                .organizationName(data.organization())
                .organizationAddress(data.orgAddress())
                .payrollId(data.payrollId())
                .month(data.month())
                .year(data.year())
                .generatedDate(LocalDateTime.now())
                .basePay(data.basePay())
                .hra(data.hra())
                .totalBonuses(data.totalBonuses())
                .totalDeductions(data.totalDeductions())
                .grossPay(data.grossPay())
                .netPay(data.netPay())
                .paymentReferenceId(data.paymentReferenceId())
                .transactionReference(data.paymentReferenceId())
                .build();

        payslipDto.setBonuses(data.bonuses());
        payslipDto.setDeductions(data.deductions());
        payslipDto.setBankName(data.bankName());
        payslipDto.setAccountHolderName(data.accountHolderName());
        payslipDto.setIfscCode(data.ifscCode());
        payslipDto.setMaskedAccountNumber(data.maskedAccountNumber());

        return payslipDto;
    }

    /**
     * Process payment completion asynchronously
     * Generates payslip and sends email notification to employee
     * This runs in a background thread via Spring's thread pool
     * 
     * NOTE: This method is NOT @Transactional
     * Instead, it calls separate @Transactional helper methods
     * This follows the proven pattern from HrServiceImpl (async + separate
     * transactional helpers)
     */
    @Async
    public void processPaymentCompletionAsync(Long payrollId) {
        try {
            log.info("Starting async payment completion processing for payroll ID: {}", payrollId);

            // Fetch payroll data in a transactional context via helper service
            PaymentCompletionHelper.PaymentCompletionData completionData = paymentCompletionHelper
                    .fetchPaymentCompletionData(payrollId);

            if (ObjectUtils.isEmpty(completionData)) {
                log.warn("Could not fetch payment completion data for payroll ID: {}", payrollId);
                return;
            }

            // Generate payslip using the completion data
            log.info("Generating payslip for employee: {}, payroll: {}",
                    completionData.employeeId(), payrollId);
            PayslipDto payslipDto = buildPayslipDtoFromData(completionData);

            // Generate and upload payslip to DMS
            CompletableFuture<AsyncDocumentService.DocumentResult> payslipFuture = asyncDocumentService
                    .generateAndUploadPayslip(payslipDto, completionData.employeeId(), completionData.hrId());

            // Wait for payslip generation
            AsyncDocumentService.DocumentResult payslipResult = payslipFuture.join();

            if (payslipResult.isSuccess()) {
                log.info("Payslip generated and uploaded successfully for employee: {}",
                        completionData.employeeId());

                // Save payslip link in a transactional context via helper service
                paymentCompletionHelper.linkPayslipToPayroll(payrollId, payslipResult);

                // Send email notification with payslip
				CommsPayload commsPayload = new CommsPayload();
				commsPayload.setHrId(completionData.hrId());
				commsPayload.setPayrollId(payrollId);
                commsService.sendCommunication(CommonConstants.CommsTriggerPoint.PAYROLL_MONTHLY, List.of(new EmailAttachmentDto(
                        "Payslip_" + completionData.month() + "_" + completionData.year() + ".pdf",
                        "application/pdf",
                        payslipResult.getDocumentUrl())), commsPayload);
//                sendPayslipEmailNotificationAsync(completionData, payslipDto, payslipResult.getDocumentUrl());
            } else {
                log.error("Failed to generate payslip for employee: {}, error: {}",
                        completionData.employeeId(), payslipResult.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error processing payment completion for payroll ID: {}", payrollId, e);
            // Exception is logged; async method handles its own errors
        }
    }

    /**
     * Send payslip email notification via Kafka to CMS microservice
     * Works with PaymentCompletionData (POJOs, not entities)
     * This triggers email delivery to employee with payslip attachment
     * @deprecated - This method is now deprecated in favor of using commsService.sendCommunication() directly with the appropriate trigger point and attachments. The logic for building the email communication has been moved to the commsService implementation to centralize email handling and leverage existing infrastructure.
     */
    @Deprecated
    private void sendPayslipEmailNotificationAsync(PaymentCompletionHelper.PaymentCompletionData data,
            PayslipDto payslipDto, String payslipUrl) {
        try {
            log.info("Sending payslip email notification for employee: {}", data.employeeId());

            // Build email communication DTO
            EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
            emailCommunicationDto.setSenderEmail("noreply@nexushr.com");
            // Note: Get employee email from user service - for now using placeholder
            emailCommunicationDto.setRecipientEmails(List.of("operatorgold69@gmail.com"));
            emailCommunicationDto.setSubject("Your Salary Payslip -" + data.month() + " " + data.year());

            // Build placeholders for email template
            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("employeeName", data.employeeId().toString());
            placeholders.put("month", data.month());
            placeholders.put("year", data.year());

            // Salary Breakdown Details
            placeholders.put("basePay",
                    "₹" + String.format("%.2f", data.basePay() != null ? data.basePay() : 0.0));
            placeholders.put("hra", "₹" + String.format("%.2f", data.hra() != null ? data.hra() : 0.0));

            // Pass both numeric and formatted versions for totalBonuses
            Double totalBonusesValue = data.totalBonuses() != null ? data.totalBonuses() : 0.0;
            placeholders.put("totalBonuses", totalBonusesValue); // Numeric for comparison
            placeholders.put("totalBonusesFormatted",
                    "₹" + String.format("%.2f", totalBonusesValue));

            // Pass both numeric and formatted versions for totalDeductions
            Double totalDeductionsValue = data.totalDeductions() != null ? data.totalDeductions() : 0.0;
            placeholders.put("totalDeductions", totalDeductionsValue); // Numeric for comparison
            placeholders.put("totalDeductionsFormatted",
                    "₹" + String.format("%.2f", totalDeductionsValue));

            placeholders.put("grossAmount",
                    "₹" + String.format("%.2f", data.grossPay() != null ? data.grossPay() : 0.0));
            placeholders.put("netAmount",
                    "₹" + String.format("%.2f", data.netPay() != null ? data.netPay() : 0.0));

            // Payment Details
            placeholders.put("paymentReferenceId",
                    data.paymentReferenceId() != null ? data.paymentReferenceId() : "N/A");
            placeholders.put("paymentDate",
                    data.paymentDate() != null ? data.paymentDate() : "N/A");
            placeholders.put("organizationName", "Nexus Corporation");

            // Bank Details (masked)
            if (!ObjectUtils.isEmpty(data.bankName())) {
                placeholders.put("bankName", data.bankName());
                placeholders.put("accountHolderName",
                        data.accountHolderName() != null ? data.accountHolderName() : "N/A");
                placeholders.put("maskedAccountNumber",
                        data.maskedAccountNumber() != null ? data.maskedAccountNumber() : "****");
                placeholders.put("ifscCode", data.ifscCode() != null ? data.ifscCode() : "N/A");
            }

            emailCommunicationDto.setPlaceholders(placeholders);

            emailCommunicationDto.setBody("");

            // Add payslip as attachment
            emailCommunicationDto.setAttachments(List.of(
                    new EmailAttachmentDto(
                            "Payslip_" + data.month() + "_" + data.year() + ".pdf",
                            "application/pdf",
                            payslipUrl)));

            // Wrap in KafkaMessageDto for CMS consumption
            KafkaMessageDto kafkaMessageDto = new KafkaMessageDto();
            kafkaMessageDto.setTopic(CommonConstants.SALARY_PAYMENT_MAIL_TOPIC);
            kafkaMessageDto.setCommsType("email");
            kafkaMessageDto.setUuid(UUID.randomUUID().toString());
            kafkaMessageDto.setMessage(objectMapper.writeValueAsString(emailCommunicationDto));

            String kafkaMessage = objectMapper.writeValueAsString(kafkaMessageDto);
            log.debug("Publishing payslip email notification to Kafka: {}", kafkaMessage);

            // Publish to Kafka via CommunicationService
            communicationService.sendCommunicationOverKafkaForPayroll(emailCommunicationDto);

            log.info("Payslip email notification published successfully for employee: {}, payroll: {}",
                    data.employeeId(), data.payrollId());

        } catch (Exception e) {
            log.error("Error sending payslip email notification for employee ID: {}", data.employeeId(), e);
            // Don't throw exception - payment processing should not fail due to email
            // notification
        }
    }
}
