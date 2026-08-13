package com.nexus.cms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.cms.exception.ResourceNotFoundException;
import com.nexus.cms.exception.ServiceLevelException;
import com.nexus.cms.model.entities.EventTemplate;
import com.nexus.cms.model.entities.TemplateParam;
import com.nexus.cms.model.enums.CommsStatus;
import com.nexus.cms.model.enums.CommsType;
import com.nexus.cms.payload.EmailAttachmentDto;
import com.nexus.cms.payload.EmailCommunicationDto;
import com.nexus.cms.repository.EventTemplateRepo;
import com.nexus.cms.util.WebConstants;
import com.nexus.nexusencryption.NexusEncryption;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class EmailCommunicationService {

    private final ObjectMapper objectMapper;
    private final JavaMailSender javaMailSender;
    private final ITemplateEngine templateEngine;
    private final WebConstants webConstants;
    private final LoggerService loggerService;
    private final KafkaBacklogService kafkaBacklogService;
    private final EventTemplateRepo eventTemplateRepo;
    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public void handleEmailCommunication(String message) {
        JSONObject kafkaContent = null;
        EmailCommunicationDto emailCommunicationDto = null;
        String uuid;
        try {
            kafkaContent = new JSONObject(message);
            if (kafkaContent.has("message")) {
                // The message field contains a JSON string, so we need to parse it with
                // readValue
                String emailDtoJson = kafkaContent.get("message").toString();
                emailCommunicationDto = objectMapper.readValue(emailDtoJson, EmailCommunicationDto.class);
            }
            if (!kafkaContent.has("uuid")) {
                throw new IllegalArgumentException("UUID is missing in the Kafka message");
            }
            uuid = kafkaContent.get("uuid").toString();

            long startTime = System.currentTimeMillis();

            try {
                validateEmailCommunication(emailCommunicationDto);

                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                String fromEmail = ObjectUtils.isEmpty(emailCommunicationDto.getSenderEmail())
                        ? webConstants.getDefaultFromEmail()
                        : emailCommunicationDto.getSenderEmail();

                helper.setFrom(fromEmail);
                helper.setTo(emailCommunicationDto.getRecipientEmails().toArray(new String[0]));
//                helper.setSubject(emailCommunicationDto.getSubject());

//                String emailFilename = null;
//                if (kafkaContent.containsKey("topic")
//                        && kafkaContent.get("topic").equals(CommonConstants.CANDIDATE_SELECTION_MAIL_TOPIC)) {
//                    emailFilename = CommonConstants.HR_INIT_EMAIL_FILE_NAME;
//                } else if (kafkaContent.containsKey("topic")
//                        && kafkaContent.get("topic").equals(CommonConstants.SALARY_PAYMENT_MAIL_TOPIC)) {
//                    emailFilename = CommonConstants.SALARY_PAYMENT_EMAIL_FILE_NAME;
//                }
//                String processedBody = renderThymeleafTemplate(emailFilename, emailCommunicationDto.getPlaceholders());
//
                // new methods
                if (ObjectUtils.isEmpty(emailCommunicationDto.getTemplateName()) || ObjectUtils.isEmpty(emailCommunicationDto.getOrgId())) {
                    throw new IllegalArgumentException("Template name and organization ID are required for email communication");
                }
//                EmailCommunicationDto finalEmailCommunicationDto = emailCommunicationDto;
                String templateName = emailCommunicationDto.getTemplateName();
                EventTemplate eventTemplate = eventTemplateRepo.findByTemplateNameAndOrgIdAndIsActiveTrue(templateName, emailCommunicationDto.getOrgId()).orElseThrow(() -> new ResourceNotFoundException("EventTemplate", "templateName", templateName));
                kafkaBacklogService.logReceived(null, uuid, emailCommunicationDto.getOrgId(), templateName);
                String templateHtmlUrl = eventTemplate.getTemplateHtmlUrl();
                // fetch the html content
                String templateHtmlEncrypted = restTemplate.getForObject(templateHtmlUrl, String.class);
                if (ObjectUtils.isEmpty(templateHtmlEncrypted)) {
                    throw new ServiceLevelException("EventOnboardingService", "Failed to fetch template HTML content from URL: " + templateHtmlUrl, "triggerMail", "TemplateFetchException", "DMS did not return template content");
                }
                String templateHtml;
                try {
                    if (templateHtmlEncrypted.startsWith("<!DOCTYPE html>")) {
                        templateHtml = templateHtmlEncrypted;
                    } else {
                        templateHtml = NexusEncryption.decrypt(templateHtmlEncrypted);
                    }
                } catch (Exception e) {
                    throw new ServiceLevelException("EventOnboardingService", "Failed to decrypt template HTML content from URL: " + templateHtmlUrl, "triggerMail", "TemplateDecryptionException", e.getMessage());
                }
                String subject = eventTemplate.getEventSubject();
                // placeholders are present in this fashion: ${key}
                for (Map.Entry<String, String> entry : emailCommunicationDto.getTemplateParams().entrySet()) {
                    String placeholder = "${" + entry.getKey() + "}";
                    templateHtml = templateHtml.replace(placeholder, entry.getValue());
                    subject = subject.replace(placeholder, entry.getValue());
                }
                // check from the applicable params if any param is left out then set the default value of that param
                if (!ObjectUtils.isEmpty(eventTemplate.getTemplateParams())) {
                    for (TemplateParam templateParam : eventTemplate.getTemplateParams()) {
                        String placeholder = "${" + templateParam.getParamName() + "}";
                        if (templateHtml.contains(placeholder) && !emailCommunicationDto.getTemplateParams().containsKey(templateParam.getParamName()) && !ObjectUtils.isEmpty(templateParam.getParamDefaultValue())) {
                            templateHtml = templateHtml.replace(placeholder, templateParam.getParamDefaultValue());
                            subject = subject.replace(placeholder, templateParam.getParamDefaultValue());
                        }
                    }
                }

                helper.setSubject(subject);
                helper.setText(templateHtml, true);
                if (!CollectionUtils.isEmpty(emailCommunicationDto.getCcEmails())) {
                    helper.setCc(emailCommunicationDto.getCcEmails().toArray(new String[0]));
                }

                // Add BCC emails if present
                if (!CollectionUtils.isEmpty(emailCommunicationDto.getBccEmails())) {
                    helper.setBcc(emailCommunicationDto.getBccEmails().toArray(new String[0]));
                }

                // Attach files if present
                if (!CollectionUtils.isEmpty(emailCommunicationDto.getAttachments())) {
                    attachFilesToEmail(helper, emailCommunicationDto.getAttachments());
                }

                // Send email
                javaMailSender.send(mimeMessage);

                loggerService.log("HR", emailCommunicationDto.getRecipientEmails(), emailCommunicationDto.getCcEmails(),
                        emailCommunicationDto.getBccEmails(), null, CommsType.EMAIL,
                        objectMapper.writeValueAsString(emailCommunicationDto.getTemplateParams()),
                        kafkaContent.get("uuid").toString(), CommsStatus.SENT);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Email sent successfully to {} recipients in {}ms",
                        emailCommunicationDto.getRecipientEmails().size(), duration);
            } catch (IllegalArgumentException e) {
                log.error("Validation error while processing email communication: {}", e.getMessage());
            } catch (MessagingException e) {
                assert emailCommunicationDto != null;
                loggerService.log("HR", emailCommunicationDto.getRecipientEmails(), emailCommunicationDto.getCcEmails(),
                        emailCommunicationDto.getBccEmails(), null, CommsType.EMAIL,
                        objectMapper.writeValueAsString(emailCommunicationDto.getTemplateParams()),
                        kafkaContent.get("uuid").toString(), CommsStatus.FAILED);
                log.error("Error sending email communication: {}", e.getMessage(), e);
            } catch (Exception e) {
                assert emailCommunicationDto != null;
                loggerService.log("HR", emailCommunicationDto.getRecipientEmails(), emailCommunicationDto.getCcEmails(),
                        emailCommunicationDto.getBccEmails(), null, CommsType.EMAIL,
                        objectMapper.writeValueAsString(emailCommunicationDto.getTemplateParams()),
                        kafkaContent.get("uuid").toString(), CommsStatus.FAILED);
                log.error("Unexpected error while processing email communication: {}", e.getMessage(), e);
            }
            kafkaBacklogService.logProcessed(kafkaContent.get("topic").toString(), kafkaContent.get("uuid").toString());
        } catch (Exception e) {
            log.error("Error processing email communication message: {}", e.getMessage(), e);
            assert kafkaContent != null;
            kafkaBacklogService.logFailed(kafkaContent.get("topic").toString(), kafkaContent.get("uuid").toString());
        }
    }

    /**
     * Validates email communication data
     */
    private void validateEmailCommunication(EmailCommunicationDto dto) {
        if (ObjectUtils.isEmpty(dto)) {
            throw new IllegalArgumentException("Email communication data cannot be null");
        }

        if (CollectionUtils.isEmpty(dto.getRecipientEmails())) {
            throw new IllegalArgumentException("At least one recipient email is required");
        }

        if (dto.getRecipientEmails().size() > webConstants.getMaxRecipients()) {
            throw new IllegalArgumentException(
                    String.format("Number of recipients exceeds maximum limit of %d", webConstants.getMaxRecipients()));
        }

        // Validate email formats
        validateEmailFormats(dto.getRecipientEmails(), "recipient");
        if (!CollectionUtils.isEmpty(dto.getCcEmails())) {
            validateEmailFormats(dto.getCcEmails(), "CC");
        }
        if (!CollectionUtils.isEmpty(dto.getBccEmails())) {
            validateEmailFormats(dto.getBccEmails(), "BCC");
        }
    }

    /**
     * Validates email address format using regex
     */
    private void validateEmailFormats(List<String> emails, String type) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        List<String> invalidEmails = emails.stream().filter(email -> !email.matches(emailRegex)).toList();

        if (!invalidEmails.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s email format: %s", type, String.join(", ", invalidEmails)));
        }
    }

    /**
     * Attaches files to email from provided URLs
     */
    private void attachFilesToEmail(MimeMessageHelper helper, List<EmailAttachmentDto> attachments)
            throws MessagingException {
        for (EmailAttachmentDto attachment : attachments) {
            try {
                if (ObjectUtils.isEmpty(attachment.getFileUrl()) || ObjectUtils.isEmpty(attachment.getFileName())) {
                    log.warn("Skipping attachment with missing file URL or name");
                    continue;
                }

                // Validate URL format before attempting download
                String fileUrl = attachment.getFileUrl().trim();
                if (!isValidUrl(fileUrl)) {
                    log.warn("Skipping attachment with invalid URL: {}", fileUrl);
                    continue;
                }

                // Download file from URL and attach
                byte[] fileData = downloadFile(fileUrl);

                // Normalize content type to proper MIME format
                String contentType = normalizeContentType(attachment.getContentType(), attachment.getFileName());

                helper.addAttachment(attachment.getFileName(), () -> new java.io.ByteArrayInputStream(fileData),
                        contentType);

                log.debug("Attached file: {} with content type: {}", attachment.getFileName(), contentType);
            } catch (Exception e) {
                log.warn("Failed to attach file: {}", attachment.getFileName(), e);
                // Continue with other attachments even if one fails
            }
        }
    }

    /**
     * Validates URL format to prevent invalid IPFS paths
     */
    private boolean isValidUrl(String url) {
        if (ObjectUtils.isEmpty(url)) {
            return false;
        }
        // Check for null/invalid patterns
        if (url.contains("/null") || url.endsWith("null") || url.equalsIgnoreCase("null")) {
            return false;
        }
        // Must start with http:// or https:// or file://
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://");
    }

    /**
     * Normalizes content type to proper MIME format
     * Handles cases where content type is just "PDF" instead of "application/pdf"
     */
    private String normalizeContentType(String contentType, String fileName) {
        // If content type is null or empty, try to infer from filename
        if (ObjectUtils.isEmpty(contentType)) {
            return inferContentTypeFromFilename(fileName);
        }

        // If content type doesn't contain '/', it's not a valid MIME type
        if (!contentType.contains("/")) {
            String normalizedType = contentType.trim().toUpperCase();

            // Map common short names to proper MIME types
            return switch (normalizedType) {
                case "PDF" -> "application/pdf";
                case "DOC", "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                case "XLS", "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                case "PPT", "PPTX" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "TXT", "TEXT" -> "text/plain";
                case "HTML", "HTM" -> "text/html";
                case "XML" -> "application/xml";
                case "JSON" -> "application/json";
                case "ZIP" -> "application/zip";
                case "PNG" -> "image/png";
                case "JPG", "JPEG" -> "image/jpeg";
                case "GIF" -> "image/gif";
                case "CSV" -> "text/csv";
                default -> inferContentTypeFromFilename(fileName);
            };
        }

        // Content type looks valid, return as-is
        return contentType;
    }

    /**
     * Infers MIME type from file extension
     */
    private String inferContentTypeFromFilename(String fileName) {
        if (ObjectUtils.isEmpty(fileName)) {
            return "application/octet-stream";
        }

        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            extension = fileName.substring(lastDotIndex + 1).toUpperCase();
        }

        return switch (extension) {
            case "PDF" -> "application/pdf";
            case "DOC" -> "application/msword";
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "XLS" -> "application/vnd.ms-excel";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PPT" -> "application/vnd.ms-powerpoint";
            case "PPTX" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "TXT" -> "text/plain";
            case "HTML", "HTM" -> "text/html";
            case "XML" -> "application/xml";
            case "JSON" -> "application/json";
            case "ZIP" -> "application/zip";
            case "PNG" -> "image/png";
            case "JPG", "JPEG" -> "image/jpeg";
            case "GIF" -> "image/gif";
            case "BMP" -> "image/bmp";
            case "SVG" -> "image/svg+xml";
            case "CSV" -> "text/csv";
            case "MP4" -> "video/mp4";
            case "MP3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    /**
     * Downloads file from URL using RestClient
     */
    private byte[] downloadFile(String fileUrl) {
        try {
            RestClient restClient = RestClient.create();
            byte[] fileData = restClient.get().uri(fileUrl).retrieve().toEntity(byte[].class).getBody();
            return fileData != null ? fileData : new byte[0];
        } catch (Exception e) {
            log.error("Error downloading file from URL: {}", fileUrl, e);
            throw new RuntimeException("Failed to download attachment", e);
        }
    }

    /**
     * @deprecated This method is deprecated in favor of fetching pre-rendered HTML templates from DMS based on template name and org ID. The new approach allows non-developers to create and manage email templates without code changes, providing greater flexibility and faster iterations.
     */
    @Deprecated
    private String renderThymeleafTemplate(String templateFileName, Map<String, Object> placeholders) {
        if (ObjectUtils.isEmpty(templateFileName)) {
            log.warn("No template file name provided, returning empty string");
            return "";
        }

        try {
            // Create Thymeleaf context with placeholders
            Context context = new Context();

            if (!CollectionUtils.isEmpty(placeholders)) {
                // Add all placeholders as context variables
                placeholders.forEach((key, value) -> {
                    context.setVariable(key, value != null ? value : "");
                    log.debug("Added template variable: {} = {}", key, value);
                });
            }

            // Process template - template name should not include .html extension
            String processedContent = templateEngine.process("emails/" + templateFileName, context);

            log.info("Successfully rendered Thymeleaf template: {}", templateFileName);
            return processedContent;

        } catch (Exception e) {
            log.error("Error rendering Thymeleaf template: {}", templateFileName, e);
            throw new RuntimeException("Failed to render email template: " + templateFileName, e);
        }
    }
}
