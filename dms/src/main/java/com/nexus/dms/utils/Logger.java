package com.nexus.dms.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.dms.entities.DmsLogs;
import com.nexus.dms.exception.ServiceLevelException;
import com.nexus.dms.repository.DmsLogsRepo;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class Logger {

    private final DmsLogsRepo dmsLogsRepo;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Logger(DmsLogsRepo dmsLogsRepo) {
        this.dmsLogsRepo = dmsLogsRepo;
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
     * @param documentRecordId The ID of the document (if available)
     * @throws JsonProcessingException If serialization fails
     */
    public void saveLogs(String requestUrl, HttpMethod httpMethod, HttpStatus httpStatus, Object request,
            Object response, Long documentRecordId) throws JsonProcessingException {
        try {
            DmsLogs dmsLogs = new DmsLogs();
            dmsLogs.setRequestUrl(requestUrl);
            dmsLogs.setHttpMethod(httpMethod.name());
            dmsLogs.setResponseStatus(httpStatus.value());

            // Serialize request body (handle both objects and already-serialized strings)
            dmsLogs.setRequest(serializeObject(request));

            // Serialize response body (handle both objects and already-serialized strings)
            dmsLogs.setResponse(serializeObject(response));

            dmsLogs.setDocumentRecordId(documentRecordId);
            dmsLogsRepo.save(dmsLogs);
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
     * @param payload The object to serialize
     * @return JSON string or null if object is null
     */
    private String serializeObject(Object payload) {
        try {
            if (payload instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) payload;
                // Create a copy to avoid serializing MultipartFile objects
                Map<String, Object> safeMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof MultipartFile) {
                        safeMap.put(entry.getKey(), "MultipartFile");
                    } else {
                        safeMap.put(entry.getKey(), entry.getValue());
                    }
                }
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(safeMap);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(payload);
            }
        } catch (Exception e) {
            return payload.toString();
        }
    }
}
