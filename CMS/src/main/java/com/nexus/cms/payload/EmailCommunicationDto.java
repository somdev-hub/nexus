package com.nexus.cms.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
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
     * Example: {"name": "John Doe", "employeeId": "12345", "organizationName": "Nexus Corp"}
     */
    private Map<String, Object> placeholders;
}
