package com.nexus.dms.exception;

import java.sql.Timestamp;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private Timestamp timestamp;
    private String message;
    private HttpStatus status;
    private String details;

    public UnauthorizedException(String message, String details) {
        super(message);
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.status = HttpStatus.UNAUTHORIZED;
        this.message = message;
        this.details = details;
    }
}
