package com.nexus.nexusbuddy.exception;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nexus.nexusbuddy.payload.ErrorResponseDto;

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

    @ExceptionHandler(ConfigNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleConfigNotFoundException(ConfigNotFoundException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "Resource Not Found",
                HttpStatus.NOT_FOUND.value(),
                Timestamp.valueOf(LocalDateTime.now()),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ConfigValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleConfigValidationException(ConfigValidationException ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "Validation Error",
                HttpStatus.BAD_REQUEST.value(),
                Timestamp.valueOf(LocalDateTime.now()),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 5xx
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Timestamp.valueOf(LocalDateTime.now()),
                ex.getMessage(),
                "An unexpected error occurred: " + ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
