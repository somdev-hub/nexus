package com.nexus.iam.exception;

import java.sql.Timestamp;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nexus.iam.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 4xx
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponseDto handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponseDto("BAD_REQUEST", HttpStatus.BAD_REQUEST, new Timestamp(new Date().getTime()),
                ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponseDto handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ErrorResponseDto("NOT_FOUND", HttpStatus.NOT_FOUND, new Timestamp(new Date().getTime()),
                ex.getMessage());
    }

}
