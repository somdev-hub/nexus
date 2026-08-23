package com.nexus.iam.utils;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nexus.iam.entities.Logs;
import com.nexus.iam.repository.LogsRepo;

@Service
public class RestService {
    private final LogsRepo logsRepo;

    private final CommonUtils commonUtils;

    private final RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestService(LogsRepo logsRepo, CommonUtils commonUtils, RestClient restClient) {
        this.logsRepo = logsRepo;
        this.commonUtils = commonUtils;
        this.restClient = restClient;
    }

    /**
     * Parse JSON response body into a Map.
     * Returns empty map if body is null, empty, or not valid JSON.
     */
    public Map<String, Object> parseJsonResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // If not valid JSON, return empty map
            return Map.of();
        }
    }

    /**
     * Parse JSON response body into a specific type.
     * Returns null if body is null, empty, or not valid JSON for the target type.
     */
    public <T> T parseJsonResponse(String responseBody, Class<T> targetType) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, targetType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse JSON response body into a generic type using TypeReference.
     * Returns null if body is null, empty, or not valid JSON for the target type.
     */
    public <T> T parseJsonResponse(String responseBody, TypeReference<T> typeReference) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, typeReference);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public ResponseEntity<String> iamRestCall(String url, Object payload, Map<String, String> headers,
            HttpMethod method, Long userId) {
        ResponseEntity<String> responseEntity = null;
        String requestLog = null;
        try {
            boolean isMultipartByHeader = headers != null
                    && headers.entrySet().stream()
                            .anyMatch(e -> "Content-Type".equalsIgnoreCase(e.getKey())
                                    && e.getValue() != null
                                    && e.getValue().toLowerCase().contains(MediaType.MULTIPART_FORM_DATA_VALUE));

            // Check if payload contains multipart files - handle any Map implementation
            boolean hasMultipartFile = false;
            if (payload instanceof Map) {
                @SuppressWarnings("rawtypes")
                Map payloadMap = (Map) payload;
                System.out.println("DEBUG RestService: payloadMap size=" + payloadMap.size() + ", keys=" + payloadMap.keySet());
                for (Object value : payloadMap.values()) {
                    System.out.println("DEBUG RestService: iterating value=" + value + ", class=" + (value != null ? value.getClass().getName() : "null") + ", isMultipartFile=" + (value instanceof MultipartFile));
                    if (value instanceof MultipartFile) {
                        hasMultipartFile = true;
                        break;
                    }
                }
            }

            System.out.println("DEBUG RestService: isMultipartByHeader=" + isMultipartByHeader + ", hasMultipartFile=" + hasMultipartFile + ", payloadClass=" + (payload != null ? payload.getClass().getName() : "null"));

            // Check if payload contains multipart files
            if (hasMultipartFile || isMultipartByHeader) {
                System.out.println("DEBUG RestService: Using handleMultipartRequest");
                responseEntity = handleMultipartRequest(url, (Map<String, Object>) payload, headers, method);
                requestLog = serializePayload(payload);
            } else {
                System.out.println("DEBUG RestService: Using handleRegularRequest");
                responseEntity = handleRegularRequest(url, payload, headers, method);
                requestLog = payload != null ? serializePayload(payload) : null;
            }
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>("Exception occurred during REST call: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
            requestLog = payload != null ? serializePayload(payload) : null;
        } finally {
            Logs log = new Logs();
            log.setRequestUrl(url);
            log.setHttpMethod(method.name());
            log.setRequest(requestLog);
            if (responseEntity != null) {
                String responseString = responseEntity.getBody();
                if (responseString != null) {
                    log.setResponse(commonUtils.jsonValidator(responseString));
                }
                log.setResponseStatus(responseEntity.getStatusCode().value());
            }
            log.setUserId(userId != null ? userId : 0L);

            logsRepo.save(log);
        }

        return responseEntity;
    }

    private boolean containsMultipartFile(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (value instanceof MultipartFile) {
                return true;
            }
        }
        return false;
    }

    private String serializePayload(Object payload) {
        try {
            if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) payload;
                // Create a copy to avoid serializing MultipartFile objects
                Map<String, Object> safeMap = new java.util.HashMap<>();
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

    private ResponseEntity<String> handleMultipartRequest(String url, Map<String, Object> payload,
            Map<String, String> headers, HttpMethod method) throws IOException {
        System.out.println("DEBUG handleMultipartRequest: payload keys=" + payload.keySet());
        // Create multipart body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            System.out.println("DEBUG handleMultipartRequest: key=" + entry.getKey() + ", valueClass=" + (value != null ? value.getClass().getName() : "null"));

            if (value instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) value;
                ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                HttpHeaders fileHeaders = new HttpHeaders();
                if (file.getContentType() != null) {
                    fileHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
                }
                HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileResource, fileHeaders);
                body.add(entry.getKey(), filePart);
            } else {
                String jsonPart;
                ObjectMapper mapper = new ObjectMapper();
                if (value instanceof String) {
                    String s = (String) value;
                    String current = s;
                    String normalized = null;
                    for (int i = 0; i < 5; i++) {
                        try {
                            JsonNode node = mapper.readTree(current);
                            if (node.isTextual()) {
                                current = node.textValue();
                                continue;
                            } else {
                                normalized = mapper.writeValueAsString(node);
                                break;
                            }
                        } catch (Exception ex) {
                            break;
                        }
                    }
                    jsonPart = normalized != null ? normalized : current;
                } else {
                    jsonPart = mapper.writeValueAsString(value);
                }
                HttpHeaders partHeaders = new HttpHeaders();
                partHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> partEntity = new HttpEntity<>(jsonPart, partHeaders);
                body.add(entry.getKey(), partEntity);
            }
        }

        // Build request headers — skip Content-Type (RestClient sets multipart boundary automatically)
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!key.equalsIgnoreCase("Content-Type")) {
                    httpHeaders.set(key, value);
                }
            });
        }

        // Always read response as String to handle any content type
        // (JSON, plain text, XML, etc.) without converter errors
        return restClient.method(method)
                .uri(url)
                .headers(h -> h.addAll(httpHeaders))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<String> handleRegularRequest(String url, Object payload, Map<String, String> headers,
            HttpMethod method) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }

        // Always read response as String to handle any content type
        // (JSON, plain text, XML, etc.) without converter errors
        var requestSpec = restClient.method(method)
                .uri(url)
                .headers(h -> h.addAll(httpHeaders));

        // Only add body for non-GET/HEAD requests with non-null payload
        if (payload != null && method != HttpMethod.GET && method != HttpMethod.HEAD) {
            requestSpec.body(payload);
        }

        return requestSpec.retrieve().toEntity(String.class);
    }

}
