package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ToolsConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsConfigResponse;
import com.nexus.nexusbuddy.service.interfaces.ToolsConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
     * Get all tool configurations with pagination.
     * GET /nexusbuddy/admin/tools-configs
     * 
     * @param page Page number (0-based, default 0)
     * @param size Page size (default 20)
     * @param sortBy Sort field (default toolsConfigId)
     * @param sortDir Sort direction asc/desc (default asc)
     * @return 200 OK with paginated list of all tool configurations
     */
    @GetMapping
    @LogActivity("GET_ALL_TOOLS_CONFIGS")
    public ResponseEntity<Page<ToolsConfigResponse>> getAllToolsConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "toolsConfigId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Fetching all tools configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        return toolsConfigService.getAllToolsConfigs(page, size, sortBy, sortDir);
    }

    /**
     * Get only active tool configurations with pagination.
     * GET /nexusbuddy/admin/tools-configs/active
     * 
     * @param page Page number (0-based, default 0)
     * @param size Page size (default 20)
     * @param sortBy Sort field (default toolsConfigId)
     * @param sortDir Sort direction asc/desc (default asc)
     * @return 200 OK with paginated list of active tool configurations
     */
    @GetMapping("/active")
    @LogActivity("GET_ACTIVE_TOOLS_CONFIGS")
    public ResponseEntity<Page<ToolsConfigResponse>> getActiveToolsConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "toolsConfigId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Fetching active tools configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        return toolsConfigService.getActiveToolsConfigs(page, size, sortBy, sortDir);
    }

    /**
     * Get tool configurations by client config ID with pagination.
     * GET /nexusbuddy/admin/tools-configs/client/{clientConfigId}
     * 
     * @param clientConfigId Client configuration ID
     * @param page Page number (0-based, default 0)
     * @param size Page size (default 20)
     * @param sortBy Sort field (default toolsConfigId)
     * @param sortDir Sort direction asc/desc (default asc)
     * @return 200 OK with paginated list of tool configurations for the client
     */
    @GetMapping("/client/{clientConfigId}")
    @LogActivity("GET_TOOLS_CONFIGS_BY_CLIENT")
    public ResponseEntity<Page<ToolsConfigResponse>> getToolsConfigsByClientConfigId(
            @PathVariable Long clientConfigId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "toolsConfigId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Fetching tools configs for client config ID: {} with pagination: page={}, size={}, sortBy={}, sortDir={}", clientConfigId, page, size, sortBy, sortDir);
        return toolsConfigService.getToolsConfigsByClientConfigId(clientConfigId, page, size, sortBy, sortDir);
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