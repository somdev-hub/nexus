package com.nexus.hr.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.hr.model.entities.HrLogs;
import com.nexus.hr.exception.ServiceLevelException;
import com.nexus.hr.repository.HrLogsRepo;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
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
     *
     * @param requestUrl The API endpoint URL
     * @param httpMethod The HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param httpStatus The HTTP response status code
     * @param request The request body (can be a DTO object or String)
     * @param response The response body (can be any object or String)
     * @param hrId The ID of the document (if available)
     */
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
        } catch (Exception e) {
            throw new ServiceLevelException("Logger", "Failed to save logs", "saveLogs", e.getClass().getSimpleName(),
                    e.getLocalizedMessage());
        }
    }

    /**
     * Helper method to serialize objects to JSON
     * If object is already a String, returns it as-is
     * Otherwise, serializes the object to JSON
     *
     * @param obj The object to serialize
     * @return JSON string or null if object is null
     */
    private String serializeObject(Object obj) {
        if (obj == null) {
            return null;
        }

        // If already a string, return as-is (already serialized)
        if (obj instanceof String) {
            return (String) obj;
        }

        // Otherwise, serialize to JSON
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException _) {
            // Fallback to toString if JSON serialization fails
            return obj.toString();
        }
    }
}