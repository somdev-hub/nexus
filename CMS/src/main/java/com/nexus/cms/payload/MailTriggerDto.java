package com.nexus.cms.payload;

import lombok.Data;

import java.util.List;

@Data
public class MailTriggerDto {

    private String templateName;
    private Long orgId;
    private List<String> recipientEmails;
    private List<TemplateParamDto> templateParams;

    @Data
    public static class TemplateParamDto {
        private String key;
        private String value;
    }
}
