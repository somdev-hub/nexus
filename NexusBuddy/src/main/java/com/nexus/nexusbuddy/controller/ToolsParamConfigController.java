package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ToolsParamConfigRequest;
import com.nexus.nexusbuddy.service.interfaces.ToolsParamConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for Tools Param Config management.
 * Provides CRUD operations for tool parameter configurations.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/tools-param-configs")
@RequiredArgsConstructor
@Slf4j
public class ToolsParamConfigController {

    private final ToolsParamConfigService toolsParamConfigService;

    /**
     * Create a new tool parameter configuration.
     * POST /nexusbuddy/admin/tools-param-configs
     * 
     * @param request Tool parameter configuration details
     * @return 201 Created with tool parameter configuration details
     */
    @PostMapping
    @LogActivity("CREATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<?> createToolsParamConfig(@Valid @RequestBody ToolsParamConfigRequest request) {
        log.info("Creating new tools param config: {}", request.getParamName());
        return toolsParamConfigService.createToolsParamConfig(request);
    }

    /**
     * Get tool parameter configuration by ID.
     * GET /nexusbuddy/admin/tools-param-configs/{toolsParamConfigId}
     * 
     * @param toolsParamConfigId Tool parameter configuration ID
     * @return 200 OK with tool parameter configuration details, or 404 Not Found
     */
    @GetMapping("/{toolsParamConfigId}")
    @LogActivity("GET_TOOLS_PARAM_CONFIG_BY_ID")
    public ResponseEntity<?> getToolsParamConfigById(@PathVariable Long toolsParamConfigId) {
        log.info("Fetching tools param config with ID: {}", toolsParamConfigId);
        return toolsParamConfigService.getToolsParamConfigById(toolsParamConfigId);
    }

    /**
     * Get all tool parameter configurations.
     * GET /nexusbuddy/admin/tools-param-configs
     * 
     * @return 200 OK with list of all tool parameter configurations
     */
    @GetMapping
    @LogActivity("GET_ALL_TOOLS_PARAM_CONFIGS")
    public ResponseEntity<?> getAllToolsParamConfigs() {
        log.info("Fetching all tools param configs");
        return toolsParamConfigService.getAllToolsParamConfigs();
    }

    /**
     * Get only active tool parameter configurations.
     * GET /nexusbuddy/admin/tools-param-configs/active
     * 
     * @return 200 OK with list of active tool parameter configurations
     */
    @GetMapping("/active")
    @LogActivity("GET_ACTIVE_TOOLS_PARAM_CONFIGS")
    public ResponseEntity<?> getActiveToolsParamConfigs() {
        log.info("Fetching active tools param configs");
        return toolsParamConfigService.getActiveToolsParamConfigs();
    }

    /**
     * Get tool parameter configurations by tool config ID.
     * GET /nexusbuddy/admin/tools-param-configs/tool/{toolsConfigId}
     * 
     * @param toolsConfigId Tool configuration ID
     * @return 200 OK with list of tool parameter configurations for the tool
     */
    @GetMapping("/tool/{toolsConfigId}")
    @LogActivity("GET_TOOLS_PARAM_CONFIGS_BY_TOOL")
    public ResponseEntity<?> getToolsParamConfigsByToolsConfigId(@PathVariable Long toolsConfigId) {
        log.info("Fetching tools param configs for tools config ID: {}", toolsConfigId);
        return toolsParamConfigService.getToolsParamConfigsByToolsConfigId(toolsConfigId);
    }

    /**
     * Update tool parameter configuration.
     * PUT /nexusbuddy/admin/tools-param-configs/{toolsParamConfigId}
     * 
     * @param toolsParamConfigId Tool parameter configuration ID
     * @param request Updated tool parameter configuration data
     * @return 200 OK with updated tool parameter configuration, or 404 Not Found
     */
    @PutMapping("/{toolsParamConfigId}")
    @LogActivity("UPDATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<?> updateToolsParamConfig(
            @PathVariable Long toolsParamConfigId,
            @Valid @RequestBody ToolsParamConfigRequest request) {
        log.info("Updating tools param config with ID: {}", toolsParamConfigId);
        return toolsParamConfigService.updateToolsParamConfig(toolsParamConfigId, request);
    }

    /**
     * Deactivate tool parameter configuration (soft delete).
     * DELETE /nexusbuddy/admin/tools-param-configs/{toolsParamConfigId}
     * 
     * @param toolsParamConfigId Tool parameter configuration ID
     * @return 200 OK with success message, or 404 Not Found
     */
    @DeleteMapping("/{toolsParamConfigId}")
    @LogActivity("DEACTIVATE_TOOLS_PARAM_CONFIG")
    public ResponseEntity<?> deactivateToolsParamConfig(@PathVariable Long toolsParamConfigId) {
        log.info("Deactivating tools param config with ID: {}", toolsParamConfigId);
        return toolsParamConfigService.deactivateToolsParamConfig(toolsParamConfigId);
    }
}