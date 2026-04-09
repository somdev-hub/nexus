package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailAttachmentDto {

    private String fileName;

    private String contentType;

    private String fileUrl;
}
