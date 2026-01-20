package com.nexus.hr.payload;

import lombok.Data;

import java.util.List;

@Data
public class EmailCommunicationDto {

    private String senderEmail;

    private List<String> recipientEmails;

    private String subject;

    private String body;

    private List<String> ccEmails;
    private List<String> bccEmails;

    private List<EmailAttachmentDto> attachments;


}
