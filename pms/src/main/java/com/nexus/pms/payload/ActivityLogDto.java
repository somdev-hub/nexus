package com.nexus.pms.payload;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class ActivityLogDto {
    private String requestUrl;

    private String httpMethod;

    private int responseStatus;

    private String request;

    private String response;

    private Long userId;

    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());
}
