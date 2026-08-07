package com.nexus.iam.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.NexusBuddyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/nexusbuddy")
@RequiredArgsConstructor
@Slf4j
public class NexusBuddyController {

    private final NexusBuddyService nexusBuddyService;

    // ============================================
    // Chat APIs (Proxy to NexusBuddy service)
    // ============================================
    @PostMapping("/api/chat")
    @LogActivity("NEXUS_BUDDY_CHAT")
    public ResponseEntity<String> chat(@RequestBody String payload) {
        log.info("Proxying chat request to NexusBuddy");
        return nexusBuddyService.chat(payload);
    }

    @PostMapping("/api/chat/conversation")
    @LogActivity("NEXUS_BUDDY_CHAT_CONVERSATION")
    public ResponseEntity<String> chatWithConversation(@RequestBody String payload) {
        log.info("Proxying chat conversation request to NexusBuddy");
        return nexusBuddyService.chatWithConversation(payload);
    }

    @PostMapping("/api/chat/direct")
    @LogActivity("NEXUS_BUDDY_CHAT_DIRECT")
    public ResponseEntity<String> directChat(@RequestBody String payload) {
        log.info("Proxying direct chat request to NexusBuddy");
        return nexusBuddyService.directChat(payload);
    }

    @PostMapping(value = "/api/chat/stream", produces = "text/event-stream")
    @LogActivity("NEXUS_BUDDY_CHAT_STREAM")
    public ResponseEntity<String> streamChat(@RequestBody String payload) {
        log.info("Proxying streaming chat request to NexusBuddy");
        return nexusBuddyService.streamChat(payload);
    }

    @GetMapping("/api/chat/health")
    @LogActivity("NEXUS_BUDDY_CHAT_HEALTH")
    public ResponseEntity<String> health() {
        log.info("Proxying health check to NexusBuddy");
        return nexusBuddyService.health();
    }

    // ============================================
    // Domain-based Chat APIs
    // ============================================
    @PostMapping(value = "/api/chat/stream/by-domain", produces = "text/event-stream")
    @LogActivity("NEXUS_BUDDY_CHAT_STREAM_BY_DOMAIN")
    public ResponseEntity<String> streamChatByDomain(@RequestBody String payload, @RequestParam String domain) {
        log.info("Proxying streaming chat request to NexusBuddy for domain: {}", domain);
        return nexusBuddyService.streamChatByDomain(payload, domain);
    }

    @PostMapping("/api/chat/by-domain")
    @LogActivity("NEXUS_BUDDY_CHAT_BY_DOMAIN")
    public ResponseEntity<String> chatByDomain(@RequestBody String payload, @RequestParam String domain) {
        log.info("Proxying chat request to NexusBuddy for domain: {}", domain);
        return nexusBuddyService.chatByDomain(payload, domain);
    }

    // ============================================
    // Test/Debug Streaming Endpoint
    // ============================================
    @PostMapping(value = "/api/chat/stream/test", produces = "text/event-stream")
    @LogActivity("NEXUS_BUDDY_CHAT_STREAM_TEST")
    public ResponseEntity<String> streamTestLogs(@RequestBody String payload) {
        log.info("Proxying test streaming request to NexusBuddy");
        return nexusBuddyService.streamTestLogs(payload);
    }

    // ============================================
    // Client Config APIs (Admin)
    // ============================================
    @PostMapping("/admin/client-configs")
    @LogActivity("CREATE_CLIENT_CONFIG")
    public ResponseEntity<String> createClientConfig(@RequestBody String payload) {
        log.info("Creating client config");
        return nexusBuddyService.createClientConfig(payload);
    }

    @GetMapping("/admin/client-configs/{clientConfigId}")
    @LogActivity("GET_CLIENT_CONFIG_BY_ID")
    public ResponseEntity<String> getClientConfigById(@PathVariable Long clientConfigId) {
        log.info("Fetching client config: {}", clientConfigId);
        return nexusBuddyService.getClientConfigById(clientConfigId);
    }

    @GetMapping("/admin/client-configs")
    @LogActivity("GET_ALL_CLIENT_CONFIGS")
    public ResponseEntity<String> getAllClientConfigs() {
        log.info("Fetching all client configs");
        return nexusBuddyService.getAllClientConfigs();
    }

    @GetMapping("/admin/client-configs/active")
    @LogActivity("GET_ACTIVE_CLIENT_CONFIGS")
    public ResponseEntity<String> getActiveClientConfigs() {
        log.info("Fetching active client configs");
        return nexusBuddyService.getActiveClientConfigs();
    }

    @PutMapping("/admin/client-configs/{clientConfigId}")
    @LogActivity("UPDATE_CLIENT_CONFIG")
    public ResponseEntity<String> updateClientConfig(@PathVariable Long clientConfigId, @RequestBody String payload) {
        log.info("Updating client config: {}", clientConfigId);
        return nexusBuddyService.updateClientConfig(clientConfigId, payload);
    }

