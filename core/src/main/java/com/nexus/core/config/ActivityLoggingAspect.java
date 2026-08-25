package com.nexus.core.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.annotation.LogActivity;
import com.nexus.core.utils.Logger;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.sql.Timestamp;

/**
 * AOP Aspect for automatic activity logging
 * Intercepts all methods annotated with @LogActivity
 * Logs all requests, responses, and exceptions to the database
 */
@Aspect
@Component
public class ActivityLoggingAspect {

	private final Logger logger;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ActivityLoggingAspect(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Around advice for logging all annotated methods
	 * Captures:
	 * - Request URL and HTTP method
	 * - Request body from method arguments (DTOs)
	 * - Response status and body
	 * - Exception details (type, message, status code)
	 * - DocumentRecordId from successful responses
	 *
	 * @param joinPoint   The method execution join point
	 * @param logActivity The annotation metadata
	 * @return The result from the intercepted method
	 * @throws Throwable If the intercepted method throws an exception
	 */
	@Around("@annotation(logActivity)")
	public Object logActivity(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

		if (request == null) {
			return joinPoint.proceed();
		}

		// Capture full request URL including query parameters
		String requestUrl = request.getRequestURI();
		if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
			requestUrl = requestUrl + "?" + request.getQueryString();
		}

		String httpMethod = request.getMethod();
		Timestamp createdOn = new Timestamp(System.currentTimeMillis());

		// Extract request body from method arguments (DTOs)
		String requestBody = extractRequestBody(joinPoint);

		Object result = null;
		Exception caughtException = null;
		int responseStatus = HttpStatus.OK.value();
		String responseBody = null;
		Long userId = null;

		try {
			// Execute the actual method
			result = joinPoint.proceed();

			// Handle successful response
			if (result instanceof ResponseEntity<?>) {
				ResponseEntity<?> response = (ResponseEntity<?>) result;
				responseStatus = response.getStatusCode().value();

				// Serialize response body
				if (response.getBody() != null) {
					try {
						responseBody = objectMapper.writeValueAsString(response.getBody());
					} catch (JsonProcessingException ex) {
						responseBody = response.getBody().toString();
					}
				}
			}

		} catch (Exception e) {
			// Capture exception details
			caughtException = e;
			responseStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();

			// Serialize exception message
			String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			try {
				responseBody = objectMapper.writeValueAsString(
						new ErrorDetails(e.getClass().getSimpleName(), errorMessage));
			} catch (JsonProcessingException ex) {
				responseBody = errorMessage;
			}
		}

		// Save log to database in all cases (success or failure)
		try {
			HttpStatus httpStatus = HttpStatus.valueOf(responseStatus);

			logger.saveLogs(
					requestUrl,
					HttpMethod.valueOf(httpMethod),
					httpStatus,
					requestBody,
					responseBody,
					userId);
		} catch (Exception logException) {
			// Log saving failed, but don't break the response
			System.err.println("Failed to save activity log: " + logException.getMessage());
		}

		// If an exception was caught, re-throw it so GlobalExceptionHandler can handle
		// it
		if (caughtException != null) {
			throw caughtException;
		}

		return result;
	}

	/**
	 * Helper class to structure exception information for JSON serialization
	 */
	private static class ErrorDetails {
		public final String exceptionType;
		public final String message;

		ErrorDetails(String exceptionType, String message) {
			this.exceptionType = exceptionType;
			this.message = message;
		}
	}

	/**
	 * Extract request body from method arguments
	 * Looks for DTO objects and serializes them to JSON
	 *
	 * @param joinPoint The method execution join point
	 * @return Serialized request body or null
	 */
	private String extractRequestBody(ProceedingJoinPoint joinPoint) {
		try {
			Object[] args = joinPoint.getArgs();
			if (args == null || args.length == 0) {
				return null;
			}

			// Look for DTO arguments (skip MultipartFile and other non-serializable
			// objects)
			for (Object arg : args) {
				if (arg == null) {
					continue;
				}

				String className = arg.getClass().getSimpleName();

				// Check if it's a DTO (ends with Dto or contains common DTO patterns)
				if (className.endsWith("Dto") ||
						className.contains("FileUpload") ||
						className.equals("IndividualFileUploadDto") ||
						className.equals("OrgFileUploadDto") ||
						className.equals("CommonFileUploadDto")) {

					try {
						return objectMapper.writeValueAsString(arg);
					} catch (JsonProcessingException ex) {
						// Skip this object and continue looking
					}
				}
			}
		} catch (Exception e) {
			// Ignore extraction errors
		}
		return null;
	}
}