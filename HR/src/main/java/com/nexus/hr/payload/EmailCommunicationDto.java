package com.nexus.hr.payload;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EmailCommunicationDto {

    private String senderEmail;

    private List<String> recipientEmails;

    private String subject;

    private String body;

    private List<String> ccEmails;
    private List<String> bccEmails;

    private List<EmailAttachmentDto> attachments;

    /**
     * Map of placeholder keys to their replacement values
     * Example: {"name": "John Doe", "employeeId": "12345"}
     * These will replace {name} and {employeeId} in the email body
     */
    private Map<String, Object> placeholders;

}
