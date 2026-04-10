package com.nexus.hr.service.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.model.entities.*;
import com.nexus.hr.payload.InitiatePaymentDto;
import com.nexus.hr.payload.InitiatePayrollDto;
import com.nexus.hr.payload.PayComponentDto;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.OrgAccountInfoRepo;
import com.nexus.hr.repository.PayrollRepo;
import com.nexus.hr.service.interfaces.PayrollService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
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
    private final CommonUtils commonUtils;
    private final OrgAccountInfoRepo orgAccountInfoRepo;

    @Override
    public ResponseEntity<?> initiatePayrollForThisMonth(InitiatePayrollDto initiatePayrollDto) {
        if (ObjectUtils.isEmpty(initiatePayrollDto) || ObjectUtils.isEmpty(initiatePayrollDto.getOrg()) || ObjectUtils.isEmpty(initiatePayrollDto.getEmployees())) {
            return new ResponseEntity<>("initiatePayrollDto is empty", HttpStatus.BAD_REQUEST);
        }
        ResponseEntity<?> response;
        try {
            InitiatePaymentDto initiatePaymentDto = new InitiatePaymentDto();
            initiatePaymentDto.setDescription("Salary payment for this month");
            initiatePaymentDto.setPaymentType("SALARY");
            initiatePaymentDto.setCurrency("INR");
            initiatePaymentDto.setTransactionReference("SALARY_PAYMENT_" + generateTransactionReference());

            enrichCustomerDetails(initiatePaymentDto, initiatePayrollDto);
            enrichPaymentMethodDetails(initiatePaymentDto, initiatePayrollDto);
            enrichMerchantDetails(initiatePaymentDto, initiatePayrollDto);
            enrichPaymentComponent(initiatePaymentDto, initiatePayrollDto);

//            RestPayload restPayloadForPayment = commonUtils.buildRestPayload(webConstants.getInitiatePaymentUrl(), null, null, CommonConstants.APPLICATION_JSON);
//            ResponseEntity<?> responseForPayment = restServices.hrRestCall(restPayloadForPayment.getBuilder().toUriString(), initiatePaymentDto, restPayloadForPayment.getHeaders(), HttpMethod.POST, hrEntity.getHrId());
//            if (responseForPayment.getStatusCode().is2xxSuccessful()) {
//                response = new ResponseEntity<>("Payroll initiated successfully for this month", HttpStatus.OK);
//            } else {
//                response = new ResponseEntity<>("Failed to initiate payroll for this month", HttpStatus.INTERNAL_SERVER_ERROR);
//            }

            response = new ResponseEntity<>(initiatePaymentDto, HttpStatus.OK);
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


    private void enrichPaymentMethodDetails(InitiatePaymentDto initiatePaymentDto, InitiatePayrollDto initiatePayrollDto) {
        try {
            InitiatePaymentDto.PaymentMethod paymentMethod = new InitiatePaymentDto.PaymentMethod();

            OrgAccountInfo orgAccountInfo = orgAccountInfoRepo.findByOrgId(Long.valueOf(initiatePayrollDto.getOrg().get("orgId"))).orElseThrow(() -> new ResourceNotFoundException("Org", "orgId", initiatePayrollDto.getOrg().get("orgId")));
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

    private void enrichCustomerDetails(InitiatePaymentDto initiatePaymentDto, InitiatePayrollDto initiatePayrollDto) throws JsonProcessingException {

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

    private void enrichMerchantDetails(InitiatePaymentDto initiatePaymentDto, InitiatePayrollDto initiatePayrollDto) throws JsonProcessingException {
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
                List<BankRecord> bankRecords =
                        hrEntityRepo.findByEmployeeId(Long.valueOf(employee.get("id"))).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "empId", Long.valueOf(employee.get("id"))
                        )).getCompensation().getBankRecords();
                if (ObjectUtils.isEmpty(bankRecords) || bankRecords.isEmpty()) {
                    throw new ServiceLevelException(
                            "PayrollService",
                            "Bank details are empty for merchant member enrichment",
                            "enrichMerchantDetails",
                            "ResourceNotFoundException",
                            "No bank records found for employeeId: " + Long.valueOf(employee.get("id"))
                    );
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
                    e.getMessage()
            );
        }
    }

    private void enrichPaymentComponent(InitiatePaymentDto initiatePaymentDto, InitiatePayrollDto initiatePayrollDto) {
        try {
            List<InitiatePaymentDto.Merchant.MerchantMember> merchantMembers = initiatePaymentDto.getMerchant().getMerchantMembers();
            double totalAmount = 0.0D;

            // Iterate through each employee and create pay components
            for (int i = 0; i < initiatePayrollDto.getEmployees().size(); i++) {
                Map<String, String> employee = initiatePayrollDto.getEmployees().get(i);
                Long empId = Long.valueOf(employee.getOrDefault("id", "0"));

                // Fetch HrEntity for the current employee
                HrEntity currentHrEntity = hrEntityRepo.findByEmployeeId(empId).orElseThrow(() ->
                        new ResourceNotFoundException("HrEntity", "empId", empId));

                // Create pay component for this employee
                PayComponentDto payComponentsDto = new PayComponentDto();
                Compensation compensation = currentHrEntity.getCompensation();

                if (ObjectUtils.isEmpty(compensation)) {
                    throw new ServiceLevelException(
                            "PayrollService",
                            "Compensation details are empty for payment component enrichment",
                            "enrichPaymentComponent",
                            "ResourceNotFoundException",
                            "No compensation details found for employeeId: " + empId
                    );
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
            }

            // Set the total amount as the sum of all employee amounts
            initiatePaymentDto.setAmount(totalAmount);

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

    private Double calculateNormalizedAmount(PayComponentDto payComponentsDto) {
        double calculatedAmount = 0.0D;
        calculatedAmount += payComponentsDto.getBasePay() != null ? payComponentsDto.getBasePay() : 0.0D;
        calculatedAmount += payComponentsDto.getHra() != null ? payComponentsDto.getHra() : 0.0D;
        if (!ObjectUtils.isEmpty(payComponentsDto.getBonuses())) {
            calculatedAmount += payComponentsDto.getBonuses().values().stream().mapToDouble(Double::doubleValue).sum();
        }
        if (!ObjectUtils.isEmpty(payComponentsDto.getDeductions())) {
            calculatedAmount -= payComponentsDto.getDeductions().values().stream().mapToDouble(Double::doubleValue).sum();
        }
        return calculatedAmount;
    }

    private Double calculateMonthlyPayForComponent(Double component) {
        return component != null ? component / 12 : 0.0D;
    }
}
