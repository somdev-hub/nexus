package com.nexus.hr.service.implementations;

import com.nexus.hr.model.enums.CommunicationStatus;
import com.nexus.hr.model.enums.CommunicationType;
import com.nexus.hr.model.entities.HrCommunication;
import com.nexus.hr.payload.EmailAttachmentDto;
import com.nexus.hr.payload.EmailCommunicationDto;
import com.nexus.hr.payload.ErrorResponseDto;
import com.nexus.hr.repository.HrCommunicationRepo;
import com.nexus.hr.service.interfaces.CommunicationService;
import com.nexus.hr.utils.Logger;
import com.nexus.hr.utils.WebConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationServiceImpl implements CommunicationService {

    private final JavaMailSender javaMailSender;
    private final Logger logger;
    private final HrCommunicationRepo hrCommunicationRepo;
    private final WebConstants webConstants;


    @Override
    public ResponseEntity<?> sendCommunicationOverEmail(EmailCommunicationDto emailCommunicationDto) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate input
            validateEmailCommunication(emailCommunicationDto);

            // Prepare email
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Set email properties
            String fromEmail = ObjectUtils.isEmpty(emailCommunicationDto.getSenderEmail())
                    ? webConstants.getDefaultFromEmail()
                    : emailCommunicationDto.getSenderEmail();

            helper.setFrom(fromEmail);
            helper.setTo(emailCommunicationDto.getRecipientEmails().toArray(new String[0]));
            helper.setSubject(emailCommunicationDto.getSubject());
            helper.setText(emailCommunicationDto.getBody(), true);

            // Add CC emails if present
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

            // Log successful operation to database
            logCommunicationToDatabase(emailCommunicationDto, CommunicationStatus.SENT);

            // Log to application logs
            logger.saveLogs(
                    "/communication/send-email",
                    HttpMethod.POST,
                    HttpStatus.OK,
                    emailCommunicationDto,
                    "Email sent successfully",
                    null
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("Email sent successfully to {} recipients in {}ms",
                    emailCommunicationDto.getRecipientEmails().size(), duration);

            Map<String, Object> successResponse = createSuccessResponse(emailCommunicationDto);
            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            return handleValidationError(emailCommunicationDto, e);
        } catch (MessagingException e) {
            logCommunicationToDatabase(emailCommunicationDto, CommunicationStatus.FAILED);
            return handleMessagingError(emailCommunicationDto, e);
        } catch (Exception e) {
            logCommunicationToDatabase(emailCommunicationDto, CommunicationStatus.FAILED);
            return handleGenericError(emailCommunicationDto, e);
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

        if (ObjectUtils.isEmpty(dto.getBody()) || dto.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Email body cannot be empty");
        }

        if (dto.getRecipientEmails().size() > webConstants.getMaxRecipients()) {
            throw new IllegalArgumentException(
                    String.format("Number of recipients exceeds maximum limit of %d", webConstants.getMaxRecipients())
            );
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
        List<String> invalidEmails = emails.stream()
                .filter(email -> !email.matches(emailRegex))
                .toList();

        if (!invalidEmails.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s email format: %s", type, String.join(", ", invalidEmails))
            );
        }
    }

    /**
     * Attaches files to email from provided URLs
     */
    private void attachFilesToEmail(MimeMessageHelper helper, List<EmailAttachmentDto> attachments)
            throws MessagingException {
        for (EmailAttachmentDto attachment : attachments) {
            try {
                if (ObjectUtils.isEmpty(attachment.getFileUrl()) ||
                        ObjectUtils.isEmpty(attachment.getFileName())) {
                    log.warn("Skipping attachment with missing file URL or name");
                    continue;
                }

                // Download file from URL and attach
                byte[] fileData = downloadFile(attachment.getFileUrl());
                helper.addAttachment(
                        attachment.getFileName(),
                        () -> new java.io.ByteArrayInputStream(fileData),
                        attachment.getContentType()
                );

                log.debug("Attached file: {}", attachment.getFileName());
            } catch (Exception e) {
                log.warn("Failed to attach file: {}", attachment.getFileName(), e);
                // Continue with other attachments even if one fails
            }
        }
    }

    /**
     * Downloads file from URL using RestClient
     */
    private byte[] downloadFile(String fileUrl) {
        try {
            RestClient restClient = RestClient.create();
            byte[] fileData = restClient.get()
                    .uri(fileUrl)
                    .retrieve()
                    .toEntity(byte[].class)
                    .getBody();
            return fileData != null ? fileData : new byte[0];
        } catch (Exception e) {
            log.error("Error downloading file from URL: {}", fileUrl, e);
            throw new RuntimeException("Failed to download attachment", e);
        }
    }

    /**
     * Handles validation errors
     */
    private ResponseEntity<?> handleValidationError(EmailCommunicationDto dto,
                                                    IllegalArgumentException e) {
        log.error("Validation error while sending email: {}", e.getMessage());

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST.value(),
                new Timestamp(System.currentTimeMillis()),
                e.getMessage(),
                "Invalid email communication parameters"
        );

        logger.saveLogs(
                "/communication/send-email",
                HttpMethod.POST,
                HttpStatus.BAD_REQUEST,
                dto,
                e.getMessage(),
                null
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles messaging errors from JavaMailSender
     */
    private ResponseEntity<?> handleMessagingError(EmailCommunicationDto dto,
                                                   MessagingException e) {
        log.error("Error sending email: {}", e.getMessage(), e);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "MESSAGING_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                new Timestamp(System.currentTimeMillis()),
                "Failed to send email",
                e.getMessage()
        );

        logger.saveLogs(
                "/communication/send-email",
                HttpMethod.POST,
                HttpStatus.INTERNAL_SERVER_ERROR,
                dto,
                e.getMessage(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles generic/unexpected errors
     */
    private ResponseEntity<?> handleGenericError(EmailCommunicationDto dto,
                                                 Exception e) {
        log.error("Unexpected error while sending email: {}", e.getMessage(), e);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                e.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                new Timestamp(System.currentTimeMillis()),
                "An unexpected error occurred while sending email",
                e.getMessage()
        );

        logger.saveLogs(
                "/communication/send-email",
                HttpMethod.POST,
                HttpStatus.INTERNAL_SERVER_ERROR,
                dto,
                e.getMessage(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Saves communication record to database for audit trail and monitoring
     */
    private void logCommunicationToDatabase(EmailCommunicationDto dto, CommunicationStatus status) {
        try {
            HrCommunication communication = new HrCommunication();
            communication.setCommunicationType(CommunicationType.EMAIL);
            communication.setSubject(dto.getSubject());
            communication.setBody(dto.getBody());
            communication.setSenderId(ObjectUtils.isEmpty(dto.getSenderEmail()) ? webConstants.getDefaultFromEmail() : dto.getSenderEmail());
            communication.setReceiverIds(dto.getRecipientEmails());
            communication.setCcEmails(dto.getCcEmails());
            communication.setBccEmails(dto.getBccEmails());
            communication.setStatus(status);
            communication.setTimestamp(new Timestamp(System.currentTimeMillis()));

            if (!CollectionUtils.isEmpty(dto.getAttachments())) {
                communication.setAttachments(
                        dto.getAttachments().stream()
                                .map(EmailAttachmentDto::getFileName)
                                .toList()
                );
            }

            hrCommunicationRepo.save(communication);
            log.debug("Communication logged to database with status: {}", status);
        } catch (Exception e) {
            log.warn("Failed to log communication to database: {}", e.getMessage());
            // Don't throw exception - this is a non-critical operation
        }
    }

    /**
     * Creates success response object with relevant details
     */
    private Map<String, Object> createSuccessResponse(EmailCommunicationDto dto) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Email sent successfully");
        response.put("timestamp", new Timestamp(System.currentTimeMillis()));
        response.put("recipientCount", dto.getRecipientEmails().size());
        response.put("subject", dto.getSubject());
        response.put("hasAttachments", !CollectionUtils.isEmpty(dto.getAttachments()));
        if (!CollectionUtils.isEmpty(dto.getAttachments())) {
            response.put("attachmentCount", dto.getAttachments().size());
        }
        return response;
    }
}
