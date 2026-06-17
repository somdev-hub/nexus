package com.nexus.hr.service.implementations;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.model.entities.HrCommsConfig;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.entities.Position;
import com.nexus.hr.model.enums.CommType;
import com.nexus.hr.payload.EmailAttachmentDto;
import com.nexus.hr.payload.EmailCommunicationDto;
import com.nexus.hr.payload.RestPayload;
import com.nexus.hr.repository.HrCommsConfigRepo;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.PositionRepository;
import com.nexus.hr.service.interfaces.CommsService;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.utils.CommonConstants;
import com.nexus.hr.utils.CommonUtils;
import com.nexus.hr.utils.RestServices;
import com.nexus.hr.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.http.HttpMethod;
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
    private final WebConstants webConstants;
    private final RestServices restServices;
    private final CommonUtils commonUtils;
    private final CommunicationService communicationService;

    @Override
    public ResponseEntity<?> sendCommunication(CommonConstants.CommsTriggerPoint triggerPoint, Long hrId, List<EmailAttachmentDto> attachments) {
        try {
            if (ObjectUtils.isEmpty(triggerPoint)) {
                return ResponseEntity.badRequest().body("Trigger point is required");
            }
            HrEntity hrEntity = hrEntityRepo.findById(hrId).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));
            RestPayload restPayload = commonUtils.buildRestPayload(webConstants.getUserDetailsUrl(),
                    Map.of("userId", hrEntity.getEmployeeId().toString()), null, CommonConstants.APPLICATION_JSON);
            ResponseEntity<?> userResponse = restServices.hrRestCall(restPayload.getBuilder().toUriString(), null,
                    restPayload.getHeaders(), HttpMethod.GET, hrEntity.getHrId());
            if (!userResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to fetch user details for hrId: {}. Status code: {}, Response body: {}", hrId, userResponse.getStatusCode(), userResponse.getBody());
                throw new ResourceNotFoundException("UserDetails", "hrId", "Failed to fetch user details for hrId: " + hrId);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> userDetails = (Map<String, Object>) userResponse.getBody();
            HrCommsConfig hrCommsConfig = hrCommsConfigRepo.findByCommsTriggerPoint(triggerPoint.name()).orElseThrow(() -> new ResourceNotFoundException("CommsTriggerConfig", "triggerPoint", triggerPoint));
            Map<String, String> eventParamsForComms = buildEventParamsForComms(hrCommsConfig.getCommParams(), hrId, userDetails);
            handleCommunicationDelivery(hrCommsConfig.getTemplateName(), hrCommsConfig.getCommType(), eventParamsForComms, hrId, attachments, userDetails);
            return ResponseEntity.ok("Communication sent successfully");
        } catch (Exception e) {
            log.error("Error while sending communication for trigger point: {}, hrId: {}, error: {}", triggerPoint, hrId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error while sending communication");
        }
    }

    private void handleCommunicationDelivery(String templateName, CommType commType, Map<String, String> eventParamsForComms, Long hrId, List<EmailAttachmentDto> attachments, Map<String, Object> userDetails) {
        try {
            EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();
            if (!userDetails.containsKey("personalEmail")) {
                throw new ResourceNotFoundException("UserDetails", "personalEmail", "Personal email not found for user with hrId: " + hrId);
            }
            String email = (String) userDetails.get("personalEmail");
            emailCommunicationDto.setRecipientEmails(List.of(email));
            emailCommunicationDto.setTemplateName(templateName);
            emailCommunicationDto.setTemplateParams(eventParamsForComms);
            emailCommunicationDto.setCommType(commType);
            emailCommunicationDto.setSenderEmail("hr@nexus-corp.com");
            emailCommunicationDto.setAttachments(attachments);
            communicationService.sendCommunicationOverKafka(emailCommunicationDto);
        } catch (Exception e) {
            log.error("Error while sending communication for template name: {}, hrId: {}, error: {}", templateName, hrId, e.getMessage());
        }
    }

    private Map<String, String> buildEventParamsForComms(String commParams, Long hrId, Map<String, Object> userDetails) {
        if (ObjectUtils.isEmpty(commParams)) {
            return Map.of();
        }
        JSONArray jsonArray = new JSONArray(commParams);
        Map<String, String> eventParams = new HashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String key = jsonArray.getJSONObject(i).getString("key");
            String value = jsonArray.getJSONObject(i).getString("value");
            populateEventParams(eventParams, key, value, hrId, userDetails);
        }
        return eventParams;
    }

    private void populateEventParams(Map<String, String> eventParams, String key, String value, Long hrId, Map<String, Object> userDetails) {
        HrEntity hrEntity = null;
        if (!ObjectUtils.isEmpty(hrId)) {
            hrEntity = hrEntityRepo.findById(hrId).orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", hrId));
        }
        switch (key) {
            case "orgName", "organization" -> {
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
                if (!ObjectUtils.isEmpty(userDetails) && userDetails.containsKey("name")) {
                    eventParams.put(value, (String) userDetails.get("name"));
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
        }
    }
}
