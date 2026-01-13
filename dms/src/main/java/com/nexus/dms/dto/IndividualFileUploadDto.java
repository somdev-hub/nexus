package com.nexus.dms.dto;

import org.springframework.web.multipart.MultipartFile;

import com.nexus.dms.entities.DocumentType;

import lombok.Data;

@Data
public class IndividualFileUploadDto {

    private MultipartFile file;

    private String fileName;

    private Long userId;

    private String remarks;

    private DocumentType documentType;
    
}
