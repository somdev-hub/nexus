package com.nexus.nexusbuddy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.model.entities.NexusBuddyLogs;
import com.nexus.nexusbuddy.repository.NexusBuddyLogsRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class Logger {

    private final NexusBuddyLogsRepo nexusBuddyLogsRepo;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Logger(NexusBuddyLogsRepo nexusBuddyLogsRepo) {
        this.nexusBuddyLogsRepo = nexusBuddyLogsRepo;
    }

    /**
     * Save logs to database
     * Handles both request and response objects
     * Serializes objects to JSON if they're not already serialized
     * Runs in a new transaction to ensure logging works even if parent transaction
     * fails
     *
     * @param requestUrl   The API endpoint URL
     * @param httpMethod   The HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param httpStatus   The HTTP response status code
     * @param request      The request body (can be a DTO object or String)
     * @param response     The response body (can be any object or String)
     * @param clientConfig The ClientConfig entity (if available)
     * @param toolName     The name of the tool being called (if available)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogs(String requestUrl, HttpMethod httpMethod, HttpStatus httpStatus, Object request,
            Object response, ClientConfig clientConfig, String toolName) {
        try {
            NexusBuddyLogs nexusBuddyLogs = new NexusBuddyLogs();
            nexusBuddyLogs.setRequestUrl(requestUrl);
            nexusBuddyLogs.setHttpMethod(httpMethod.name());
            nexusBuddyLogs.setResponseStatus(httpStatus.value());

            // Serialize request body (handle both objects and already-serialized strings)
            nexusBuddyLogs.setRequest(serializeObject(request));

            // Serialize response body (handle both objects and already-serialized strings)
            nexusBuddyLogs.setResponse(serializeObject(response));

            nexusBuddyLogs.setClientConfig(clientConfig);
            nexusBuddyLogs.setToolName(toolName);

            nexusBuddyLogsRepo.save(nexusBuddyLogs);

            log.debug("Successfully saved log for {} {} with status {}", httpMethod, requestUrl, httpStatus);
        } catch (Exception e) {
            // Log the error but don't throw exception to prevent disrupting the main flow
            log.error("Failed to save logs for {} {}: {}", httpMethod, requestUrl, e.getMessage(), e);
            // Don't throw ServiceLevelException here as it would disrupt the main business
            // logic
        }
    }

    /**
         * Helper method to serialize objects to JsonNode
         * Always returns valid JsonNode for PostgreSQL jsonb columns
         * Strings are parsed as JSON, objects are serialized to JSON
     *
     * @param obj The object to serialize
         * @return JsonNode or null if object is null
     */
        private JsonNode serializeObject(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
                // If it's already a JsonNode, return as-is
                if (obj instanceof JsonNode) {
                    return (JsonNode) obj;
                }

                // If it's a String, try to parse it as JSON
                if (obj instanceof String) {
                    try {
                        return objectMapper.readTree((String) obj);
                    } catch (JsonProcessingException e) {
                        // If it's not valid JSON, wrap it as a JSON string
                        return objectMapper.valueToTree(obj);
                    }
                }

                // For other objects, serialize to JSON
                return objectMapper.valueToTree(obj);
            } catch (Exception e) {
                // If serialization fails, wrap the toString() result as a JSON string
                log.warn("Failed to serialize object to JSON, using toString(): {}", e.getMessage());
                return objectMapper.valueToTree("Serialization failed: " + obj.getClass().getSimpleName());
            }
        }
}