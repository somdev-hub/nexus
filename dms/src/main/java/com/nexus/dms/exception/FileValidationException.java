package com.nexus.dms.exception;

import java.sql.Timestamp;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private String message;
    private final HttpStatus status = HttpStatus.BAD_REQUEST;
    private FileExceptionType fileExceptionType;
    private String fileName;
    private String details;
    private Timestamp timestamp;

}
