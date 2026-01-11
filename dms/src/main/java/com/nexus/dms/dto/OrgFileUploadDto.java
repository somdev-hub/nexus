package com.nexus.dms.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class OrgFileUploadDto {

    private String fileName;

    private Long orgId;

    private String remarks;

    private MultipartFile file;
}
