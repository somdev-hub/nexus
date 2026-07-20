package com.nexus.nexusbuddy.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigValidationException extends RuntimeException {
    private String fieldName;
    private Object fieldValue;
    private String errorCode;

    public ConfigValidationException(String message, String fieldName, Object fieldValue, String errorCode) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.errorCode = errorCode;
    }

    public ConfigValidationException(String message) {
        super(message);
    }
}