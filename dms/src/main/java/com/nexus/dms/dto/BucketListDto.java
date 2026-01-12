package com.nexus.dms.dto;

import lombok.Data;

@Data
public class BucketListDto {

    private Long id;

    private String bucketName;

    private String region;

    private String createdBy;

    private String createdAt;

    private String orgType;

}
