package com.nexus.iam.dto;

import java.sql.Timestamp;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDto {
    private String message;
    private HttpStatus status;
    private Timestamp timestamp;
    private String description;
}
