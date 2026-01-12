package com.nexus.dms.exception;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nexus.dms.dto.ErrorResponseDto;
import com.nexus.dms.dto.FileValidationExceptionDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 4xx
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponseDto handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponseDto(
                "Bad Request",
                HttpStatus.BAD_REQUEST.value(),
                Timestamp.valueOf(LocalDateTime.now()),
                ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                Timestamp.valueOf(LocalDateTime.now()),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ServiceLevelException.class)
    public ResponseEntity<ErrorResponseDto> handleServiceLevelException(ServiceLevelException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                ex.getExceptionType(),
                ex.getStatusCode(),
                ex.getTimestamp(),
                ex.getMessage(),
                ex.getDescription(),
                ex.getServiceName(),
                ex.getServiceMethod());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<FileValidationExceptionDto> handleFileValidationException(FileValidationException ex) {
        FileValidationExceptionDto dto = new FileValidationExceptionDto();
        dto.setMessage(ex.getMessage());
        dto.setStatus(ex.getStatus());
        dto.setFileExceptionType(ex.getFileExceptionType().name());
        dto.setFileName(ex.getFileName());
        dto.setDetails(ex.getDetails());
        dto.setTimestamp(ex.getTimestamp());

        return ResponseEntity.status(ex.getStatus()).body(dto);
    }

}