package com.nexus.iam.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.iam.entities.Logs;
import com.nexus.iam.repository.LogsRepo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class RestService {
    private final LogsRepo logsRepo;

    private final CommonUtils commonUtils;

    public RestService(LogsRepo logsRepo, CommonUtils commonUtils) {
        this.logsRepo = logsRepo;
        this.commonUtils = commonUtils;
    }

    public ResponseEntity<?> iamRestCall(String url, Object payload, Map<String, String> headers,
                                         HttpMethod method, Long userId) {
        ResponseEntity<?> responseEntity = null;
        String requestLog = null;
        try {
            // Check if payload contains multipart files
            if (payload instanceof Map && containsMultipartFile((Map<String, Object>) payload)) {
                responseEntity = handleMultipartRequest(url, (Map<String, Object>) payload, headers, method);
                requestLog = serializePayload(payload); // Serialize to JSON even for multipart
            } else {
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
                Object respBody = responseEntity.getBody();
                String responseString = respBody != null ? respBody.toString() : null;
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

    private ResponseEntity<?> handleMultipartRequest(String url, Map<String, Object> payload,
                                                     Map<String, String> headers, HttpMethod method) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        // Create multipart body
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) value;
                // Convert MultipartFile to ByteArrayResource
                ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add(entry.getKey(), fileResource);
            } else {
                // Send DTO and other objects directly - RestTemplate will handle serialization
                body.add(entry.getKey(), value);
            }
        }

        // Create headers - DO NOT set Content-Type, RestTemplate will handle it automatically
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((key, value) -> {
                // Skip Content-Type - RestTemplate sets it automatically for multipart
                if (!key.equalsIgnoreCase("Content-Type")) {
                    httpHeaders.set(key, value);
                }
            });
        }

        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
        return restTemplate.exchange(url, method, httpEntity, Object.class);
    }

    private ResponseEntity<?> handleRegularRequest(String url, Object payload, Map<String, String> headers, HttpMethod method) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }

        HttpEntity<Object> httpEntity = new HttpEntity<>(payload, httpHeaders);
        return restTemplate.exchange(url, method, httpEntity, Object.class);
    }
}
