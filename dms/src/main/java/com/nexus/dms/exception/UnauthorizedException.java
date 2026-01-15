package com.nexus.dms.exception;

import java.io.Serial;
import java.sql.Timestamp;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UnauthorizedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final Timestamp timestamp;
    private final String message;
    private final HttpStatus status;
    private final String details;

    public UnauthorizedException(String message, String details) {
        super(message);
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.status = HttpStatus.UNAUTHORIZED;
        this.message = message;
        this.details = details;
    }

}
