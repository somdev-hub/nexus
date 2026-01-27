package com.nexus.hr.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.hr.model.entities.HrLogs;
import com.nexus.hr.repository.HrLogsRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class Logger {

    private final HrLogsRepo hrLogsRepo;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Logger(HrLogsRepo hrLogsRepo) {
        this.hrLogsRepo = hrLogsRepo;
    }

    /**
     * Save logs to database
     * Handles both request and response objects
     * Serializes objects to JSON if they're not already serialized
     * Runs in a new transaction to ensure logging works even if parent transaction fails
     *
     * @param requestUrl The API endpoint URL
     * @param httpMethod The HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param httpStatus The HTTP response status code
     * @param request The request body (can be a DTO object or String)
     * @param response The response body (can be any object or String)
     * @param hrId The ID of the document (if available)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogs(String requestUrl, HttpMethod httpMethod, HttpStatus httpStatus, Object request,
                         Object response, Long hrId) {
        try {
            HrLogs hrLogs = new HrLogs();
            hrLogs.setRequestUrl(requestUrl);
            hrLogs.setHttpMethod(httpMethod.name());
            hrLogs.setResponseStatus(httpStatus.value());

            // Serialize request body (handle both objects and already-serialized strings)
            hrLogs.setRequest(serializeObject(request));

            // Serialize response body (handle both objects and already-serialized strings)
            hrLogs.setResponse(serializeObject(response));

            hrLogs.setHrId(hrId);
            hrLogsRepo.save(hrLogs);

            log.debug("Successfully saved log for {} {} with status {}", httpMethod, requestUrl, httpStatus);
        } catch (Exception e) {
            // Log the error but don't throw exception to prevent disrupting the main flow
            log.error("Failed to save logs for {} {}: {}", httpMethod, requestUrl, e.getMessage(), e);
            // Don't throw ServiceLevelException here as it would disrupt the main business logic
        }
    }

    /**
     * Helper method to serialize objects to JSON
     * Always returns valid JSON format for PostgreSQL jsonb columns
     * Strings are wrapped in quotes, objects are serialized to JSON
     *
     * @param obj The object to serialize
     * @return Valid JSON string or null if object is null
     */
    private String serializeObject(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // Always serialize through ObjectMapper to ensure valid JSON
            // This handles both String and complex objects correctly
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            // If serialization fails, wrap the toString() result as a JSON string
            log.warn("Failed to serialize object to JSON, using toString(): {}", e.getMessage());
            try {
                // Manually create a valid JSON string by wrapping in quotes and escaping
                return objectMapper.writeValueAsString(obj.toString());
            } catch (JsonProcessingException ex) {
                // Last resort: return a simple JSON string with error message
                return "\"Serialization failed: " + obj.getClass().getSimpleName() + "\"";
            }
        }
    }
}