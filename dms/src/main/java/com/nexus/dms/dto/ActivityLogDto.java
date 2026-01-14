package com.nexus.dms.dto;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ActivityLogDto {
    private String requestUrl;

    private String httpMethod;

    private int responseStatus;

    private String request;

    private String response;

    private Long documentRecordId;

    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());
}
