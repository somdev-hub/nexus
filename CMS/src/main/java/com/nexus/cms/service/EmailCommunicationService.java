package com.nexus.cms.service;

import com.nexus.cms.model.enums.CommsStatus;
import com.nexus.cms.model.enums.CommsType;
import com.nexus.cms.payload.EmailAttachmentDto;
import com.nexus.cms.payload.EmailCommunicationDto;
import com.nexus.cms.util.CommonConstants;
import com.nexus.cms.util.WebConstants;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public void handleEmailCommunication(String message) {
        Map<String, Object> kafkaContent = null;
        EmailCommunicationDto emailCommunicationDto = null;
        try {
            kafkaContent = objectMapper.readValue(message, Map.class);
            if (kafkaContent.containsKey("message")) {
                // The message field contains a JSON string, so we need to parse it with
                // readValue
                String emailDtoJson = kafkaContent.get("message").toString();
                emailCommunicationDto = objectMapper.readValue(emailDtoJson, EmailCommunicationDto.class);
            }

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
                helper.setSubject(emailCommunicationDto.getSubject());

                String emailFilename = null;
                if (kafkaContent.containsKey("topic")
                        && kafkaContent.get("topic").equals(CommonConstants.CANDIDATE_SELECTION_MAIL_TOPIC)) {
                    emailFilename = CommonConstants.HR_INIT_EMAIL_FILE_NAME;
                } else if (kafkaContent.containsKey("topic")
                        && kafkaContent.get("topic").equals(CommonConstants.SALARY_PAYMENT_MAIL_TOPIC)) {
                    emailFilename = CommonConstants.SALARY_PAYMENT_EMAIL_FILE_NAME;
                }
                String processedBody = renderThymeleafTemplate(emailFilename, emailCommunicationDto.getPlaceholders());
                helper.setText(processedBody, true);
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
                        objectMapper.writeValueAsString(emailCommunicationDto.getPlaceholders()),
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
                        objectMapper.writeValueAsString(emailCommunicationDto.getPlaceholders()),
                        kafkaContent.get("uuid").toString(), CommsStatus.FAILED);
                log.error("Error sending email communication: {}", e.getMessage(), e);
            } catch (Exception e) {
                assert emailCommunicationDto != null;
                loggerService.log("HR", emailCommunicationDto.getRecipientEmails(), emailCommunicationDto.getCcEmails(),
                        emailCommunicationDto.getBccEmails(), null, CommsType.EMAIL,
                        objectMapper.writeValueAsString(emailCommunicationDto.getPlaceholders()),
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

        if (ObjectUtils.isEmpty(dto.getSubject()) || dto.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject cannot be empty");
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

                // Download file from URL and attach
                byte[] fileData = downloadFile(attachment.getFileUrl());

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
     * Renders Thymeleaf template with provided placeholders/context variables
     * Templates should be placed in src/main/resources/templates/emails/
     *
     * @param templateFileName The name of the template file (e.g., "hr-init-email")
     * @param placeholders     Map containing context variables for template
     *                         rendering
     * @return The rendered HTML content
     */
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
