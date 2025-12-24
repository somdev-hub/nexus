package com.nexus.iam.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDto {
    private String message;
    private int statusCode;
    private Timestamp timestamp;
    private String description;
}