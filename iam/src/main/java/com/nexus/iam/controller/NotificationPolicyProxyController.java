package com.nexus.iam.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxies notification policy CRUD to CMS.
 */
@RestController
@RequestMapping("/api/notifications/policies")
@RequiredArgsConstructor
@Slf4j
public class NotificationPolicyProxyController {

    private final RestTemplate restTemplate;

    @Value("${cms.service.url}")
    private String cmsServiceUrl;

    @GetMapping
    public ResponseEntity<Object> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String eventType) {
        String url = cmsServiceUrl + "/api/notifications/policies?page=" + page + "&size=" + size;
        if (eventType != null && !eventType.isBlank()) url += "&eventType=" + eventType;
        return proxyGet(url);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> get(@PathVariable Long id) {
        return proxyGet(cmsServiceUrl + "/api/notifications/policies/" + id);
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody Map<String, Object> body) {
        try {
            Object result = restTemplate.postForObject(cmsServiceUrl + "/api/notifications/policies", body, Object.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create notification policy: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            // RestTemplate doesn't have putForObject, use exchange
            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(body);
            org.springframework.http.ResponseEntity<Object> resp = restTemplate.exchange(
                    cmsServiceUrl + "/api/notifications/policies/" + id,
                    org.springframework.http.HttpMethod.PUT, entity, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            log.error("Failed to update notification policy: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deactivate(@PathVariable Long id) {
        try {
            org.springframework.http.ResponseEntity<Object> resp = restTemplate.exchange(
                    cmsServiceUrl + "/api/notifications/policies/" + id,
                    org.springframework.http.HttpMethod.DELETE, null, Object.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (Exception e) {
            log.error("Failed to deactivate notification policy: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }

    @GetMapping("/event-types")
    public ResponseEntity<Object> eventTypes() {
        return proxyGet(cmsServiceUrl + "/api/notifications/policies/event-types");
    }

    private ResponseEntity<Object> proxyGet(String url) {
        try {
            Object body = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to proxy to CMS: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }
}