package com.nexus.hr.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

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
    private final String microservice = "HR";

    public ErrorResponseDto(String message, int statusCode, Timestamp timestamp, String description) {
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.description = description;
    }

    public ErrorResponseDto(String exceptionType, int statusCode, Timestamp timestamp, String message, String description) {
        this.exceptionType = exceptionType;
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = timestamp;
        this.description = description;
    }
}
