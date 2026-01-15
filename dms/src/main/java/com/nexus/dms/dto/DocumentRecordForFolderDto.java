package com.nexus.dms.dto;

import com.nexus.dms.entities.UploaderType;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class DocumentRecordForFolderDto {
    private Long documentId;
    private String documentName;
    private String documentType;
    private String dmsId;
    private String documentUrl;
    private Timestamp uploadedAt;
    private UploaderType uploaderType;
}
