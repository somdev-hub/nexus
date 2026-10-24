package com.nexus.iam.controller;

import com.nexus.iam.dto.OrganizationFetchDto;
import com.nexus.iam.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Proxies notification analytics requests to the CMS backend.
 * <p>
 * The admin frontend talks only to IAM; IAM forwards to CMS which
 * owns the notification data (cms schema). This keeps the CMS URL
 * and its security boundary behind IAM.
 * <p>
 * Error handling mirrors the nexus-suite pattern: every failure
 * produces a structured JSON error with a meaningful message and
 * the correct HTTP status code.
 */
@RestController
@RequestMapping("/api/notifications/analytics")
@RequiredArgsConstructor
@Slf4j
public class NotificationAnalyticsProxyController {

    private final RestTemplate restTemplate;
    private final OrganizationService organizationService;

    @Value("${cms.service.url}")
    private String cmsServiceUrl;

    @GetMapping("/overview")
    public ResponseEntity<Object> overview() {
        return proxy("/api/notifications/analytics/overview");
    }

    @GetMapping("/trends")
    public ResponseEntity<Object> trends(@RequestParam(defaultValue = "30") int days) {
        return proxy("/api/notifications/analytics/trends?days=" + days);
    }

    @GetMapping("/by-org")
    public ResponseEntity<Object> byOrg(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String url = cmsServiceUrl + "/api/notifications/analytics/by-org?page=" + page + "&size=" + size;
            Object body = restTemplate.getForObject(url, Object.class);
            if (body instanceof Map<?, ?> map) {
                // Enrich org names in the paginated content
                Object contentObj = map.get("content");
                if (contentObj instanceof List<?> list) {
                    List<Map<String, Object>> enriched = (List<Map<String, Object>>) (List<?>) list.stream().map(item -> {
                        if (item instanceof Map<?, ?> m) {
                            Map<String, Object> entry = new java.util.LinkedHashMap<>((Map<String, Object>) m);
                            Object orgIdObj = entry.get("orgId");
                            if (orgIdObj != null) {
                                try {
                                    Long orgId = orgIdObj instanceof Number
                                            ? ((Number) orgIdObj).longValue()
                                            : Long.valueOf(orgIdObj.toString());
                                    OrganizationFetchDto org = organizationService.getOrganizationById(orgId);
                                    if (org != null && org.getOrgName() != null) {
                                        entry.put("orgName", org.getOrgName());
                                    }
                                } catch (Exception e) {
                                    log.debug("Could not resolve org name for orgId={}: {}", orgIdObj, e.getMessage());
                                }
                            }
                            return entry;
                        }
                        return item;
                    }).collect(Collectors.toList());
                    Map<String, Object> result = new java.util.LinkedHashMap<>((Map<String, Object>) map);
                    result.put("content", enriched);
                    return ResponseEntity.ok(result);
                }
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to proxy by-org to CMS: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }

    @GetMapping("/by-event-type")
    public ResponseEntity<Object> byEventType() {
        return proxy("/api/notifications/analytics/by-event-type");
    }

    @GetMapping("/by-severity")
    public ResponseEntity<Object> bySeverity() {
        return proxy("/api/notifications/analytics/by-severity");
    }

    @GetMapping("/by-addressing")
    public ResponseEntity<Object> byAddressing() {
        return proxy("/api/notifications/analytics/by-addressing");
    }

    @GetMapping("/delivery")
    public ResponseEntity<Object> delivery() {
        return proxy("/api/notifications/analytics/delivery");
    }

    @GetMapping("/outbox/summary")
    public ResponseEntity<Object> outboxSummary() {
        return proxy("/api/notifications/analytics/outbox/summary");
    }

    @GetMapping("/outbox/by-event-type")
    public ResponseEntity<Object> outboxByEventType() {
        return proxy("/api/notifications/analytics/outbox/by-event-type");
    }

    @GetMapping("/outbox/trend")
    public ResponseEntity<Object> outboxTrend(@RequestParam(defaultValue = "30") int days) {
        return proxy("/api/notifications/analytics/outbox/trend?days=" + days);
    }

    @GetMapping("/outbox/retried")
    public ResponseEntity<Object> outboxRetried(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proxy("/api/notifications/analytics/outbox/retried?page=" + page + "&size=" + size);
    }

    @GetMapping("/outbox/failed")
    public ResponseEntity<Object> outboxFailed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proxy("/api/notifications/analytics/outbox/failed?page=" + page + "&size=" + size);
    }

