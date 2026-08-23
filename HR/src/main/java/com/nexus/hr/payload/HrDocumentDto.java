package com.nexus.hr.payload;

import lombok.Data;

@Data
public class HrDocumentDto {
    private Long hrDocumentId;
    private String documentName;
    private String hrDocumentType;
    private String documentUrl;
}
