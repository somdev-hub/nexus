package com.nexus.dms.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.dms.entities.DmsLogs;
import com.nexus.dms.exception.ServiceLevelException;
import com.nexus.dms.repository.DmsLogsRepo;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class Logger {

    private final DmsLogsRepo dmsLogsRepo;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Logger(DmsLogsRepo dmsLogsRepo) {
        this.dmsLogsRepo = dmsLogsRepo;
    }

    /**
     * Save logs to database asynchronously
     * Uses manual transaction management to avoid connection pool conflicts
     * Silently handles failures since logging is non-critical
     */
    @Async
    public void saveLogs(String requestUrl, HttpMethod httpMethod, HttpStatus httpStatus, Object request,
            Object response, Long documentRecordId) {
        try {
            DmsLogs dmsLogs = new DmsLogs();
            dmsLogs.setRequestUrl(requestUrl);
            dmsLogs.setHttpMethod(httpMethod.name());
            dmsLogs.setResponseStatus(httpStatus.value());
            dmsLogs.setRequest(serializeObject(request));
            dmsLogs.setResponse(serializeObject(response));
            dmsLogs.setDocumentRecordId(documentRecordId);

            // Use repository which manages its own transaction
            // HikariCP will deallocate prepared statements automatically
            dmsLogsRepo.save(dmsLogs);
            log.debug("Activity log saved successfully for URL: {}", requestUrl);

        } catch (Exception e) {
            // Non-critical logging - silently handle all errors
            // The HikariCP DEALLOCATE ALL will clean up stale statements
            log.debug("Non-critical: Failed to save activity log for URL: {} - {}",
                    requestUrl, e.getMessage());
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
