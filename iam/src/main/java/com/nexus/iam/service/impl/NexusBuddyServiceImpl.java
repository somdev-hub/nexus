package com.nexus.iam.service.impl;

import com.nexus.iam.service.NexusBuddyService;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
        String url = webConstants.getNexusBuddyClientConfigUrl();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<String> getClientConfigById(Long clientConfigId) {
        String url = webConstants.getNexusBuddyClientConfigUrl() + "/" + clientConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getAllClientConfigs() {
        String url = webConstants.getNexusBuddyClientConfigUrl();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getActiveClientConfigs() {
        String url = webConstants.getNexusBuddyClientConfigUrl() + "/active";
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> updateClientConfig(Long clientConfigId, String payload) {
        String url = webConstants.getNexusBuddyClientConfigUrl() + "/" + clientConfigId;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<String> deactivateClientConfig(Long clientConfigId) {
        String url = webConstants.getNexusBuddyClientConfigUrl() + "/" + clientConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.DELETE, null);
    }

    // ============================================
    // Tools Config APIs
    // ============================================
    @Override
    public ResponseEntity<String> createToolsConfig(String payload) {
        String url = webConstants.getNexusBuddyToolsConfigUrl();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<String> getToolsConfigById(Long toolsConfigId) {
        String url = webConstants.getNexusBuddyToolsConfigUrl() + "/" + toolsConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getAllToolsConfigs() {
        String url = webConstants.getNexusBuddyToolsConfigUrl();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getActiveToolsConfigs() {
        String url = webConstants.getNexusBuddyToolsConfigUrl() + "/active";
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getToolsConfigsByClientConfigId(Long clientConfigId) {
        String url = webConstants.getNexusBuddyToolsConfigUrl() + "/client/" + clientConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> updateToolsConfig(Long toolsConfigId, String payload) {
        String url = webConstants.getNexusBuddyToolsConfigUrl() + "/" + toolsConfigId;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<String> deactivateToolsConfig(Long toolsConfigId) {
        String url = webConstants.getNexusBuddyToolsConfigUrl() + "/" + toolsConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.DELETE, null);
    }

    // ============================================
    // Tools Param Config APIs
    // ============================================
    @Override
    public ResponseEntity<String> createToolsParamConfig(String payload) {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<String> getToolsParamConfigById(Long toolsParamConfigId) {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl() + "/" + toolsParamConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getAllToolsParamConfigs() {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl();
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getActiveToolsParamConfigs() {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl() + "/active";
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> getToolsParamConfigsByToolsConfigId(Long toolsConfigId) {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl() + "/tool/" + toolsConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<String> updateToolsParamConfig(Long toolsParamConfigId, String payload) {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl() + "/" + toolsParamConfigId;
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return restService.iamRestCall(url, payload, headers, HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<String> deactivateToolsParamConfig(Long toolsParamConfigId) {
        String url = webConstants.getNexusBuddyToolsParamConfigUrl() + "/" + toolsParamConfigId;
        return restService.iamRestCall(url, null, null, HttpMethod.DELETE, null);
    }
}
