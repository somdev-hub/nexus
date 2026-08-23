package com.nexus.cms.payload;

import com.nexus.cms.model.enums.EventTemplateType;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ShortEventTemplatePayload {
    private Long eventTemplateId;
    private String templateName;
    private EventTemplateType eventTemplateType;
    private String templateHtmlUrl;
    private Long orgId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Integer numberOfParams;
}
