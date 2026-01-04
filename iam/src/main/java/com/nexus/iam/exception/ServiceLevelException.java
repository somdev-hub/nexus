package com.nexus.iam.exception;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ServiceLevelException extends RuntimeException {

    private String serviceName;
    private String message;
    private final Integer statusCode = 500;
    private String serviceMethod;
    private Timestamp timestamp;
    private String exceptionType;
    private String description;
    private final String microservice = "IAM";

    public ServiceLevelException(String serviceName, String message, String serviceMethod,
            String exceptionType, String description) {
        super(String.format("Service: %s, Method: %s, Error: %s", serviceName, serviceMethod, message));
        this.serviceName = serviceName;
        this.message = message;
        this.serviceMethod = serviceMethod;
        this.exceptionType = exceptionType;
        this.description = description;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

}
