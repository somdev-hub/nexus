package com.nexus.dms.exception;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
public class ServiceLevelException extends RuntimeException {

    private final String serviceName;
    private final String message;
    private final Integer statusCode;
    private final String serviceMethod;
    private final Timestamp timestamp;
    private final String exceptionType;
    private final String description;
    private final String microservice;

    public ServiceLevelException(String serviceName, String message, String serviceMethod,
                                 String exceptionType, String description) {
        super(String.format("Service: %s, Method: %s, Error: %s", serviceName, serviceMethod, message));
        this.serviceName = serviceName;
        this.message = message;
        this.serviceMethod = serviceMethod;
        this.exceptionType = exceptionType;
        this.description = description;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.microservice = "DMS";
        this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

}
