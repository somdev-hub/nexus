package com.nexus.cms.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexus.cms.model.enums.CommsType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailCommunicationDto {

    private String templateName;
    private Map<String, String> templateParams;
    private CommsType commType;

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