    @DeleteMapping("/admin/client-configs/{clientConfigId}")
    @LogActivity("DEACTIVATE_CLIENT_CONFIG")
    public ResponseEntity<String> deactivateClientConfig(@PathVariable Long clientConfigId) {
        log.info("Deactivating client config: {}", clientConfigId);
        return nexusBuddyService.deactivateClientConfig(clientConfigId);
    }

    @GetMapping("/admin/client-configs/by-domain")
    @LogActivity("GET_CLIENT_CONFIGS_BY_DOMAIN")
    public ResponseEntity<String> getClientConfigsByDomain(@RequestParam String domain) {
        log.info("Fetching client configs by domain: {}", domain);
        return nexusBuddyService.getClientConfigsByDomain(domain);
    }

    // ============================================
    // Tools Config APIs (Admin)
    // ============================================
    @PostMapping("/admin/tools-configs")
    @LogActivity("CREATE_TOOLS_CONFIG")
    public ResponseEntity<String> createToolsConfig(@RequestBody String payload) {
        log.info("Creating tools config");
        return nexusBuddyService.createToolsConfig(payload);
    }

    @GetMapping("/admin/tools-configs/{toolsConfigId}")
    @LogActivity("GET_TOOLS_CONFIG_BY_ID")
    public ResponseEntity<String> getToolsConfigById(@PathVariable Long toolsConfigId) {
        log.info("Fetching tools config: {}", toolsConfigId);
        return nexusBuddyService.getToolsConfigById(toolsConfigId);
    }

    @GetMapping("/admin/tools-configs")
    @LogActivity("GET_ALL_TOOLS_CONFIGS")
    public ResponseEntity<String> getAllToolsConfigs() {
        log.info("Fetching all tools configs");
        return nexusBuddyService.getAllToolsConfigs();
    }

    @GetMapping("/admin/tools-configs/active")
    @LogActivity("GET_ACTIVE_TOOLS_CONFIGS")
    public ResponseEntity<String> getActiveToolsConfigs() {
        log.info("Fetching active tools configs");
        return nexusBuddyService.getActiveToolsConfigs();
    }

    @GetMapping("/admin/tools-configs/client/{clientConfigId}")
    @LogActivity("GET_TOOLS_CONFIGS_BY_CLIENT")
    public ResponseEntity<String> getToolsConfigsByClientConfigId(@PathVariable Long clientConfigId) {
        log.info("Fetching tools configs for client: {}", clientConfigId);
        return nexusBuddyService.getToolsConfigsByClientConfigId(clientConfigId);
    }

    @PutMapping("/admin/tools-configs/{toolsConfigId}")
    @LogActivity("UPDATE_TOOLS_CONFIG")
    public ResponseEntity<String> updateToolsConfig(@PathVariable Long toolsConfigId, @RequestBody String payload) {
        log.info("Updating tools config: {}", toolsConfigId);
        return nexusBuddyService.updateToolsConfig(toolsConfigId, payload);
    }

    @DeleteMapping("/admin/tools-configs/{toolsConfigId}")
    @LogActivity("DEACTIVATE_TOOLS_CONFIG")
    public ResponseEntity<String> deactivateToolsConfig(@PathVariable Long toolsConfigId) {
        log.info("Deactivating tools config: {}", toolsConfigId);
        return nexusBuddyService.deactivateToolsConfig(toolsConfigId);
    }

    // ============================================
    // Tools Param Config APIs (Admin)
    // ============================================
    @PostMapping("/admin/tools-param-configs")
    @LogActivity("CREATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<String> createToolsParamConfig(@RequestBody String payload) {
        log.info("Creating tools param config");
        return nexusBuddyService.createToolsParamConfig(payload);
    }

    @GetMapping("/admin/tools-param-configs/{toolsParamConfigId}")
    @LogActivity("GET_TOOLS_PARAM_CONFIG_BY_ID")
    public ResponseEntity<String> getToolsParamConfigById(@PathVariable Long toolsParamConfigId) {
        log.info("Fetching tools param config: {}", toolsParamConfigId);
        return nexusBuddyService.getToolsParamConfigById(toolsParamConfigId);
    }

    @GetMapping("/admin/tools-param-configs")
    @LogActivity("GET_ALL_TOOLS_PARAM_CONFIGS")
    public ResponseEntity<String> getAllToolsParamConfigs() {
        log.info("Fetching all tools param configs");
        return nexusBuddyService.getAllToolsParamConfigs();
    }

    @GetMapping("/admin/tools-param-configs/active")
    @LogActivity("GET_ACTIVE_TOOLS_PARAM_CONFIGS")
    public ResponseEntity<String> getActiveToolsParamConfigs() {
        log.info("Fetching active tools param configs");
        return nexusBuddyService.getActiveToolsParamConfigs();
    }

