package com.nexus.dms.dto;

import java.sql.Timestamp;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class FileValidationExceptionDto {
    private String message;
    private HttpStatus status;
    private String fileExceptionType;
    private String fileName;
    private String details;
    private Timestamp timestamp;
}
