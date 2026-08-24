package com.nexus.core.exception;

/**
 * Exception thrown when organization context validation fails.
 * <p>
 * Used by OrganizationContextFilter to signal organization access violations.
 */
public class OrganizationContextException extends RuntimeException {

	public OrganizationContextException(String message) {
		super(message);
	}

	public OrganizationContextException(String message, Throwable cause) {
		super(message, cause);
	}
}