    @GetMapping("/admin/tools-param-configs/tool/{toolsConfigId}")
    @LogActivity("GET_TOOLS_PARAM_CONFIGS_BY_TOOL")
    public ResponseEntity<String> getToolsParamConfigsByToolsConfigId(@PathVariable Long toolsConfigId) {
        log.info("Fetching tools param configs for tool: {}", toolsConfigId);
        return nexusBuddyService.getToolsParamConfigsByToolsConfigId(toolsConfigId);
    }

    @PutMapping("/admin/tools-param-configs/{toolsParamConfigId}")
    @LogActivity("UPDATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<String> updateToolsParamConfig(@PathVariable Long toolsParamConfigId,
            @RequestBody String payload) {
        log.info("Updating tools param config: {}", toolsParamConfigId);
        return nexusBuddyService.updateToolsParamConfig(toolsParamConfigId, payload);
    }

    @DeleteMapping("/admin/tools-param-configs/{toolsParamConfigId}")
    @LogActivity("DEACTIVATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<String> deactivateToolsParamConfig(@PathVariable Long toolsParamConfigId) {
        log.info("Deactivating tools param config: {}", toolsParamConfigId);
        return nexusBuddyService.deactivateToolsParamConfig(toolsParamConfigId);
    }

    // ============================================
    // Dashboard Analytics APIs
    // ============================================
    @GetMapping("/admin/dashboard/summary")
    @LogActivity("GET_DASHBOARD_SUMMARY")
    public ResponseEntity<String> getDashboardSummary(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching dashboard summary for range: {}, clientIds: {}", range, clientIds);
        return nexusBuddyService.getDashboardSummary(range, start, end, clientIds);
    }

    @GetMapping("/admin/dashboard/client-health")
    @LogActivity("GET_CLIENT_HEALTH")
    public ResponseEntity<String> getClientHealth(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching client health for range: {}, clientIds: {}", range, clientIds);
        return nexusBuddyService.getClientHealth(range, start, end, clientIds);
    }

    @GetMapping("/admin/dashboard/requests/trends")
    @LogActivity("GET_REQUEST_TRENDS")
    public ResponseEntity<String> getRequestTrends(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching request trends for range: {}, clientIds: {}", range, clientIds);
        return nexusBuddyService.getRequestTrends(range, start, end, clientIds);
    }

    @GetMapping("/admin/dashboard/tools/usage")
    @LogActivity("GET_TOOL_USAGE")
    public ResponseEntity<String> getToolUsage(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching tool usage for range: {}, clientIds: {}", range, clientIds);
        return nexusBuddyService.getToolUsage(range, start, end, clientIds);
    }

    @GetMapping("/admin/dashboard/performance")
    @LogActivity("GET_PERFORMANCE")
    public ResponseEntity<String> getPerformance(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) List<Long> clientIds) {
        log.info("Fetching performance metrics for range: {}, clientIds: {}", range, clientIds);
        return nexusBuddyService.getPerformance(range, start, end, clientIds);
    }

    @GetMapping("/admin/dashboard/config-insights")
    @LogActivity("GET_CONFIG_INSIGHTS")
    public ResponseEntity<String> getConfigInsights() {
        log.info("Fetching configuration insights");
        return nexusBuddyService.getConfigInsights();
    }

    // ============================================
    // Client Insights APIs
    // ============================================
    @GetMapping("/admin/client-insights/{clientId}")
    @LogActivity("GET_CLIENT_INSIGHTS")
    public ResponseEntity<String> getClientInsights(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        log.info("Fetching client insights for clientId: {}, range: {}", clientId, range);
        return nexusBuddyService.getClientInsights(clientId, range, start, end);
    }

    @GetMapping("/admin/client-insights/{clientId}/tools")
    @LogActivity("GET_CLIENT_TOOL_INSIGHTS")
    public ResponseEntity<String> getClientToolInsights(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageOffset,
            @RequestParam(required = false) String sort) {
        log.info("Fetching client tool insights for clientId: {}, range: {}, page: {}, size: {}",
                clientId, range, pageNo, pageOffset);
        return nexusBuddyService.getClientToolInsights(clientId, range, start, end, pageNo, pageOffset, sort);
    }

    @GetMapping("/admin/client-insights/{clientId}/logs")
    @LogActivity("GET_CLIENT_LOGS")
    public ResponseEntity<String> getClientLogs(
            @PathVariable Long clientId,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageOffset,
            @RequestParam(required = false) String sort) {
        log.info(
                "Fetching client logs for clientId: {}, toolName: {}, status: {}, statusCode: {}, httpMethod: {}, startDate: {}, endDate: {}, page: {}, size: {}",
                clientId, toolName, status, statusCode, httpMethod, startDate, endDate, pageNo, pageOffset);
        return nexusBuddyService.getClientLogs(clientId, toolName, status, statusCode, httpMethod, startDate, endDate,
                pageNo, pageOffset, sort);
    }
}
