package com.nexus.core.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationException extends RuntimeException {

	private String fieldName;
	private Object fieldValue;
	private String message;

	public ValidationException(String message) {
		super(message);
		this.message = message;
	}

	public ValidationException(String fieldName, Object fieldValue, String message) {
		super(String.format("Validation failed for %s : '%s' - %s", fieldName, fieldValue, message));
		this.fieldName = fieldName;
		this.fieldValue = fieldValue;
		this.message = message;
	}
}