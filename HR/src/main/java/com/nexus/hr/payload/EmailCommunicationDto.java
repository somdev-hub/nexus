package com.nexus.hr.payload;

import com.nexus.hr.model.enums.CommType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EmailCommunicationDto {

    private String templateName;
    private Map<String, String> templateParams;
    private CommType commType;

    private String senderEmail;
    private List<String> recipientEmails;
    private List<String> ccEmails;
    private List<String> bccEmails;
    private Long orgId;

    private List<EmailAttachmentDto> attachments;

    @Deprecated
    private String subject;
    @Deprecated
    private String body;
    @Deprecated
    private Map<String, Object> placeholders;

}
