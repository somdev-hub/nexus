package com.nexus.iam.exception;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.nexus.iam.dto.ErrorResponseDto;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private Timestamp now() {
		return Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	}

	// 4xx - client errors return only message, never stacktrace
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponseDto> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
		log.warn("Missing header: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Bad Request",
				HttpStatus.BAD_REQUEST.value(),
				now(),
				ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(NoResourceFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Not Found",
				HttpStatus.NOT_FOUND.value(),
				now(),
				"No static resource " + ex.getResourcePath() + " or endpoint not found.");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b).orElse("Validation failed");
		log.warn("Validation failed: {}", msg);
		ErrorResponseDto body = new ErrorResponseDto(
				"Bad Request",
				HttpStatus.BAD_REQUEST.value(),
				now(),
				msg);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(ConstraintViolationException ex) {
		log.warn("Constraint violation: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Bad Request",
				HttpStatus.BAD_REQUEST.value(),
				now(),
				ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
		log.warn("Malformed request: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Bad Request",
				HttpStatus.BAD_REQUEST.value(),
				now(),
				"Malformed JSON request");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponseDto> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
		log.warn("Method not supported: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Method Not Allowed",
				HttpStatus.METHOD_NOT_ALLOWED.value(),
				now(),
				ex.getMessage());
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
	}

	// 4xx
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Bad Request",
				HttpStatus.BAD_REQUEST.value(),
				now(),
				ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
		ErrorResponseDto errorResponse = new ErrorResponseDto(
				"Resource Not Found",
				HttpStatus.NOT_FOUND.value(),
				Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))),
				ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
	}

	@ExceptionHandler(ServiceLevelException.class)
	public ResponseEntity<ErrorResponseDto> handleServiceLevelException(ServiceLevelException ex) {
		ErrorResponseDto errorResponse = new ErrorResponseDto(
				ex.getExceptionType(),
				ex.getStatusCode(),
				ex.getTimestamp(),
				ex.getMessage(),
				ex.getDescription(),
				ex.getServiceName(),
				ex.getServiceMethod());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponseDto> handleIllegalStateException(IllegalStateException ex) {
		log.warn("Invalid state: {}", ex.getMessage());
		ErrorResponseDto body = new ErrorResponseDto(
				"Invalid State", HttpStatus.BAD_REQUEST.value(),
				now(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ErrorResponseDto> handleUnauthorizedException(UnauthorizedException ex,
			HttpServletRequest request) {
		log.warn("Unauthorized: {}", ex.getMessage());
		ErrorResponseDto errorResponse = new ErrorResponseDto(
				"Unauthorized",
				ex.getStatus().value(),
				ex.getTimestamp(),
				ex.getMessage(),
				ex.getDetails());

		return ResponseEntity.status(ex.getStatus()).body(errorResponse);
	}

	// Fallback - never expose stacktrace to client, log it server-side only
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
		log.error("Unhandled exception", ex);
		ErrorResponseDto body = new ErrorResponseDto(
				"Internal Server Error",
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				now(),
				ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}

}