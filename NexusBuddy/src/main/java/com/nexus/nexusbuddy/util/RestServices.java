package com.nexus.nexusbuddy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.util.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Modern WebClient-based REST client for NexusBuddy.
 * Single method nexusBuddyCall similar to HR's hrRestCall.
 * Supports multipart, regular requests, and async logging via Logger service.
 */
public class RestServices {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RestServices.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final Duration defaultTimeout;
    private final Map<String, String> defaultHeaders;
    private final Logger logger;

    /**
     * Creates a new RestServices with default configuration.
     */
    public RestServices(String baseUrl, Logger logger) {
        this(baseUrl, Duration.ofSeconds(30), Collections.emptyMap(), logger);
    }

    /**
     * Creates a new RestServices with custom timeout.
     */
    public RestServices(String baseUrl, Duration timeout, Logger logger) {
        this(baseUrl, timeout, Collections.emptyMap(), logger);
    }

    /**
     * Creates a new RestServices with custom timeout and default headers.
     */
    public RestServices(String baseUrl, Duration timeout, Map<String, String> defaultHeaders, Logger logger) {
        this.defaultTimeout = timeout;
        this.defaultHeaders = defaultHeaders != null ? new HashMap<>(defaultHeaders) : new HashMap<>();
        this.logger = logger;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    this.defaultHeaders.forEach(headers::set);
                })
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    /**
     * Main entry point for all REST calls - similar to HR's hrRestCall.
     * Handles both multipart and regular requests.
     * Logs asynchronously via Logger service to prevent blocking.
     *
     * @param url          The full URL to call
     * @param payload      Request payload (Map for multipart, or any object for
     *                     JSON)
     * @param headers      HTTP headers
     * @param method       HTTP method
     * @param clientConfig ClientConfig entity for logging
     * @param toolName     Tool name for logging
     * @return ResponseEntity with response body
     */
    public ResponseEntity<?> nexusBuddyCall(String url, Object payload, Map<String, String> headers,
            HttpMethod method, ClientConfig clientConfig, String toolName) {
        ResponseEntity<?> responseEntity = null;
        String requestLog = null;
        long startTime = System.currentTimeMillis();

        try {
            // Check if payload contains multipart files
            if (payload instanceof Map && containsMultipartFile((Map<String, Object>) payload)) {
                responseEntity = handleMultipartRequest(url, (Map<String, Object>) payload, headers, method);
                requestLog = serializePayload(payload);
            } else {
                responseEntity = handleRegularRequest(url, payload, headers, method);
                requestLog = payload != null ? serializePayload(payload) : null;
            }
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>("Exception occurred during REST call: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
            requestLog = payload != null ? serializePayload(payload) : null;
        } finally {
            // Log asynchronously via Logger service to prevent blocking the main operation
            logRestCallAsync(url, method, requestLog, responseEntity, clientConfig, toolName, startTime);
        }

        return responseEntity;
    }

    /**
     * Logs REST calls asynchronously via Logger service.
     */
    private void logRestCallAsync(String url, HttpMethod method, String requestLog,
            ResponseEntity<?> responseEntity, ClientConfig clientConfig, String toolName, long startTime) {
        try {
            long executionTime = System.currentTimeMillis() - startTime;

            String responseString = null;
            Integer responseStatus = null;

            if (responseEntity != null) {
                Object respBody = responseEntity.getBody();
                responseString = respBody != null ? respBody.toString() : null;
                responseStatus = responseEntity.getStatusCode().value();
            }

            // Log to SLF4J
            LOGGER.info(
                    "NexusBuddy REST Call - Client: {}, Tool: {}, Method: {}, URL: {}, Status: {}, Time: {}ms, Request: {}, Response: {}",
                    clientConfig != null ? clientConfig.getClientConfigId() : "unknown", toolName, method.name(), url,
                    responseStatus, executionTime, requestLog, responseString);

            // Save to database via Logger service (runs in separate transaction)
            if (logger != null && responseStatus != null) {
                logger.saveLogs(url, method, HttpStatus.valueOf(responseStatus), requestLog, responseString,
                        clientConfig, toolName);
            }

        } catch (Exception e) {
            // Log error but don't propagate - logging failure shouldn't crash the main
            // operation
            LOGGER.error("Failed to log REST call to {}: {}", url, e.getMessage(), e);
        }
    }

    private boolean containsMultipartFile(Map<String, Object> map) {
        if (map == null)
            return false;
        return map.values().stream().anyMatch(v -> v instanceof MultipartFile);
    }

    private String serializePayload(Object payload) {
        try {
            if (payload instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) payload;
                // Create a copy to avoid serializing MultipartFile objects
                Map<String, Object> safeMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (entry.getValue() instanceof MultipartFile) {
                        safeMap.put(entry.getKey(),
                                "MultipartFile:" + ((MultipartFile) entry.getValue()).getOriginalFilename());
                    } else {
                        safeMap.put(entry.getKey(), entry.getValue());
                    }
                }
                return OBJECT_MAPPER.writeValueAsString(safeMap);
            } else {
                return OBJECT_MAPPER.writeValueAsString(payload);
            }
        } catch (Exception e) {
            return payload != null ? payload.toString() : "null";
        }
    }

    private ResponseEntity<?> handleMultipartRequest(String url, Map<String, Object> payload,
            Map<String, String> headers, HttpMethod method) throws IOException {
        // Build multipart body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof MultipartFile file) {
                // Convert MultipartFile to ByteArrayResource
                org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(
                        file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add(entry.getKey(), fileResource);
            } else {
                // Send other objects directly
                body.add(entry.getKey(), value);
            }
        }

        // Create headers - DO NOT set Content-Type, WebClient will handle it
        // automatically for multipart
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!key.equalsIgnoreCase("Content-Type")) {
                    httpHeaders.set(key, value);
                }
            });
        }

        // Add default headers
        defaultHeaders.forEach(httpHeaders::set);

        return webClient.method(method)
                .uri(url)
                .headers(h -> h.addAll(httpHeaders))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("Unknown error")
                        .flatMap(errorBody -> Mono
                                .error(new RuntimeException("HTTP " + clientResponse.statusCode() + ": " + errorBody))))
                .toEntity(Object.class)
                .block(defaultTimeout);
    }

    private ResponseEntity<?> handleRegularRequest(String url, Object payload, Map<String, String> headers,
            HttpMethod method) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }
        defaultHeaders.forEach(httpHeaders::set);

        return webClient.method(method)
                .uri(url)
                .headers(h -> h.addAll(httpHeaders))
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("Unknown error")
                        .flatMap(errorBody -> Mono
                                .error(new RuntimeException("HTTP " + clientResponse.statusCode() + ": " + errorBody))))
                .toEntity(Object.class)
                .block(defaultTimeout);
    }
}