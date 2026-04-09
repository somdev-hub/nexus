package com.nexus.hr.service.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.payload.InitiatePaymentDto;
import com.nexus.hr.payload.InitiatePayrollDto;
import com.nexus.hr.payload.PayComponentDto;
import com.nexus.hr.payload.RestPayload;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.OrgAccountInfoRepo;
import com.nexus.hr.repository.PayrollRepo;
import com.nexus.hr.service.interfaces.PayrollService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepo payrollRepo;
    private final HrEntityRepo hrEntityRepo;
    private final RestServices restServices;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;
    private final ObjectMapper objectMapper;
    private final OrgAccountInfoRepo orgAccountInfoRepo;

    @Override
    public ResponseEntity<?> initiatePayrollForThisMonth(InitiatePayrollDto initiatePayrollDto) {
        if (ObjectUtils.isEmpty(initiatePayrollDto) || ObjectUtils.isEmpty(initiatePayrollDto.getEmpId()) || ObjectUtils.isEmpty(initiatePayrollDto.getOrgId())) {
            return new ResponseEntity<>("initiatePayrollDto is empty", HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<?> response;
        try {
            HrEntity hrEntity = hrEntityRepo.findByEmployeeId(initiatePayrollDto.getEmpId()).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "empId", initiatePayrollDto.getEmpId()
            ));
            InitiatePaymentDto initiatePaymentDto = new InitiatePaymentDto();
            initiatePaymentDto.setDescription("Salary payment for this month");
            initiatePaymentDto.setPaymentType("SALARY");
            initiatePaymentDto.setTransactionReference("SALARY_PAYMENT_" + generateTransactionReference());

            RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getOrgDetailsUrl(), null, Map.of(1, initiatePayrollDto.getOrgId().toString()), CommonConstants.APPLICATION_JSON);

            ResponseEntity<?> orgDetailsresponse = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null, restPayload.getHeaders(), HttpMethod.GET, hrEntity.getHrId());

            Map<String, String> orgDetails = null;
            if (orgDetailsresponse.getStatusCode().is2xxSuccessful()) {
                orgDetails = objectMapper.readValue(Objects.requireNonNull(orgDetailsresponse.getBody()).toString(), new TypeReference<>() {
                });
            }

            enrichCustomerDetails(initiatePaymentDto, hrEntity, initiatePayrollDto, orgDetails);
            enrichPaymentMethodDetails(initiatePaymentDto, hrEntity, initiatePayrollDto);
            enrichMerchantDetails(initiatePaymentDto, hrEntity, initiatePayrollDto, orgDetails);
            enrichPaymentComponent(initiatePaymentDto, hrEntity, initiatePayrollDto);

            RestPayload restPayloadForPayment = commonUtils.buildRestPayload(webConstants.getInitiatePaymentUrl(), null, null, CommonConstants.APPLICATION_JSON);
            ResponseEntity<?> responseForPayment = restServices.hrRestCall(restPayloadForPayment.getBuilder().toUriString(), initiatePaymentDto, restPayloadForPayment.getHeaders(), HttpMethod.POST, hrEntity.getHrId());
            if (responseForPayment.getStatusCode().is2xxSuccessful()) {
                response = new ResponseEntity<>("Payroll initiated successfully for this month", HttpStatus.OK);
            } else {
                response = new ResponseEntity<>("Failed to initiate payroll for this month", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (RuntimeException | JsonProcessingException e) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to initiate payroll for this month",
                    "initiatePayrollForThisMonth",
                    e.getClass().getSimpleName(),
                    e.getMessage()

            );
        }

        return response;
    }

    private String generateTransactionReference() {
        // APR_2026_uuid
        return LocalDate.now().getMonth().name() + "_" + LocalDate.now().getYear() + "_" + UUID.randomUUID();
    }


    private void enrichPaymentMethodDetails(InitiatePaymentDto initiatePaymentDto, HrEntity hrEntity, InitiatePayrollDto initiatePayrollDto) {
        try {
            InitiatePaymentDto.PaymentMethod paymentMethod = new InitiatePaymentDto.PaymentMethod();

            OrgAccountInfo orgAccountInfo = orgAccountInfoRepo.findByOrgId(initiatePayrollDto.getOrgId()).orElseThrow(() -> new ResourceNotFoundException("Org", "orgId", initiatePayrollDto.getOrgId()));
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
                    e.getMessage()
            );
        }
    }

    private void normalizePayWithAttendance(PayComponentDto payComponentsDto, HrEntity hrEntity) {
        List<TimeManagement> timeManagements = hrEntity.getTimeManagements();
        // calculate for last month all the absences and halfDays and deduct from the base pay

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

    private void normalizePayWithBonusAndDeductions(PayComponentDto payComponentsDto, InitiatePaymentDto initiatePayrollDto) {
        double calculatedAmount = 0.0D;
        calculatedAmount += payComponentsDto.getBasePay() != null ? payComponentsDto.getBasePay() : 0.0D;
        calculatedAmount += payComponentsDto.getHra() != null ? payComponentsDto.getHra() : 0.0D;
        if (!ObjectUtils.isEmpty(payComponentsDto.getBonuses())) {
            calculatedAmount += payComponentsDto.getBonuses().values().stream().mapToDouble(Double::doubleValue).sum();
        }
        if (!ObjectUtils.isEmpty(payComponentsDto.getDeductions())) {
            calculatedAmount -= payComponentsDto.getDeductions().values().stream().mapToDouble(Double::doubleValue).sum();
        }

        initiatePayrollDto.setAmount(calculatedAmount);
    }

    private void enrichPaycomponentsForBonusesAndDeductions(PayComponentDto payComponents, Compensation compensation) {
        List<Bonus> bonuses = compensation.getBonuses();
        List<Deduction> deductions = compensation.getDeductions();

        // iterate and set all bonuses and deductions
        Map<String, Double> bonusesCalculated = new HashMap<>();
        bonuses.forEach(bonus -> {
            if (bonus.getExpiresOn() != null && bonus.getExpiresOn().toLocalDateTime().toLocalDate().isBefore(LocalDate.now())) {
                // skip expired bonuses
                return;
            }
            bonusesCalculated.put(bonus.getBonusType(), calculateMonthlyPayForComponent(bonus.getAmount()));
        });
        payComponents.setBonuses(bonusesCalculated);

        Map<String, Double> deductionsCalculated = new HashMap<>();
        deductions.forEach(deduction -> {
            if (deduction.getExpiresOn() != null && deduction.getExpiresOn().toLocalDateTime().toLocalDate().isBefore(LocalDate.now())) {
                // skip expired deductions
                return;
            }
            deductionsCalculated.put(deduction.getDeductionType(), calculateMonthlyPayForComponent(deduction.getAmount()));
        });
        deductionsCalculated.put("pf", calculateMonthlyPayForComponent(compensation.getPf()));
        deductionsCalculated.put("insurancePremium", calculateMonthlyPayForComponent(compensation.getInsurancePremium()));
        deductionsCalculated.put("gratuity", calculateMonthlyPayForComponent(compensation.getGratuity()));
        payComponents.setDeductions(deductionsCalculated);
    }

    private void enrichCustomerDetails(InitiatePaymentDto initiatePaymentDto, HrEntity hrEntity, InitiatePayrollDto initiatePayrollDto, Map<String, String> orgDetails) throws JsonProcessingException {
        if (ObjectUtils.isEmpty(orgDetails)) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Organization details are empty for payroll initiation",
                    "enrichCustomerDetails",
                    "ResourceNotFoundException",
                    "No organization details found for orgId: " + initiatePayrollDto.getOrgId()
            );
        }

        InitiatePaymentDto.Customer customer = new InitiatePaymentDto.Customer();
        customer.setCustomerName(orgDetails.getOrDefault("orgName", ""));
        customer.setCustomerEmail(orgDetails.getOrDefault("orgEmail", ""));
        customer.setCustomerPhone(orgDetails.getOrDefault("orgPhone", ""));
        customer.setAddressLine1(orgDetails.getOrDefault("addressLine1", ""));
        customer.setAddressLine2(orgDetails.getOrDefault("addressLine2", ""));
        customer.setCity(orgDetails.getOrDefault("city", ""));
        customer.setState(orgDetails.getOrDefault("state", ""));
        customer.setPinCode(orgDetails.getOrDefault("pinCode", ""));
        customer.setCountry(orgDetails.getOrDefault("country", ""));
        customer.setClientMasterId(CommonConstants.CLIENT_MASTER_ID);
        customer.setSourceSystemId(initiatePayrollDto.getOrgId());

        initiatePaymentDto.setCustomer(customer);
    }

    private void enrichMerchantDetails(InitiatePaymentDto initiatePaymentDto, HrEntity hrEntity, InitiatePayrollDto initiatePayrollDto, Map<String, String> orgDetails) throws JsonProcessingException {
        try {
            if (ObjectUtils.isEmpty(orgDetails)) {
                throw new ServiceLevelException(
                        "PayrollService",
                        "Organization details are empty for payroll initiation",
                        "enrichMerchantDetails",
                        "ResourceNotFoundException",
                        "No organization details found for orgId: " + initiatePayrollDto.getOrgId()
                );
            }

            InitiatePaymentDto.Merchant merchant = new InitiatePaymentDto.Merchant();
            merchant.setClientMasterId(CommonConstants.CLIENT_MASTER_ID);
            merchant.setSourceSystemId(initiatePayrollDto.getOrgId());
            merchant.setMerchantOfficialEmail(orgDetails.getOrDefault("orgEmail", ""));
            merchant.setAddressLine1(orgDetails.getOrDefault("addressLine1", ""));
            merchant.setCity(orgDetails.getOrDefault("city", ""));
            merchant.setState(orgDetails.getOrDefault("state", ""));
            merchant.setPinCode(orgDetails.getOrDefault("pinCode", ""));
            merchant.setCountry(orgDetails.getOrDefault("country", ""));

            InitiatePaymentDto.Merchant.MerchantMember merchantMember = new InitiatePaymentDto.Merchant.MerchantMember();
            merchantMember.setSourceMemberId(hrEntity.getEmployeeId());

            RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getUserDetailsUrl(), Map.of("userId", initiatePayrollDto.getEmpId().toString()), null, CommonConstants.APPLICATION_JSON);
            ResponseEntity<?> response = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null, restPayload.getHeaders(), HttpMethod.GET, hrEntity.getHrId());
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, String> userDetails = objectMapper.readValue(Objects.requireNonNull(response.getBody()).toString(), new TypeReference<>() {
                });
                merchantMember.setName(userDetails.getOrDefault("name", ""));
                merchantMember.setEmail(userDetails.getOrDefault("email", ""));
            } else {
                throw new ServiceLevelException(
                        "PayrollService",
                        "Failed to fetch user details for merchant member enrichment",
                        "enrichMerchantDetails",
                        "ResourceNotFoundException",
                        "No user details found for userId: " + initiatePayrollDto.getEmpId()
                );
            }
            List<BankRecord> bankRecords = hrEntity.getCompensation().getBankRecords();
            if (ObjectUtils.isEmpty(bankRecords) || bankRecords.isEmpty()) {
                throw new ServiceLevelException(
                        "PayrollService",
                        "Bank details are empty for merchant member enrichment",
                        "enrichMerchantDetails",
                        "ResourceNotFoundException",
                        "No bank records found for employeeId: " + initiatePayrollDto.getEmpId()
                );
            }
            merchantMember.setBankName(bankRecords.getFirst().getBankName());
            merchantMember.setBankAccountNumber(bankRecords.getFirst().getAccountNumber());
            merchantMember.setBankAccountName(bankRecords.getFirst().getAccountHolderName());
            merchantMember.setIfscCode(bankRecords.getFirst().getIfscCode());
            merchantMember.setBankAccountType(bankRecords.getFirst().getAccountType().name());

            merchant.setMerchantMembers(List.of(merchantMember));
            initiatePaymentDto.setMerchant(merchant);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to enrich merchant details for payroll initiation",
                    "enrichMerchantDetails",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    private void enrichPaymentComponent(InitiatePaymentDto initiatePaymentDto, HrEntity hrEntity, InitiatePayrollDto initiatePayrollDto) {
        try {
//            Map<String, Double> payComponents = new HashMap<>();
            PayComponentDto payComponentsDto = new PayComponentDto();
            Compensation compensation = hrEntity.getCompensation();
            if (ObjectUtils.isEmpty(compensation)) {
                throw new ServiceLevelException(
                        "PayrollService",
                        "Compensation details are empty for payment component enrichment",
                        "enrichPaymentComponent",
                        "ResourceNotFoundException",
                        "No compensation details found for employeeId: " + initiatePayrollDto.getEmpId()
                );
            }

//            payComponents.put("basePay", calculateMonthlyPayForComponent(compensation.getBasePay()));
//            payComponents.put("hra", calculateMonthlyPayForComponent(compensation.getHra()));
            payComponentsDto.setBasePay(calculateMonthlyPayForComponent(compensation.getBasePay()));
            payComponentsDto.setHra(calculateMonthlyPayForComponent(compensation.getHra()));

            enrichPaycomponentsForBonusesAndDeductions(payComponentsDto, compensation);

            normalizePayWithAttendance(payComponentsDto, hrEntity);
            normalizePayWithBonusAndDeductions(payComponentsDto, initiatePaymentDto);

            initiatePaymentDto.setPayComponents(payComponentsDto);

        } catch (RuntimeException e) {
            throw new ServiceLevelException(
                    "PayrollService",
                    "Failed to enrich payment components for payroll initiation",
                    "enrichPaymentComponent",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    private Double calculateMonthlyPayForComponent(Double component) {
        return component != null ? component / 12 : 0.0D;
    }
}
