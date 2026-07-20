package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ToolsConfigRequest;
import com.nexus.nexusbuddy.service.interfaces.ToolsConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for Tools Config management.
 * Provides CRUD operations for tool configurations.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/tools-configs")
@RequiredArgsConstructor
@Slf4j
public class ToolsConfigController {

    private final ToolsConfigService toolsConfigService;

    /**
     * Create a new tool configuration.
     * POST /nexusbuddy/admin/tools-configs
     * 
     * @param request Tool configuration details
     * @return 201 Created with tool configuration details
     */
    @PostMapping
    @LogActivity("CREATE_TOOLS_CONFIG")
    public ResponseEntity<?> createToolsConfig(@Valid @RequestBody ToolsConfigRequest request) {
        log.info("Creating new tools config: {}", request.getToolName());
        return toolsConfigService.createToolsConfig(request);
    }

    /**
     * Get tool configuration by ID.
     * GET /nexusbuddy/admin/tools-configs/{toolsConfigId}
     * 
     * @param toolsConfigId Tool configuration ID
     * @return 200 OK with tool configuration details, or 404 Not Found
     */
    @GetMapping("/{toolsConfigId}")
    @LogActivity("GET_TOOLS_CONFIG_BY_ID")
    public ResponseEntity<?> getToolsConfigById(@PathVariable Long toolsConfigId) {
        log.info("Fetching tools config with ID: {}", toolsConfigId);
        return toolsConfigService.getToolsConfigById(toolsConfigId);
    }

    /**
     * Get all tool configurations.
     * GET /nexusbuddy/admin/tools-configs
     * 
     * @return 200 OK with list of all tool configurations
     */
    @GetMapping
    @LogActivity("GET_ALL_TOOLS_CONFIGS")
    public ResponseEntity<?> getAllToolsConfigs() {
        log.info("Fetching all tools configs");
        return toolsConfigService.getAllToolsConfigs();
    }

    /**
     * Get only active tool configurations.
     * GET /nexusbuddy/admin/tools-configs/active
     * 
     * @return 200 OK with list of active tool configurations
     */
    @GetMapping("/active")
    @LogActivity("GET_ACTIVE_TOOLS_CONFIGS")
    public ResponseEntity<?> getActiveToolsConfigs() {
        log.info("Fetching active tools configs");
        return toolsConfigService.getActiveToolsConfigs();
    }

    /**
     * Get tool configurations by client config ID.
     * GET /nexusbuddy/admin/tools-configs/client/{clientConfigId}
     * 
     * @param clientConfigId Client configuration ID
     * @return 200 OK with list of tool configurations for the client
     */
    @GetMapping("/client/{clientConfigId}")
    @LogActivity("GET_TOOLS_CONFIGS_BY_CLIENT")
    public ResponseEntity<?> getToolsConfigsByClientConfigId(@PathVariable Long clientConfigId) {
        log.info("Fetching tools configs for client config ID: {}", clientConfigId);
        return toolsConfigService.getToolsConfigsByClientConfigId(clientConfigId);
    }

    /**
     * Update tool configuration.
     * PUT /nexusbuddy/admin/tools-configs/{toolsConfigId}
     * 
     * @param toolsConfigId Tool configuration ID
     * @param request Updated tool configuration data
     * @return 200 OK with updated tool configuration, or 404 Not Found
     */
    @PutMapping("/{toolsConfigId}")
    @LogActivity("UPDATE_TOOLS_CONFIG")
    public ResponseEntity<?> updateToolsConfig(
            @PathVariable Long toolsConfigId,
            @Valid @RequestBody ToolsConfigRequest request) {
        log.info("Updating tools config with ID: {}", toolsConfigId);
        return toolsConfigService.updateToolsConfig(toolsConfigId, request);
    }

    /**
     * Deactivate tool configuration (soft delete).
     * DELETE /nexusbuddy/admin/tools-configs/{toolsConfigId}
     * 
     * @param toolsConfigId Tool configuration ID
     * @return 200 OK with success message, or 404 Not Found
     */
    @DeleteMapping("/{toolsConfigId}")
    @LogActivity("DEACTIVATE_TOOLS_CONFIG")
    public ResponseEntity<?> deactivateToolsConfig(@PathVariable Long toolsConfigId) {
        log.info("Deactivating tools config with ID: {}", toolsConfigId);
        return toolsConfigService.deactivateToolsConfig(toolsConfigId);
    }
}