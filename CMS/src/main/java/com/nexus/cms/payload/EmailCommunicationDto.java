package com.nexus.cms.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailCommunicationDto {
    private String senderEmail;

    private List<String> recipientEmails;

    private String subject;

    private List<String> ccEmails;

    private List<String> bccEmails;

    private List<EmailAttachmentDto> attachments;

    /**
     * Map of placeholder keys to their replacement values
     * These will be passed as context variables to the Thymeleaf template
     * Example: {"name": "John Doe", "employeeId": "12345", "organizationName":
     * "Nexus Corp"}
     */
    private Map<String, Object> placeholders;
}
