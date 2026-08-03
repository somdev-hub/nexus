package com.nexus.iam.service.impl;

import com.nexus.iam.service.NexusBuddyService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NexusBuddyServiceImpl implements NexusBuddyService {
    private final RestService restService;
    private final WebConstants webConstants;

    // ============================================
    // Client Config APIs
    // ============================================
    @Override
    public ResponseEntity<String> createClientConfig(String payload) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyClientConfigUrl())
                .toUriString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<String> getClientConfigById(Long clientConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyClientConfigUrl())
                .pathSegment(clientConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getAllClientConfigs() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyClientConfigUrl())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getActiveClientConfigs() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyClientConfigUrl())
                .pathSegment("active")
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> updateClientConfig(Long clientConfigId, String payload) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyClientConfigUrl())
                .pathSegment(clientConfigId.toString())
                .toUriString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<String> deactivateClientConfig(Long clientConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyClientConfigUrl())
                .pathSegment(clientConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.DELETE, null);
    }

    // ============================================
    // Tools Config APIs
    // ============================================
    @Override
    public ResponseEntity<String> createToolsConfig(String payload) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .toUriString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<String> getToolsConfigById(Long toolsConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .pathSegment(toolsConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getAllToolsConfigs() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getActiveToolsConfigs() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .pathSegment("active")
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getToolsConfigsByClientConfigId(Long clientConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .pathSegment("client", clientConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> updateToolsConfig(Long toolsConfigId, String payload) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .pathSegment(toolsConfigId.toString())
                .toUriString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<String> deactivateToolsConfig(Long toolsConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsConfigUrl())
                .pathSegment(toolsConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.DELETE, null);
    }

    // ============================================
    // Tools Param Config APIs
    // ============================================
    @Override
    public ResponseEntity<String> createToolsParamConfig(String payload) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .toUriString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<String> getToolsParamConfigById(Long toolsParamConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .pathSegment(toolsParamConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getAllToolsParamConfigs() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getActiveToolsParamConfigs() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .pathSegment("active")
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getToolsParamConfigsByToolsConfigId(Long toolsConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .pathSegment("tool", toolsConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> updateToolsParamConfig(Long toolsParamConfigId, String payload) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .pathSegment(toolsParamConfigId.toString())
                .toUriString();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<String> deactivateToolsParamConfig(Long toolsParamConfigId) {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyToolsParamConfigUrl())
                .pathSegment(toolsParamConfigId.toString())
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.DELETE, null);
    }

    // ============================================
    // Dashboard Analytics APIs
    // ============================================
    @Override
    public ResponseEntity<String> getDashboardSummary(String range, String start, String end, List<Long> clientIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyDashboardUrl())
                .pathSegment("summary")
                .queryParam("range", range);
        if (start != null)
            builder.queryParam("start", start);
        if (end != null)
            builder.queryParam("end", end);
        if (clientIds != null && !clientIds.isEmpty()) {
            for (Long id : clientIds) {
                builder.queryParam("clientIds", id);
            }
        }
        return restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getClientHealth(String range, String start, String end, List<Long> clientIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyDashboardUrl())
                .pathSegment("client-health")
                .queryParam("range", range);
        if (start != null)
            builder.queryParam("start", start);
        if (end != null)
            builder.queryParam("end", end);
        if (clientIds != null && !clientIds.isEmpty()) {
            for (Long id : clientIds) {
                builder.queryParam("clientIds", id);
            }
        }
        return restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getRequestTrends(String range, String start, String end, List<Long> clientIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyDashboardUrl())
                .pathSegment("requests", "trends")
                .queryParam("range", range);
        if (start != null)
            builder.queryParam("start", start);
        if (end != null)
            builder.queryParam("end", end);
        if (clientIds != null && !clientIds.isEmpty()) {
            for (Long id : clientIds) {
                builder.queryParam("clientIds", id);
            }
        }
        return restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getToolUsage(String range, String start, String end, List<Long> clientIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyDashboardUrl())
                .pathSegment("tools", "usage")
                .queryParam("range", range);
        if (start != null)
            builder.queryParam("start", start);
        if (end != null)
            builder.queryParam("end", end);
        if (clientIds != null && !clientIds.isEmpty()) {
            for (Long id : clientIds) {
                builder.queryParam("clientIds", id);
            }
        }
        return restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getPerformance(String range, String start, String end, List<Long> clientIds) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyDashboardUrl())
                .pathSegment("performance")
                .queryParam("range", range);
        if (start != null)
            builder.queryParam("start", start);
        if (end != null)
            builder.queryParam("end", end);
        if (clientIds != null && !clientIds.isEmpty()) {
            for (Long id : clientIds) {
                builder.queryParam("clientIds", id);
            }
        }
        return restService.iamRestCall(builder.toUriString(), null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getConfigInsights() {
        String url = UriComponentsBuilder.fromUriString(webConstants.getNexusBuddyDashboardUrl())
                .pathSegment("config-insights")
                .toUriString();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }
}
