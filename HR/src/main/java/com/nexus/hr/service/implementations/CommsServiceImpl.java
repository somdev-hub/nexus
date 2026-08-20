package com.nexus.hr.service.implementations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.hr.exception.ResourceNotFoundException;
import com.nexus.hr.model.entities.ApplicantRecruitmentMapping;
import com.nexus.hr.model.entities.HrCommsConfig;
import com.nexus.hr.model.entities.HrEntity;
import com.nexus.hr.model.entities.Position;
import com.nexus.hr.model.entities.RecruitmentInterview;
import com.nexus.hr.payload.CommsPayload;
import com.nexus.hr.payload.EmailAttachmentDto;
import com.nexus.hr.payload.EmailCommunicationDto;
import com.nexus.hr.repository.ApplicantRecruitmentMappingRepo;
import com.nexus.hr.repository.HrCommsConfigRepo;
import com.nexus.hr.repository.HrEntityRepo;
import com.nexus.hr.repository.PositionRepository;
import com.nexus.hr.repository.RecruitmentInterviewRepo;
import com.nexus.hr.service.interfaces.CommsService;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.utils.CommonConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommsServiceImpl implements CommsService {

	private final HrCommsConfigRepo hrCommsConfigRepo;
	private final HrEntityRepo hrEntityRepo;
	private final PositionRepository positionRepository;
	private final CommunicationService communicationService;
	private final PaymentCompletionHelper paymentCompletionHelper;
	private final ApplicantRecruitmentMappingRepo applicantRecruitmentMappingRepo;
	private final RecruitmentInterviewRepo recruitmentInterviewRepo;

	@Override
	public ResponseEntity<?> sendCommunication(CommonConstants.CommsTriggerPoint triggerPoint,
			List<EmailAttachmentDto> attachments, CommsPayload commsPayload) {
		try {
			if (ObjectUtils.isEmpty(triggerPoint)) {
				return ResponseEntity.badRequest().body("Trigger point is required");
			}

			CommsContext context = buildContext(triggerPoint, attachments, commsPayload);
			handleCommunicationDelivery(context);
			return ResponseEntity.ok("Communication sent successfully");
		} catch (Exception e) {
			log.error("Error while sending communication for trigger point: {}, hrId: {}, error: {}", triggerPoint,
					commsPayload.getHrId(), e.getMessage());
			return ResponseEntity.internalServerError().body("Error while sending communication");
		}
	}

	private CommsContext buildContext(CommonConstants.CommsTriggerPoint triggerPoint,
			List<EmailAttachmentDto> attachments, CommsPayload commsPayload) {
		HrEntity hrEntity = null;
		if (!ObjectUtils.isEmpty(commsPayload.getHrId())) {
			hrEntity = hrEntityRepo.findById(commsPayload.getHrId())
					.orElseThrow(() -> new ResourceNotFoundException("HrEntity", "hrId", commsPayload.getHrId()));
		}
		HrCommsConfig hrCommsConfig = hrCommsConfigRepo.findByCommsTriggerPoint(triggerPoint.name()).orElseThrow(
				() -> new ResourceNotFoundException("CommsTriggerConfig", "triggerPoint", triggerPoint));

		ApplicantRecruitmentMapping applicantRecruitmentMapping = null;
		if (!ObjectUtils.isEmpty(commsPayload.getApplicantRecruitmentMappingId())) {
			applicantRecruitmentMapping = applicantRecruitmentMappingRepo
					.findById(commsPayload.getApplicantRecruitmentMappingId())
					.orElseThrow(() -> new ResourceNotFoundException("ApplicantRecruitmentMapping", "id",
							commsPayload.getApplicantRecruitmentMappingId()));
		}

		RecruitmentInterview recruitmentInterview = null;
		if (!ObjectUtils.isEmpty(commsPayload.getRecruitmentInterviewId())) {
			recruitmentInterview = recruitmentInterviewRepo.findById(commsPayload.getRecruitmentInterviewId())
					.orElseThrow(() -> new ResourceNotFoundException("RecruitmentInterview", "id",
							commsPayload.getRecruitmentInterviewId()));
		}

		PaymentCompletionHelper.PaymentCompletionData completionData = null;
		if (CommonConstants.CommsTriggerPoint.PAYROLL_MONTHLY.name().equals(triggerPoint.name())
				&& !ObjectUtils.isEmpty(commsPayload.getPayrollId())) {
			completionData = paymentCompletionHelper.fetchPaymentCompletionData(commsPayload.getPayrollId());
		}

		Position activePosition = null;
		if (hrEntity != null) {
			activePosition = positionRepository.findByHrEntityAndIsActiveTrue(hrEntity).orElse(null);
		}

		Map<String, String> eventParamsForComms = buildEventParamsForComms(
				hrCommsConfig.getCommParams(),
				triggerPoint.name(),
				new EventParamsContext(hrEntity, applicantRecruitmentMapping, recruitmentInterview, completionData,
						activePosition));

		return new CommsContext(
				triggerPoint,
				attachments,
				commsPayload,
				hrEntity,
				hrCommsConfig,
				eventParamsForComms,
				applicantRecruitmentMapping);
	}

	private void handleCommunicationDelivery(CommsContext context) {
		List<String> recruitmentComms = List
				.of(CommonConstants.CommsTriggerPoint.APPLICANT_INTERVIEW_SCHEDULED.name());
		try {
			EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();

			if (recruitmentComms.contains(context.hrCommsConfig().getCommsTriggerPoint())) {
				if (isApplicantEmailAvailable(context.applicantRecruitmentMapping())) {
					emailCommunicationDto.setRecipientEmails(
							List.of(context.applicantRecruitmentMapping().getApplicant().getApplicantEmail()));
				} else {
					log.warn(
							"Applicant email is not available for applicantRecruitmentMappingId: {}. Skipping communication.",
							context.applicantRecruitmentMapping().getApplicantRecruitmentMappingId());
					return;
				}
			} else {
				if (ObjectUtils.isEmpty(context.hrEntity().getEmployeePersonalEmail())) {
					log.warn("Personal email is not available for hrId: {}. Skipping communication.",
							context.hrEntity().getHrId());
					return;
				}
				emailCommunicationDto.setRecipientEmails(List.of(context.hrEntity().getEmployeePersonalEmail()));
			}

			emailCommunicationDto.setTemplateName(context.hrCommsConfig().getTemplateName());
			emailCommunicationDto.setTemplateParams(context.eventParamsForComms());
			emailCommunicationDto.setCommType(context.hrCommsConfig().getCommType());
			if (recruitmentComms.contains(context.hrCommsConfig.getCommsTriggerPoint())) {
				emailCommunicationDto.setOrgId(context.applicantRecruitmentMapping().getRecruitment().getOrgId());
				// only the first word before space
				emailCommunicationDto.setSenderEmail("hr@"
						+ context.applicantRecruitmentMapping().getRecruitment().getOrgName().trim().split(" ", 2)[0]
						+ ".com");
			} else {
				emailCommunicationDto.setOrgId(context.hrEntity().getOrg());
				emailCommunicationDto
						.setSenderEmail("hr@" + context.hrEntity().getOrgName().trim().split(" ", 2)[0] + ".com");
			}
			emailCommunicationDto.setAttachments(context.attachments());
			communicationService.sendCommunicationOverKafka(emailCommunicationDto);
		} catch (Exception e) {
			log.error("Error while sending communication for template name: {}, hrId: {}, error: {}",
					context.hrCommsConfig().getTemplateName(), context.hrEntity().getHrId(), e.getMessage());
		}
	}

	private boolean isApplicantEmailAvailable(ApplicantRecruitmentMapping applicantRecruitmentMapping) {
		return applicantRecruitmentMapping != null
				&& applicantRecruitmentMapping.getApplicant() != null
				&& !ObjectUtils.isEmpty(applicantRecruitmentMapping.getApplicant().getApplicantEmail());
	}

	private Map<String, String> buildEventParamsForComms(String commParams, String triggerPoint,
			EventParamsContext context) {
		if (ObjectUtils.isEmpty(commParams)) {
			return Map.of();
		}

		JSONArray jsonArray = new JSONArray(commParams);
		Map<String, String> eventParams = new HashMap<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			String key = jsonArray.getJSONObject(i).getString("key");
			String value = jsonArray.getJSONObject(i).getString("value");
			populateEventParams(eventParams, triggerPoint, key, value, context);
		}
		return eventParams;
	}

	private void populateEventParams(Map<String, String> eventParams, String triggerPoint, String key, String value,
			EventParamsContext context) {
		HrEntity hrEntity = context.hrEntity();
		ApplicantRecruitmentMapping applicantRecruitmentMapping = context.applicantRecruitmentMapping();
		RecruitmentInterview recruitmentInterview = context.recruitmentInterview();
		PaymentCompletionHelper.PaymentCompletionData completionData = context.completionData();
		Position activePosition = context.activePosition();

		switch (key) {
			case "orgName", "organization", "organizationName" -> {
				if (CommonConstants.CommsTriggerPoint.APPLICANT_INTERVIEW_SCHEDULED.name().equals(triggerPoint)
						&& applicantRecruitmentMapping != null) {
					if (applicantRecruitmentMapping.getRecruitment() != null
							&& applicantRecruitmentMapping.getRecruitment().getOrgName() != null) {
						eventParams.put(value, applicantRecruitmentMapping.getRecruitment().getOrgName());
					}
				} else if (!ObjectUtils.isEmpty(hrEntity)) {
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
				if (activePosition != null) {
					eventParams.put(value, activePosition.getDepartment());
				}
			}
			case "position" -> {
				if (activePosition != null) {
					eventParams.put(value, activePosition.getTitle());
				}
			}
			case "doj" -> {
				if (!ObjectUtils.isEmpty(hrEntity)) {
					eventParams.put(value, hrEntity.getDateOfJoining().toString());
				}
			}
			case "contactMail" -> eventParams.put(value, "contact-hr@nexus.com");
			case "basePay" ->
				eventParams.put(value, "₹" + String.format("%.2f",
						completionData != null && completionData.basePay() != null ? completionData.basePay() : 0.0));
			case "hra" ->
				eventParams.put(value, "₹" + String.format("%.2f",
						completionData != null && completionData.hra() != null ? completionData.hra() : 0.0));
			case "totalBonuses" ->
				eventParams.put(value,
						completionData.totalBonuses() != null ? String.valueOf(completionData.totalBonuses()) : "0.0");
			case "totalBonusesFormatted" ->
				eventParams.put(value,
						"₹" + String.format("%.2f",
								completionData != null && completionData.totalBonuses() != null
										? completionData.totalBonuses()
										: 0.0));
			case "totalDeductions" ->
				eventParams.put(value,
						completionData.totalDeductions() != null ? String.valueOf(completionData.totalDeductions())
								: "0.0");
			case "totalDeductionsFormatted" ->
				eventParams.put(value,
						"₹" + String.format("%.2f",
								completionData != null && completionData.totalDeductions() != null
										? completionData.totalDeductions()
										: 0.0));
			case "grossAmount" ->
				eventParams.put(value, "₹" + String.format("%.2f",
						completionData != null && completionData.grossPay() != null ? completionData.grossPay() : 0.0));
			case "netAmount" ->
				eventParams.put(value, "₹" + String.format("%.2f",
						completionData != null && completionData.netPay() != null ? completionData.netPay() : 0.0));
			case "paymentReferenceId" ->
				eventParams.put(value,
						completionData.paymentReferenceId() != null ? completionData.paymentReferenceId() : "N/A");
			case "paymentDate" ->
				eventParams.put(value, completionData.paymentDate() != null ? completionData.paymentDate() : "N/A");
			case "bankName" ->
				eventParams.put(value, completionData.bankName() != null ? completionData.bankName() : "N/A");
			case "accountHolderName" ->
				eventParams.put(value,
						completionData.accountHolderName() != null ? completionData.accountHolderName() : "N/A");
			case "maskedAccountNumber" ->
				eventParams.put(value,
						completionData.maskedAccountNumber() != null ? completionData.maskedAccountNumber() : "N/A");
			case "ifscCode" ->
				eventParams.put(value, completionData.ifscCode() != null ? completionData.ifscCode() : "N/A");
			case "roleName" -> {
				eventParams.put(value,
						nullCheckApplicantRecruitmentMappingRecruitment(applicantRecruitmentMapping)
								? applicantRecruitmentMapping.getRecruitment().getRoleName()
								: "N/A");
			}
			case "interviewDate" -> {

				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewDate() != null
								? recruitmentInterview.getInterviewDate().toString()
								: "N/A");
			}
			case "interviewTime" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewTime() != null
								? recruitmentInterview.getInterviewTime().toString()
								: "N/A");
			}
			case "interviewMode" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewMode() != null
								? recruitmentInterview.getInterviewMode().name()
								: "N/A");
			}
			case "interviewerName" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewerName() != null
								? recruitmentInterview.getInterviewerName()
								: "N/A");
			}
			case "interviewType" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewType() != null
								? recruitmentInterview.getInterviewType().name()
								: "N/A");
			}
			case "interviewLocation" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewLocation() != null
								? recruitmentInterview.getInterviewLocation()
								: "N/A");
			}
			case "interviewUrl" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewUrl() != null
								? recruitmentInterview.getInterviewUrl()
								: "N/A");
			}
			case "interviewConfirmationDeadline" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewConfirmationDeadline() != null
								? recruitmentInterview.getInterviewConfirmationDeadline().toString()
								: "N/A");
			}
			case "interviewConfirmationLink" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewConfirmationLink() != null
								? recruitmentInterview.getInterviewConfirmationLink()
								: "N/A");
			}
			case "applicantFirstName" -> {
				eventParams.put(value,
						nullCheckApplicantRecruitmentMapping(applicantRecruitmentMapping)
								&& applicantRecruitmentMapping.getApplicant() != null
										? applicantRecruitmentMapping.getApplicant().getApplicantFirstName()
										: "N/A");
			}
			case "applicantLastName" -> {
				eventParams.put(value,
						nullCheckApplicantRecruitmentMapping(applicantRecruitmentMapping)
								&& applicantRecruitmentMapping.getApplicant() != null
										? applicantRecruitmentMapping.getApplicant().getApplicantLastName()
										: "N/A");
			}
			case "interviewDuration" -> {
				eventParams.put(value,
						recruitmentInterview != null && recruitmentInterview.getInterviewDuration() != null
								? recruitmentInterview.getInterviewDuration().toString()
								: "N/A");
			}
			case "hiringType" -> {
				eventParams.put(value,
						nullCheckApplicantRecruitmentMappingRecruitment(applicantRecruitmentMapping)
								? applicantRecruitmentMapping.getRecruitment().getHiringType().name()
								: "N/A");
			}
			case "location" -> {
				eventParams.put(value,
						nullCheckApplicantRecruitmentMappingRecruitment(applicantRecruitmentMapping)
								? applicantRecruitmentMapping.getRecruitment().getLocation()
								: "N/A");
			}
			case "departmentName" -> {
				eventParams.put(value,
						nullCheckApplicantRecruitmentMappingRecruitment(applicantRecruitmentMapping)
								? applicantRecruitmentMapping.getRecruitment().getDepartmentName()
								: "N/A");
			}
			default -> log.warn("Unknown key: {} for event params", key);

		}
	}

	private boolean nullCheckApplicantRecruitmentMapping(ApplicantRecruitmentMapping applicantRecruitmentMapping) {
		return applicantRecruitmentMapping != null;
	}

	private boolean nullCheckApplicantRecruitmentMappingRecruitment(
			ApplicantRecruitmentMapping applicantRecruitmentMapping) {
		return nullCheckApplicantRecruitmentMapping(applicantRecruitmentMapping)
				&& applicantRecruitmentMapping.getRecruitment() != null;
	}

	private record CommsContext(
			CommonConstants.CommsTriggerPoint triggerPoint,
			List<EmailAttachmentDto> attachments,
			CommsPayload commsPayload,
			HrEntity hrEntity,
			HrCommsConfig hrCommsConfig,
			Map<String, String> eventParamsForComms,
			ApplicantRecruitmentMapping applicantRecruitmentMapping) {
	}

	private record EventParamsContext(
			HrEntity hrEntity,
			ApplicantRecruitmentMapping applicantRecruitmentMapping,
			RecruitmentInterview recruitmentInterview,
			PaymentCompletionHelper.PaymentCompletionData completionData,
			Position activePosition) {
	}
}
/**
 * [{"key": "applicantFirstName", "value": "applicantFirstName"}, {"key":
 * "applicantLastName", "value": "applicantLastName"}, {"key": "orgName",
 * "value": "orgName"}, {"key": "roleName", "value": "roleName"}, {"key":
 * "interviewType", "value": "interviewType"}, {"key": "interviewDate", "value":
 * "interviewDate"}, {"key": "interviewTime", "value": "interviewTime"}, {"key":
 * "interviewDuration", "value": "interviewDuration"}, {"key": "interviewMode",
 * "value": "interviewMode"}, {"key": "interviewLocation", "value":
 * "interviewLocation"}, {"key": "interviewUrl", "value": "interviewUrl"},
 * {"key": "interviewerName", "value": "interviewerName"}, {"key":
 * "interviewConfirmationDeadline", "value": "interviewConfirmationDeadline"},
 * {"key": "interviewConfirmationLink", "value": "interviewConfirmationLink"}]
 */
