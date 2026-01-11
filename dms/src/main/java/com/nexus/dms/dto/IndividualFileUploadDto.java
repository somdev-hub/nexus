package com.nexus.dms.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class IndividualFileUploadDto {

    private MultipartFile file;

    private String fileName;

    private Long userId;

    private String remarks;
    
}
