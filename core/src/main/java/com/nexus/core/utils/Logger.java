package com.nexus.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.model.entities.Logs;
import com.nexus.core.exception.ServiceLevelException;
import com.nexus.core.repository.LogsRepo;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Service
public class Logger {

	private final LogsRepo logsRepo;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public Logger(LogsRepo logsRepo) {
		this.logsRepo = logsRepo;
	}

    /**
     * Save logs to database
     * <p>
     * Runs in its own transaction: activity logging also fires inside
     * {@code @Transactional(readOnly = true)} request handlers (e.g. ASN
     * lookups), where an INSERT would otherwise mark the surrounding
     * read-only transaction rollback-only and fail the request with
     * UnexpectedRollbackException.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogs(String requestUrl, HttpMethod httpMethod, HttpStatus httpStatus, Object request,
                         Object response, Long userId) {
		try {
			Logs log = new Logs();
			log.setRequestUrl(requestUrl);
			log.setHttpMethod(httpMethod.name());
			log.setRequest(serializeObject(request));
			log.setResponse(serializeObject(response));
			log.setResponseStatus(httpStatus.value());
			log.setUserId(userId != null ? userId : 0L);
			log.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			logsRepo.save(log);
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
		} catch (JsonProcessingException ex) {
			// Fallback to toString if JSON serialization fails
			return obj.toString();
		}
	}
}
