package com.nexus.dms.dto;

import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.entities.DocumentType;
import com.nexus.dms.entities.OrgType;

import lombok.Data;

@Data
public class CommonFileUploadDto {
    private String fileName;

    private Long orgId;

    private Long userId;

    private String remarks;

//    private MultipartFile file;

    private DocumentType documentType;

    private OrgType orgType;
}
