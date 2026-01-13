package com.nexus.dms.dto;

import lombok.Data;

@Data
public class FolderListDto {

    private Long id;

    private String folderName;

    private String region;

    private String createdBy;

    private String createdAt;

    private String orgType;

}
