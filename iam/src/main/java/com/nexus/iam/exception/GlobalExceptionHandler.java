package com.nexus.iam.exception;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nexus.iam.dto.ErrorResponseDto;

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

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "Unauthorized",
                ex.getStatus().value(),
                ex.getTimestamp(),
                ex.getMessage(),
                ex.getDetails());

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

}