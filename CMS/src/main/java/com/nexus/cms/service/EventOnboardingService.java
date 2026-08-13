package com.nexus.cms.service;

import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.model.entities.EventTemplate;
import com.nexus.cms.model.entities.TemplateParam;
import com.nexus.cms.model.enums.KafkaStatus;
import com.nexus.cms.payload.MailTriggerDto;
import com.nexus.cms.payload.ShortEventTemplatePayload;
import com.nexus.cms.repository.EventTemplateRepo;
import com.nexus.cms.repository.KafkaBacklogsRepo;
import com.nexus.cms.util.CommonConstants;
import com.nexus.cms.util.CommonUtils;
import com.nexus.cms.util.RestService;
import com.nexus.cms.util.WebConstants;
import com.nexus.nexusencryption.NexusEncryption;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventOnboardingService {

	private final EventTemplateRepo eventTemplateRepo;
	private final WebConstants webConstants;
	private final CommonUtils commonUtils;
	private final RestService restService;
	private final ModelMapper modelMapper;
	private final RestTemplate restTemplate;
	private final JavaMailSender javaMailSender;
	private final KafkaBacklogService kafkaBacklogService;
	private final KafkaBacklogsRepo kafkaBacklogsRepo;

	private static @NonNull ByteArrayResource getByteArrayResource(String templateHtml, Long orgId, String templateName)
			throws IOException {
		String encryptedFile = NexusEncryption.encrypt(templateHtml);
		byte[] htmlBytes = encryptedFile.getBytes(StandardCharsets.UTF_8);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(htmlBytes);
		// transfer to ByteArrayOutputStream
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayInputStream.transferTo(byteArrayOutputStream);
		byte[] fileBytes = byteArrayOutputStream.toByteArray();
		return new ByteArrayResource(fileBytes) {
			@Override
			public String getFilename() {
				return templateName + "_" + orgId + "_" + LocalDateTime.now() + ".txt"; // filename MUST be present
			}
		};
	}

	@Transactional
	public ResponseEntity<?> addEventTemplate(EventTemplate eventTemplate) {
		if (ObjectUtils.isEmpty(eventTemplate)) {
			throw new IllegalArgumentException("Event Template cannot be null or empty");
		}
		try {
			if (!ObjectUtils.isEmpty(eventTemplate.getTemplateHtml())) {
				// Logic to save the HTML content to a file and set the URL
				String htmlUrl = saveHtmlToFile(eventTemplate.getTemplateHtml(), eventTemplate.getOrgId(),
						eventTemplate.getTemplateName());
				eventTemplate.setTemplateHtmlUrl(htmlUrl);
			}
			EventTemplate savedTemplate = eventTemplateRepo.save(eventTemplate);
			return ResponseEntity.ok(savedTemplate);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "addEventTemplate",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	private String saveHtmlToFile(String templateHtml, Long orgId, String templateName) {
		try {
			// STEP 1 create a temp file and write templateHtml to that file
			ByteArrayResource fileResource = getByteArrayResource(templateHtml, orgId, templateName);
			// STEP 2 save the file to DMS
			Map<String, String> headers = new ConcurrentHashMap<>();
			headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
			headers.put(CommonConstants.AUTHORIZATION, commonUtils.getToken());
			UriComponentsBuilder url = UriComponentsBuilder.fromUriString(
					webConstants.getDmsOrgDocumentUploadUrl());

			Map<String, Object> payload = new ConcurrentHashMap<>();
			Map<String, Object> dto = new HashMap<>();
			dto.put("orgId", orgId);
			dto.put("fileName", templateName + "_" + orgId + "_" + LocalDateTime.now() + ".txt");
			dto.put("remarks", "Event Template HTML file upload for template: " + templateName);
			dto.put("documentType", "EVENT_TEMPLATE");

			payload.put("file", fileResource);
			payload.put("dto", dto);
			ResponseEntity<?> dmsResponse = restService.cmsRestCall(url.toUriString(), payload, headers,
					HttpMethod.POST, orgId);
			// STEP 3 return the url
			if (dmsResponse.getStatusCode().is2xxSuccessful() && !ObjectUtils.isEmpty(dmsResponse.getBody())) {
				return commonUtils.parseAndGetDocumentUrl(dmsResponse);
			} else {
				throw new ServiceLevelException("EventOnboardingService",
						"Failed to upload HTML to DMS. Response: " + dmsResponse, "saveHtmlToFile",
						"DMSUploadException", "DMS did not return a successful response");
			}
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "saveHtmlToFile",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	@Transactional
	public ResponseEntity<?> updateEventTemplate(EventTemplate eventTemplate, Boolean templateUpdate) {
		if (ObjectUtils.isEmpty(eventTemplate) || ObjectUtils.isEmpty(eventTemplate.getEventTemplateId())) {
			throw new IllegalArgumentException("Event Template and its ID cannot be null or empty");
		}
		try {
			EventTemplate savedEventTemplate = eventTemplateRepo.findById(eventTemplate.getEventTemplateId())
					.orElseThrow(() -> new ResourceNotFoundException("EventTemplate", "eventTemplateId",
							eventTemplate.getEventTemplateId()));
			if (Boolean.TRUE.equals(templateUpdate) && !ObjectUtils.isEmpty(eventTemplate.getTemplateHtml())) {
				// Logic to save the HTML content to a file and set the URL
				String htmlUrl = saveHtmlToFile(eventTemplate.getTemplateHtml(), eventTemplate.getOrgId(),
						eventTemplate.getTemplateName());
				savedEventTemplate.setTemplateHtmlUrl(htmlUrl);
			}
			setIfExist(eventTemplate.getEventTemplateType(), savedEventTemplate::setEventTemplateType);
			setIfExist(eventTemplate.getTemplateName(), savedEventTemplate::setTemplateName);
			setIfExist(eventTemplate.getIsActive(), savedEventTemplate::setIsActive);
			setIfExist(eventTemplate.getEventSubject(), savedEventTemplate::setEventSubject);
			savedEventTemplate.getTemplateParams().clear(); // Clear existing params to avoid duplication
			savedEventTemplate.getTemplateParams().addAll(eventTemplate.getTemplateParams()); // Add new params
			EventTemplate updatedTemplate = eventTemplateRepo.save(savedEventTemplate);
			return ResponseEntity.ok(updatedTemplate);
		} catch (ServiceLevelException e) {
			throw e; // Rethrow custom exceptions
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "updateEventTemplate",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	private <T> void setIfExist(T object, Consumer<T> consumer) {
		if (!ObjectUtils.isEmpty(object)) {
			consumer.accept(object);
		}
	}

	@Transactional
	public ResponseEntity<?> addTemplateParams(List<TemplateParam> templateParams, Long eventTemplateId) {
		if (ObjectUtils.isEmpty(templateParams) || ObjectUtils.isEmpty(eventTemplateId)) {
			throw new IllegalArgumentException("Template Param and Event Template ID cannot be null or empty");
		}
		try {
			EventTemplate eventTemplate = eventTemplateRepo.findById(eventTemplateId)
					.orElseThrow(
							() -> new ResourceNotFoundException("EventTemplate", "eventTemplateId", eventTemplateId));

			List<TemplateParam> existingParams = eventTemplate.getTemplateParams();
			existingParams.addAll(templateParams);
			eventTemplate.setTemplateParams(existingParams);

			EventTemplate updatedTemplate = eventTemplateRepo.save(eventTemplate);
			return ResponseEntity.ok(updatedTemplate);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "addTemplateParam",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	@Transactional
	public ResponseEntity<?> updateTemplateParams(List<TemplateParam> templateParams, Long eventTemplateId) {
		if (ObjectUtils.isEmpty(templateParams) || ObjectUtils.isEmpty(eventTemplateId)) {
			throw new IllegalArgumentException("Template Params and Event Template ID cannot be null or empty");
		}
		try {
			EventTemplate eventTemplate = eventTemplateRepo.findById(eventTemplateId)
					.orElseThrow(
							() -> new ResourceNotFoundException("EventTemplate", "eventTemplateId", eventTemplateId));

			eventTemplate.setTemplateParams(templateParams);
			EventTemplate updatedTemplate = eventTemplateRepo.save(eventTemplate);
			return ResponseEntity.ok(updatedTemplate);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "updateTemplateParams",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	public ResponseEntity<?> getEventTemplates(Long orgId) {
		if (orgId == null) {
			throw new IllegalArgumentException("Organization ID cannot be null");
		}
		try {
			List<EventTemplate> eventTemplates = eventTemplateRepo.findByOrgIdAndIsActiveTrue(orgId);
			List<ShortEventTemplatePayload> shortEventTemplatePayloads = eventTemplates.stream().map(template -> {
				ShortEventTemplatePayload map = modelMapper.map(template, ShortEventTemplatePayload.class);
				map.setNumberOfParams(
						ObjectUtils.isEmpty(template.getTemplateParams()) ? 0 : template.getTemplateParams().size());
				return map;
			}).toList();
			return ResponseEntity.ok(shortEventTemplatePayloads);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "getEventTemplates",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	public ResponseEntity<?> getEventTemplateById(Long eventTemplateId) {
		if (eventTemplateId == null) {
			throw new IllegalArgumentException("Event Template ID cannot be null");
		}
		try {
			EventTemplate eventTemplate = eventTemplateRepo.findById(eventTemplateId)
					.orElseThrow(
							() -> new ResourceNotFoundException("EventTemplate", "eventTemplateId", eventTemplateId));
			return ResponseEntity.ok(eventTemplate);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "getEventTemplateById",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	public ResponseEntity<?> getEventTemplateByName(String templateName, Long orgId) {
		if (ObjectUtils.isEmpty(templateName) || orgId == null) {
			throw new IllegalArgumentException("Template Name and Organization ID cannot be null or empty");
		}
		try {
			EventTemplate eventTemplate = eventTemplateRepo
					.findByTemplateNameAndOrgIdAndIsActiveTrue(templateName, orgId)
					.orElseThrow(() -> new ResourceNotFoundException("EventTemplate", "templateName and orgId",
							templateName + " and " + orgId));
			ShortEventTemplatePayload map = modelMapper.map(eventTemplate, ShortEventTemplatePayload.class);
			map.setNumberOfParams(ObjectUtils.isEmpty(eventTemplate.getTemplateParams()) ? 0
					: eventTemplate.getTemplateParams().size());
			return ResponseEntity.ok(map);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "getEventTemplateByName",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	public ResponseEntity<?> triggerMail(MailTriggerDto mailTriggerDto) {
		if (ObjectUtils.isEmpty(mailTriggerDto) || ObjectUtils.isEmpty(mailTriggerDto.getTemplateName())
				|| ObjectUtils.isEmpty(mailTriggerDto.getOrgId())
				|| ObjectUtils.isEmpty(mailTriggerDto.getRecipientEmails())) {
			throw new IllegalArgumentException(
					"Mail Trigger DTO, Template Name, Organization ID and Recipient Emails cannot be null or empty");
		}
		UUID uuid = UUID.randomUUID();
		try {
			EventTemplate eventTemplate = eventTemplateRepo
					.findByTemplateNameAndOrgIdAndIsActiveTrue(mailTriggerDto.getTemplateName(),
							mailTriggerDto.getOrgId())
					.orElseThrow(() -> new ResourceNotFoundException("EventTemplate", "templateName and orgId",
							mailTriggerDto.getTemplateName() + " and " + mailTriggerDto.getOrgId()));
			kafkaBacklogService.logReceived(null, uuid.toString(), mailTriggerDto.getOrgId(),
					mailTriggerDto.getTemplateName());
			Map<String, String> paramMap = new HashMap<>();
			if (!ObjectUtils.isEmpty(mailTriggerDto.getTemplateParams())) {
				mailTriggerDto.getTemplateParams().forEach(param -> paramMap.put(param.getKey(), param.getValue()));
			}
			String templateHtmlUrl = eventTemplate.getTemplateHtmlUrl();
			// fetch the html content
			String templateHtml = restTemplate.getForObject(templateHtmlUrl, String.class);
			if (ObjectUtils.isEmpty(templateHtml)) {
				throw new ServiceLevelException("EventOnboardingService",
						"Failed to fetch template HTML content from URL: " + templateHtmlUrl, "triggerMail",
						"TemplateFetchException", "DMS did not return template content");
			}
			String subject = eventTemplate.getEventSubject();
			// placeholders are present in this fashion: ${key}
			for (Map.Entry<String, String> entry : paramMap.entrySet()) {
				String placeholder = "${" + entry.getKey() + "}";
				templateHtml = templateHtml.replace(placeholder, entry.getValue());
				subject = subject.replace(placeholder, entry.getValue());
			}
			// check from the applicable params if any param is left out then set the
			// default value of that param
			if (!ObjectUtils.isEmpty(eventTemplate.getTemplateParams())) {
				for (TemplateParam templateParam : eventTemplate.getTemplateParams()) {
					String placeholder = "${" + templateParam.getParamName() + "}";
					if (templateHtml.contains(placeholder) && !paramMap.containsKey(templateParam.getParamName())
							&& !ObjectUtils.isEmpty(templateParam.getParamDefaultValue())) {
						templateHtml = templateHtml.replace(placeholder, templateParam.getParamDefaultValue());
						subject = subject.replace(placeholder, templateParam.getParamDefaultValue());
					}
				}
			}
			// trigger the mail with the final HTML content
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
			helper.setText(templateHtml, true); // set to true for HTML content
			helper.setTo(mailTriggerDto.getRecipientEmails().toArray(new String[0]));
			helper.setSubject(subject); // You can customize the subject as needed
			javaMailSender.send(mimeMessage);
			kafkaBacklogService.logProcessed(null, uuid.toString());
			return ResponseEntity.ok("Mail triggered successfully");

		} catch (Exception e) {
			kafkaBacklogService.logFailed(null, uuid.toString());
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "triggerMail",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}

	public ResponseEntity<?> getEventHits(String templateParam, Long orgId) {
		if (!eventTemplateRepo.existsByTemplateNameAndOrgIdAndIsActiveTrue(templateParam, orgId)) {
			throw new ResourceNotFoundException("EventTemplate", "templateParam and orgId",
					templateParam + " and " + orgId);
		}
		List<Object[]> monthlyCountByTemplateParam = kafkaBacklogsRepo.findMonthlyCountByTemplateParam(templateParam);
		Map<String, Long> monthlyCount = new HashMap<>();
		for (Object[] row : monthlyCountByTemplateParam) {
			String month = (String) row[0];
			Long count = ((Number) row[2]).longValue();
			monthlyCount.put(month, count);
		}

		return ResponseEntity.ok(monthlyCount);
	}

	public ResponseEntity<?> getEventStatusBreakdown(String templateName, Long orgId) {
		try {
			if (!eventTemplateRepo.existsByTemplateNameAndOrgIdAndIsActiveTrue(templateName, orgId)) {
				throw new ResourceNotFoundException("EventTemplate", "templateName and orgId",
						templateName + " and " + orgId);
			}

			Map<String, Long> statusMap = Arrays.stream(KafkaStatus.values())
					.collect(Collectors.toMap(Enum::name, s -> 0L)); // seed all 4 with 0

			kafkaBacklogsRepo.findStatusCountByTemplateParamAndOrgId(templateName, orgId)
					.forEach(row -> statusMap.put((String) row[0], ((Number) row[1]).longValue()));

			return ResponseEntity.ok(statusMap);
		} catch (Exception e) {
			throw new ServiceLevelException("EventOnboardingService", e.getMessage(), "getEventStatusBreakdown",
					e.getClass().getName(), e.getLocalizedMessage());
		}
	}
}
