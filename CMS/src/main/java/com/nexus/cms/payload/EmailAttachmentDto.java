package com.nexus.cms.payload;

import lombok.Data;

@Data
public class EmailAttachmentDto {
    private String fileName;

    private String contentType;

    private String fileUrl;
}
