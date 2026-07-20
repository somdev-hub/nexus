package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.NexusBuddyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nexusbuddy/admin")
@RequiredArgsConstructor
@Slf4j
public class NexusBuddyController {

    private final NexusBuddyService nexusBuddyService;

    // ============================================
    // Client Config APIs
    // ============================================
    @PostMapping("/client-configs")
    @LogActivity("CREATE_CLIENT_CONFIG")
    public ResponseEntity<String> createClientConfig(@RequestBody String payload) {
        log.info("Creating client config");
        return nexusBuddyService.createClientConfig(payload);
    }

    @GetMapping("/client-configs/{clientConfigId}")
    @LogActivity("GET_CLIENT_CONFIG_BY_ID")
    public ResponseEntity<String> getClientConfigById(@PathVariable Long clientConfigId) {
        log.info("Fetching client config: {}", clientConfigId);
        return nexusBuddyService.getClientConfigById(clientConfigId);
    }

    @GetMapping("/client-configs")
    @LogActivity("GET_ALL_CLIENT_CONFIGS")
    public ResponseEntity<String> getAllClientConfigs() {
        log.info("Fetching all client configs");
        return nexusBuddyService.getAllClientConfigs();
    }

    @GetMapping("/client-configs/active")
    @LogActivity("GET_ACTIVE_CLIENT_CONFIGS")
    public ResponseEntity<String> getActiveClientConfigs() {
        log.info("Fetching active client configs");
        return nexusBuddyService.getActiveClientConfigs();
    }

    @PutMapping("/client-configs/{clientConfigId}")
    @LogActivity("UPDATE_CLIENT_CONFIG")
    public ResponseEntity<String> updateClientConfig(@PathVariable Long clientConfigId, @RequestBody String payload) {
        log.info("Updating client config: {}", clientConfigId);
        return nexusBuddyService.updateClientConfig(clientConfigId, payload);
    }

    @DeleteMapping("/client-configs/{clientConfigId}")
    @LogActivity("DEACTIVATE_CLIENT_CONFIG")
    public ResponseEntity<String> deactivateClientConfig(@PathVariable Long clientConfigId) {
        log.info("Deactivating client config: {}", clientConfigId);
        return nexusBuddyService.deactivateClientConfig(clientConfigId);
    }

    // ============================================
    // Tools Config APIs
    // ============================================
    @PostMapping("/tools-configs")
    @LogActivity("CREATE_TOOLS_CONFIG")
    public ResponseEntity<String> createToolsConfig(@RequestBody String payload) {
        log.info("Creating tools config");
        return nexusBuddyService.createToolsConfig(payload);
    }

    @GetMapping("/tools-configs/{toolsConfigId}")
    @LogActivity("GET_TOOLS_CONFIG_BY_ID")
    public ResponseEntity<String> getToolsConfigById(@PathVariable Long toolsConfigId) {
        log.info("Fetching tools config: {}", toolsConfigId);
        return nexusBuddyService.getToolsConfigById(toolsConfigId);
    }

    @GetMapping("/tools-configs")
    @LogActivity("GET_ALL_TOOLS_CONFIGS")
    public ResponseEntity<String> getAllToolsConfigs() {
        log.info("Fetching all tools configs");
        return nexusBuddyService.getAllToolsConfigs();
    }

    @GetMapping("/tools-configs/active")
    @LogActivity("GET_ACTIVE_TOOLS_CONFIGS")
    public ResponseEntity<String> getActiveToolsConfigs() {
        log.info("Fetching active tools configs");
        return nexusBuddyService.getActiveToolsConfigs();
    }

    @GetMapping("/tools-configs/client/{clientConfigId}")
    @LogActivity("GET_TOOLS_CONFIGS_BY_CLIENT")
    public ResponseEntity<String> getToolsConfigsByClientConfigId(@PathVariable Long clientConfigId) {
        log.info("Fetching tools configs for client: {}", clientConfigId);
        return nexusBuddyService.getToolsConfigsByClientConfigId(clientConfigId);
    }

    @PutMapping("/tools-configs/{toolsConfigId}")
    @LogActivity("UPDATE_TOOLS_CONFIG")
    public ResponseEntity<String> updateToolsConfig(@PathVariable Long toolsConfigId, @RequestBody String payload) {
        log.info("Updating tools config: {}", toolsConfigId);
        return nexusBuddyService.updateToolsConfig(toolsConfigId, payload);
    }

    @DeleteMapping("/tools-configs/{toolsConfigId}")
    @LogActivity("DEACTIVATE_TOOLS_CONFIG")
    public ResponseEntity<String> deactivateToolsConfig(@PathVariable Long toolsConfigId) {
        log.info("Deactivating tools config: {}", toolsConfigId);
        return nexusBuddyService.deactivateToolsConfig(toolsConfigId);
    }

    // ============================================
    // Tools Param Config APIs
    // ============================================
    @PostMapping("/tools-param-configs")
    @LogActivity("CREATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<String> createToolsParamConfig(@RequestBody String payload) {
        log.info("Creating tools param config");
        return nexusBuddyService.createToolsParamConfig(payload);
    }

    @GetMapping("/tools-param-configs/{toolsParamConfigId}")
    @LogActivity("GET_TOOLS_PARAM_CONFIG_BY_ID")
    public ResponseEntity<String> getToolsParamConfigById(@PathVariable Long toolsParamConfigId) {
        log.info("Fetching tools param config: {}", toolsParamConfigId);
        return nexusBuddyService.getToolsParamConfigById(toolsParamConfigId);
    }

    @GetMapping("/tools-param-configs")
    @LogActivity("GET_ALL_TOOLS_PARAM_CONFIGS")
    public ResponseEntity<String> getAllToolsParamConfigs() {
        log.info("Fetching all tools param configs");
        return nexusBuddyService.getAllToolsParamConfigs();
    }

    @GetMapping("/tools-param-configs/active")
    @LogActivity("GET_ACTIVE_TOOLS_PARAM_CONFIGS")
    public ResponseEntity<String> getActiveToolsParamConfigs() {
        log.info("Fetching active tools param configs");
        return nexusBuddyService.getActiveToolsParamConfigs();
    }

    @GetMapping("/tools-param-configs/tool/{toolsConfigId}")
    @LogActivity("GET_TOOLS_PARAM_CONFIGS_BY_TOOL")
    public ResponseEntity<String> getToolsParamConfigsByToolsConfigId(@PathVariable Long toolsConfigId) {
        log.info("Fetching tools param configs for tool: {}", toolsConfigId);
        return nexusBuddyService.getToolsParamConfigsByToolsConfigId(toolsConfigId);
    }

    @PutMapping("/tools-param-configs/{toolsParamConfigId}")
    @LogActivity("UPDATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<String> updateToolsParamConfig(@PathVariable Long toolsParamConfigId, @RequestBody String payload) {
        log.info("Updating tools param config: {}", toolsParamConfigId);
        return nexusBuddyService.updateToolsParamConfig(toolsParamConfigId, payload);
    }

    @DeleteMapping("/tools-param-configs/{toolsParamConfigId}")
    @LogActivity("DEACTIVATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<String> deactivateToolsParamConfig(@PathVariable Long toolsParamConfigId) {
        log.info("Deactivating tools param config: {}", toolsParamConfigId);
        return nexusBuddyService.deactivateToolsParamConfig(toolsParamConfigId);
    }
}
