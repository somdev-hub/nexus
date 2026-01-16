package com.nexus.iam.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.ActivityLogDto;
import com.nexus.iam.entities.User;
import com.nexus.iam.utils.Logger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
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

        ActivityLogDto activityLog = new ActivityLogDto();

        // Capture full request URL including query parameters
        String requestUrl = request.getRequestURI();
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            requestUrl = requestUrl + "?" + request.getQueryString();
        }
        activityLog.setRequestUrl(requestUrl);

        activityLog.setHttpMethod(request.getMethod());
        activityLog.setCreatedOn(new Timestamp(System.currentTimeMillis()));

        // Extract request body from method arguments (DTOs)
        extractAndSetRequestBody(joinPoint, activityLog);

        Object result = null;
        Exception caughtException = null;

        try {
            // Execute the actual method
            result = joinPoint.proceed();

            // Handle successful response
            if (result instanceof ResponseEntity<?>) {
                ResponseEntity<?> response = (ResponseEntity<?>) result;
                activityLog.setResponseStatus(response.getStatusCode().value());

                // Serialize response body
                if (response.getBody() != null) {
                    try {
                        activityLog.setResponse(objectMapper.writeValueAsString(response.getBody()));

                        // Extract documentRecordId from DocumentRecord response
                        if (response.getBody() instanceof User user) {
                            activityLog.setUserId(user.getId());
                        }
                    } catch (JsonProcessingException _) {
                        if (response.getBody() != null) {
                            activityLog.setResponse(response.getBody().toString());
                        }
                    }
                }
            }

        } catch (Exception e) {
            // Capture exception details
            caughtException = e;
            activityLog.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            // Serialize exception message
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            try {
                activityLog.setResponse(objectMapper.writeValueAsString(
                        new ErrorDetails(e.getClass().getSimpleName(), errorMessage)
                ));
            } catch (JsonProcessingException _) {
                activityLog.setResponse(errorMessage);
            }
        }

        // Save log to database in all cases (success or failure)
        try {
            HttpStatus httpStatus = HttpStatus.valueOf(activityLog.getResponseStatus());

            logger.saveLogs(
                    activityLog.getRequestUrl(),
                    HttpMethod.valueOf(activityLog.getHttpMethod()),
                    httpStatus,
                    activityLog.getRequest(),  // ✓ NOW PASSING REQUEST BODY
                    activityLog.getResponse(),
                    activityLog.getUserId()
            );
        } catch (Exception logException) {
            // Log saving failed, but don't break the response
            System.err.println("Failed to save activity log: " + logException.getMessage());
        }

        // If an exception was caught, re-throw it so GlobalExceptionHandler can handle it
        if (caughtException != null) {
            throw caughtException;
        }

        return result;
    }

    /**
     * Helper class to structure exception information for JSON serialization
     */
    @Getter
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
     * Looks for DTO objects (IndividualFileUploadDto, OrgFileUploadDto, CommonFileUploadDto)
     * and serializes them to JSON
     *
     * @param joinPoint   The method execution join point
     * @param activityLog The log DTO to update
     */
    private void extractAndSetRequestBody(ProceedingJoinPoint joinPoint, ActivityLogDto activityLog) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return;
            }

            // Look for DTO arguments (skip MultipartFile and other non-serializable objects)
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
                        String requestJson = objectMapper.writeValueAsString(arg);
                        activityLog.setRequest(requestJson);
                        break; // We found the main DTO, don't need to continue
                    } catch (JsonProcessingException _) {
                        // Skip this object and continue looking
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail - don't break request logging if extraction fails
            System.err.println("Warning: Failed to extract request body: " + e.getMessage());
        }
    }
}