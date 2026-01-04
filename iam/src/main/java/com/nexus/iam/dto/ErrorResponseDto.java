package com.nexus.iam.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
    private String message;
    private int statusCode;
    private Timestamp timestamp;
    private String description;
    private String serviceName;
    private String serviceMethod;
    private String exceptionType;
    private final String microservice = "IAM";

    public ErrorResponseDto(String message, int statusCode, Timestamp timestamp, String description) {
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.description = description;
    }
}