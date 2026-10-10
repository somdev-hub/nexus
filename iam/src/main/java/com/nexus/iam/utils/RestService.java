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
import org.springframework.web.client.RestTemplate;
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

    // Used for the terminal response read: its StringHttpMessageConverter
    // supports ALL media types, so JSON, text and binary (octet-stream)
    // bodies are all readable as text — something RestClient's String
    // decoding cannot do.
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RestService(LogsRepo logsRepo, CommonUtils commonUtils, RestTemplate restTemplate) {
        this.logsRepo = logsRepo;
        this.commonUtils = commonUtils;
        this.restTemplate = restTemplate;
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

            // Check if payload contains multipart files - handle any Map implementation.
            // NOTE: MultiValueMap bodies store values as lists, so look inside
            // lists too; and buildMultipartHeaders() deliberately omits
            // Content-Type (boundary is auto-set), so the header alone is not
            // a reliable signal.
            boolean hasMultipartFile = false;
            if (payload instanceof Map) {
                @SuppressWarnings("rawtypes")
                Map payloadMap = (Map) payload;
                System.out.println("DEBUG RestService: payloadMap size=" + payloadMap.size() + ", keys=" + payloadMap.keySet());
                for (Object value : payloadMap.values()) {
                    System.out.println("DEBUG RestService: iterating value=" + value + ", class=" + (value != null ? value.getClass().getName() : "null") + ", isBinary=" + isBinaryPayload(value));
                    if (isBinaryPayload(value)) {
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
            // Request logging must never break the proxied call (e.g. file
            // uploads): persist best-effort and swallow logging failures.
            try {
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
            } catch (Exception logException) {
                System.err.println("Failed to save REST log for " + url + ": " + logException.getMessage());
            }
        }

        return responseEntity;
    }

    private boolean containsMultipartFile(Map<String, Object> map) {
        for (Object value : map.values()) {
            if (isBinaryPayload(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True for file-ish payload content: MultipartFile, Spring Resource,
     * raw bytes/streams, or lists holding any of those (MultiValueMap
     * bodies wrap every value in a list).
     */
    private boolean isBinaryPayload(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof MultipartFile
                || value instanceof org.springframework.core.io.Resource
                || value instanceof byte[]
                || value instanceof java.io.InputStream) {
            return true;
        }
        if (value instanceof java.util.List) {
            for (Object item : (java.util.List<?>) value) {
                if (isBinaryPayload(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String serializePayload(Object payload) {
        try {
            if (payload instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) payload;
                // Create a copy to avoid serializing binary content
                // (MultipartFile, resources, or MultiValueMap lists holding them).
                Map<String, Object> safeMap = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    safeMap.put(entry.getKey(), sanitizeForLog(entry.getValue()));
                }
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(safeMap);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(sanitizeForLog(payload));
            }
        } catch (Exception e) {
            // The request column is jsonb: never fall back to toString(),
            // which is not valid JSON (e.g. "{file=[...]}" for uploads).
            return "\"[unserializable payload omitted]\"";
        }
    }

    /**
     * Replace binary content with a placeholder so request logging stays
     * valid JSON. Handles direct values as well as MultiValueMap-style
     * lists holding files/resources.
     */
    private Object sanitizeForLog(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof MultipartFile
                || value instanceof org.springframework.core.io.Resource
                || value instanceof byte[]
                || value instanceof java.io.InputStream) {
            return "[binary content omitted]";
        }
        if (value instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) value;
            java.util.List<Object> safeList = new java.util.ArrayList<>(list.size());
            for (Object item : list) {
                safeList.add(sanitizeForLog(item));
            }
            return safeList;
        }
        return value;
    }

    private ResponseEntity<String> handleMultipartRequest(String url, Map<String, Object> payload,
            Map<String, String> headers, HttpMethod method) throws IOException {
        System.out.println("DEBUG handleMultipartRequest: payload keys=" + payload.keySet());
        // Create multipart body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();
            System.out.println("DEBUG handleMultipartRequest: key=" + entry.getKey() + ", valueClass=" + (value != null ? value.getClass().getName() : "null"));
            // MultiValueMap-style bodies store values as lists: flatten them so
            // each file/resource becomes its own part.
            if (value instanceof java.util.List) {
                for (Object item : (java.util.List<?>) value) {
                    addMultipartPart(body, entry.getKey(), item, mapper);
                }
            } else {
                addMultipartPart(body, entry.getKey(), value, mapper);
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
        // (JSON, plain text, XML, binary, etc.) without converter errors.
        // RestTemplate is used for the terminal read because its String
        // converter supports ALL media types, unlike RestClient's.
        return restTemplate.exchange(url, method, new HttpEntity<>(body, httpHeaders), String.class);
    }

    /**
     * Add one part to a multipart body: files/resources become file parts
     * (never JSON-serialized), everything else becomes a JSON part.
     */
    private void addMultipartPart(MultiValueMap<String, Object> body, String key, Object value,
            ObjectMapper mapper) throws IOException {
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
            body.add(key, filePart);
            return;
        }
        if (value instanceof org.springframework.core.io.Resource) {
            org.springframework.core.io.Resource resource = (org.springframework.core.io.Resource) value;
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            HttpEntity<org.springframework.core.io.Resource> filePart = new HttpEntity<>(resource, fileHeaders);
            body.add(key, filePart);
            return;
        }
        String jsonPart;
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
        body.add(key, partEntity);
    }

    private ResponseEntity<String> handleRegularRequest(String url, Object payload, Map<String, String> headers,
            HttpMethod method) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }

        // Only add body for non-GET/HEAD requests with non-null payload.
        // Terminal read goes through RestTemplate (see above) so every
        // content type is readable as text.
        HttpEntity<?> entity = (payload != null && method != HttpMethod.GET && method != HttpMethod.HEAD)
                ? new HttpEntity<>(payload, httpHeaders)
                : new HttpEntity<>(httpHeaders);
        return restTemplate.exchange(url, method, entity, String.class);
    }

}
