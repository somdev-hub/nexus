package com.nexus.dms.dto;

import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.entities.DocumentType;
import com.nexus.dms.entities.OrgType;

import lombok.Data;

@Data
public class OrgFileUploadDto {

    private String fileName;

    private Long orgId;

    private String remarks;

//    private MultipartFile file;

    private DocumentType documentType;

    private OrgType orgType;
}
