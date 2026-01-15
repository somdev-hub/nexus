package com.nexus.dms.exception;

import java.io.Serial;
import java.sql.Timestamp;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileValidationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String message;
    private final HttpStatus status;
    private final FileExceptionType fileExceptionType;
    private final String fileName;
    private final String details;
    private final Timestamp timestamp;


}