    @GetMapping("/timeline")
    public ResponseEntity<Object> timeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String url = cmsServiceUrl + "/api/notifications/analytics/timeline?page=" + page + "&size=" + size;
            Object body = restTemplate.getForObject(url, Object.class);
            if (body instanceof Map<?, ?> map) {
                Object contentObj = map.get("content");
                if (contentObj instanceof List<?> list) {
                    List<Map<String, Object>> enriched = (List<Map<String, Object>>) (List<?>) list.stream().map(item -> {
                        if (item instanceof Map<?, ?> m) {
                            Map<String, Object> entry = new java.util.LinkedHashMap<>((Map<String, Object>) m);
                            Object orgIdObj = entry.get("targetOrgId");
                            if (orgIdObj != null) {
                                try {
                                    Long orgId = orgIdObj instanceof Number
                                            ? ((Number) orgIdObj).longValue()
                                            : Long.valueOf(orgIdObj.toString());
                                    OrganizationFetchDto org = organizationService.getOrganizationById(orgId);
                                    if (org != null && org.getOrgName() != null) {
                                        entry.put("targetOrgName", org.getOrgName());
                                    }
                                } catch (Exception e) {
                                    log.debug("Could not resolve org name for orgId={}: {}", orgIdObj, e.getMessage());
                                }
                            }
                            return entry;
                        }
                        return item;
                    }).collect(Collectors.toList());
                    Map<String, Object> result = new java.util.LinkedHashMap<>((Map<String, Object>) map);
                    result.put("content", enriched);
                    return ResponseEntity.ok(result);
                }
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to proxy timeline to CMS: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }

    @GetMapping("/timeline-legacy")
    public ResponseEntity<Object> timelineLegacy(@RequestParam(defaultValue = "50") int limit) {
        try {
            String url = cmsServiceUrl + "/api/notifications/analytics/timeline?limit=" + limit;
            Object body = restTemplate.getForObject(url, Object.class);
            if (body instanceof List<?> list) {
                List<Map<String, Object>> enriched = (List<Map<String, Object>>) (List<?>) list.stream().map(item -> {
                    if (item instanceof Map<?, ?> m) {
                        Map<String, Object> entry = new java.util.LinkedHashMap<>((Map<String, Object>) m);
                        Object orgIdObj = entry.get("targetOrgId");
                        if (orgIdObj != null) {
                            try {
                                Long orgId = orgIdObj instanceof Number
                                        ? ((Number) orgIdObj).longValue()
                                        : Long.valueOf(orgIdObj.toString());
                                OrganizationFetchDto org = organizationService.getOrganizationById(orgId);
                                if (org != null && org.getOrgName() != null) {
                                    entry.put("targetOrgName", org.getOrgName());
                                }
                            } catch (Exception e) {
                                log.debug("Could not resolve org name for orgId={}: {}", orgIdObj, e.getMessage());
                            }
                        }
                        return entry;
                    }
                    return item;
                }).collect(Collectors.toList());
                return ResponseEntity.ok(enriched);
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to proxy timeline to CMS: {}", e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", "CMS unavailable", "detail", e.getMessage()));
        }
    }

    private ResponseEntity<Object> proxy(String path) {
        String url = cmsServiceUrl + path;
        try {
            Object body = restTemplate.getForObject(url, Object.class);
            if (body == null) {
                log.warn("CMS returned null body for {}", path);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Map.of(
                        "error", "No data returned from CMS",
                        "path", path
                ));
            }
            return ResponseEntity.ok(body);
        } catch (ResourceAccessException e) {
            // Connection refused, timeout, DNS failure, etc.
            Throwable cause = e.getCause();
            String detail;
            if (cause instanceof ConnectException) {
                detail = "CMS backend is not running or not reachable at " + cmsServiceUrl;
            } else if (cause instanceof SocketTimeoutException) {
                detail = "CMS backend timed out responding to " + path;
            } else {
                detail = "Cannot reach CMS backend: " + (cause != null ? cause.getMessage() : e.getMessage());
            }
            log.error("CMS connection failure for {}: {}", path, detail);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "CMS unavailable",
                    "detail", detail,
                    "path", path
            ));
        } catch (HttpClientErrorException e) {
            // 4xx from CMS
            log.warn("CMS returned {} for {}: {}", e.getStatusCode(), path, e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", "CMS request failed",
                    "status", e.getStatusCode().value(),
                    "detail", e.getResponseBodyAsString(),
                    "path", path
            ));
        } catch (HttpServerErrorException e) {
            // 5xx from CMS
            log.error("CMS returned {} for {}: {}", e.getStatusCode(), path, e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", "CMS server error",
                    "status", e.getStatusCode().value(),
                    "detail", e.getResponseBodyAsString(),
                    "path", path
            ));
        } catch (Exception e) {
            log.error("Unexpected error proxying {} to CMS", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Unexpected error",
                    "detail", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    "path", path
            ));
        }
    }
}