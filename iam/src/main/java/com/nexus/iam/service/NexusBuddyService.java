package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface NexusBuddyService {

    // ============================================
    // Client Config APIs
    // ============================================
    ResponseEntity<String> createClientConfig(String payload);

    ResponseEntity<String> getClientConfigById(Long clientConfigId);

    ResponseEntity<String> getAllClientConfigs();

    ResponseEntity<String> getActiveClientConfigs();

    ResponseEntity<String> updateClientConfig(Long clientConfigId, String payload);

    ResponseEntity<String> deactivateClientConfig(Long clientConfigId);

    // ============================================
    // Tools Config APIs
    // ============================================
    ResponseEntity<String> createToolsConfig(String payload);

    ResponseEntity<String> getToolsConfigById(Long toolsConfigId);

    ResponseEntity<String> getAllToolsConfigs();

    ResponseEntity<String> getActiveToolsConfigs();

    ResponseEntity<String> getToolsConfigsByClientConfigId(Long clientConfigId);

    ResponseEntity<String> updateToolsConfig(Long toolsConfigId, String payload);

    ResponseEntity<String> deactivateToolsConfig(Long toolsConfigId);

    // ============================================
    // Tools Param Config APIs
    // ============================================
    ResponseEntity<String> createToolsParamConfig(String payload);

    ResponseEntity<String> getToolsParamConfigById(Long toolsParamConfigId);

    ResponseEntity<String> getAllToolsParamConfigs();

    ResponseEntity<String> getActiveToolsParamConfigs();

    ResponseEntity<String> getToolsParamConfigsByToolsConfigId(Long toolsConfigId);

    ResponseEntity<String> updateToolsParamConfig(Long toolsParamConfigId, String payload);

    ResponseEntity<String> deactivateToolsParamConfig(Long toolsParamConfigId);

    // ============================================
    // Dashboard Analytics APIs
    // ============================================
    ResponseEntity<String> getDashboardSummary(String range, String start, String end, List<Long> clientIds);

    ResponseEntity<String> getClientHealth(String range, String start, String end, List<Long> clientIds);

    ResponseEntity<String> getRequestTrends(String range, String start, String end, List<Long> clientIds);

    ResponseEntity<String> getToolUsage(String range, String start, String end, List<Long> clientIds);

    ResponseEntity<String> getPerformance(String range, String start, String end, List<Long> clientIds);

    ResponseEntity<String> getConfigInsights();

    // ============================================
    // Client Insights APIs
    // ============================================
    ResponseEntity<String> getClientInsights(Long clientId, String range, String start, String end);

    ResponseEntity<String> getClientToolInsights(Long clientId, String range, String start, String end, Integer pageNo, Integer pageOffset, String sort);

    ResponseEntity<String> getClientLogs(Long clientId, String toolName, String status, String statusCode,
                                         String httpMethod, String startDate, String endDate, Integer pageNo, Integer pageOffset, String sort);
}
