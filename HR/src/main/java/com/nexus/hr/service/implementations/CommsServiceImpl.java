package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.model.entities.HrCommsConfig;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.entities.Position;
import com.nexus.hr.model.enums.CommType;
import com.nexus.hr.payload.EmailAttachmentDto;
import com.nexus.hr.payload.EmailCommunicationDto;
import com.nexus.hr.repository.HrCommsConfigRepo;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.PositionRepository;
import com.nexus.hr.service.interfaces.CommsService;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.utils.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommsServiceImpl implements CommsService {

    private final HrCommsConfigRepo hrCommsConfigRepo;
    private final HrEntityRepo hrEntityRepo;
    private final PositionRepository positionRepository;
    private final CommunicationService communicationService;
    private final PaymentCompletionHelper paymentCompletionHelper;

    @Override
    public ResponseEntity<?> sendCommunication(CommonConstants.CommsTriggerPoint triggerPoint, Long hrId, List<EmailAttachmentDto> attachments, Long payrollId) {
        try {
            if (ObjectUtils.isEmpty(triggerPoint)) {
                return ResponseEntity.badRequest().body("Trigger point is required");
            }
            HrEntity hrEntity = hrEntityRepo.findById(hrId).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));
//            RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getUserDetailsUrl(),
//                    Map.of("userId", hrEntity.getEmployeeId().toString()), null, CommonConstants.APPLICATION_JSON);
//            ResponseEntity<?> userResponse = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null,
//                    restPayload.getHeaders(), HttpMethod.GET, hrEntity.getHrId());
//            if (!userResponse.getStatusCode().is2xxSuccessful()) {
//                log.error("Failed to fetch user details for hrId: {}. Status code: {}, Response body: {}", hrId, userResponse.getStatusCode(), userResponse.getBody());
//                throw new ResourceNotFoundException("UserDetails", "hrId", "Failed to fetch user details for hrId: " + hrId);
//            }
//            @SuppressWarnings("unchecked")
//            Map<String, Object> userDetails = (Map<String, Object>) userResponse.getBody();
            HrCommsConfig hrCommsConfig = hrCommsConfigRepo.findByCommsTriggerPoint(triggerPoint.name()).orElseThrow(() -> new ResourceNotFoundException("CommsTriggerConfig", "triggerPoint", triggerPoint));
            Map<String, String> eventParamsForComms = buildEventParamsForComms(hrCommsConfig.getCommParams(), hrCommsConfig.getTemplateName(), hrId, payrollId);
            handleCommunicationDelivery(hrCommsConfig.getTemplateName(), hrCommsConfig.getCommType(), eventParamsForComms, hrEntity, attachments);
            return ResponseEntity.ok("Communication sent successfully");
        } catch (Exception e) {
            log.error("Error while sending communication for trigger point: {}, hrId: {}, error: {}", triggerPoint, hrId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error while sending communication");
        }
    }

    private void handleCommunicationDelivery(String templateName, CommType commType, Map<String, String> eventParamsForComms, HrEntity hrEntity, List<EmailAttachmentDto> attachments) {
        try {
            EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
            if (ObjectUtils.isEmpty(hrEntity.getEmployeePersonalEmail())) {
                log.warn("Personal email is not available for hrId: {}. Skipping communication.", hrEntity.getHrId());
                return;
            }
            emailCommunicationDto.setRecipientEmails(List.of(hrEntity.getEmployeePersonalEmail()));
            emailCommunicationDto.setTemplateName(templateName);
            emailCommunicationDto.setTemplateParams(eventParamsForComms);
            emailCommunicationDto.setCommType(commType);
            emailCommunicationDto.setOrgId(hrEntity.getOrg());
            emailCommunicationDto.setSenderEmail("hr@nexus-corp.com");
            emailCommunicationDto.setAttachments(attachments);
            communicationService.sendCommunicationOverKafka(emailCommunicationDto);
        } catch (Exception e) {
            log.error("Error while sending communication for template name: {}, hrId: {}, error: {}", templateName, hrEntity.getHrId(), e.getMessage());
        }
    }

    private Map<String, String> buildEventParamsForComms(String commParams, String templateName, Long hrId, Long payrollId) {
        if (ObjectUtils.isEmpty(commParams)) {
            return Map.of();
        }
        PaymentCompletionHelper.PaymentCompletionData completionData = null;
        if (CommonConstants.CommsTriggerPoint.PAYROLL_MONTHLY.name().equals(templateName)) {
            completionData = paymentCompletionHelper
                    .fetchPaymentCompletionData(payrollId);
        }
        JSONArray jsonArray = new JSONArray(commParams);
        Map<String, String> eventParams = new HashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String key = jsonArray.getJSONObject(i).getString("key");
            String value = jsonArray.getJSONObject(i).getString("value");
            populateEventParams(eventParams, key, value, hrId, completionData);
        }
        return eventParams;
    }

    private void populateEventParams(Map<String, String> eventParams, String key, String value, Long hrId, PaymentCompletionHelper.PaymentCompletionData completionData) {
        HrEntity hrEntity = null;
        if (!ObjectUtils.isEmpty(hrId)) {
            hrEntity = hrEntityRepo.findById(hrId).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));
        }
        switch (key) {
            case "orgName", "organization", "organizationName" -> {
                if (!ObjectUtils.isEmpty(hrEntity)) {
                    eventParams.put(value, hrEntity.getOrgName());
                }
            }
            case "empId", "employeeId" -> {
                if (!ObjectUtils.isEmpty(hrEntity)) {
                    eventParams.put(value, hrEntity.getEmployeeId().toString());
                }
            }
            case "employeeName" -> {
                if (!ObjectUtils.isEmpty(hrEntity)) {
                    eventParams.put(value, hrEntity.getEmployeeName());
                }
            }
            case "department" -> {
                Optional<Position> position = positionRepository.findByHrEntityAndIsActiveTrue(hrEntity);
                position.ifPresent(positionObj -> eventParams.put(value, positionObj.getDepartment()));
            }
            case "position" -> {
                Optional<Position> position = positionRepository.findByHrEntityAndIsActiveTrue(hrEntity);
                position.ifPresent(positionObj -> eventParams.put(value, positionObj.getTitle()));
            }
            case "doj" -> {
                if (!ObjectUtils.isEmpty(hrEntity)) {
                    eventParams.put(value, hrEntity.getDateOfJoining().toString());
                }
            }
            case "contactMail" -> eventParams.put(value, "contact-hr@nexus.com");
            case "basePay" ->
                    eventParams.put(value, "₹" + String.format("%.2f", completionData != null && completionData.basePay() != null ? completionData.basePay() : 0.0));
            case "hra" ->
                    eventParams.put(value, "₹" + String.format("%.2f", completionData != null && completionData.hra() != null ? completionData.hra() : 0.0));
            case "totalBonuses" ->
                    eventParams.put(value, completionData.totalBonuses() != null ? String.valueOf(completionData.totalBonuses()) : "0.0");
            case "totalBonusesFormatted" ->
                    eventParams.put(value, "₹" + String.format("%.2f", completionData != null && completionData.totalBonuses() != null ? completionData.totalBonuses() : 0.0));
            case "totalDeductions" ->
                    eventParams.put(value, completionData.totalDeductions() != null ? String.valueOf(completionData.totalDeductions()) : "0.0");
            case "totalDeductionsFormatted" ->
                    eventParams.put(value, "₹" + String.format("%.2f", completionData != null && completionData.totalDeductions() != null ? completionData.totalDeductions() : 0.0));
            case "grossAmount" ->
                    eventParams.put(value, "₹" + String.format("%.2f", completionData != null && completionData.grossPay() != null ? completionData.grossPay() : 0.0));
            case "netAmount" ->
                    eventParams.put(value, "₹" + String.format("%.2f", completionData != null && completionData.netPay() != null ? completionData.netPay() : 0.0));
            case "paymentReferenceId" ->
                    eventParams.put(value, completionData.paymentReferenceId() != null ? completionData.paymentReferenceId() : "N/A");
            case "paymentDate" ->
                    eventParams.put(value, completionData.paymentDate() != null ? completionData.paymentDate() : "N/A");
            case "bankName" ->
                    eventParams.put(value, completionData.bankName() != null ? completionData.bankName() : "N/A");
            case "accountHolderName" ->
                    eventParams.put(value, completionData.accountHolderName() != null ? completionData.accountHolderName() : "N/A");
            case "maskedAccountNumber" ->
                    eventParams.put(value, completionData.maskedAccountNumber() != null ? completionData.maskedAccountNumber() : "N/A");
            case "ifscCode" ->
                    eventParams.put(value, completionData.ifscCode() != null ? completionData.ifscCode() : "N/A");
            default -> log.warn("Unknown key: {} for event params", key);

        }
    }
}
