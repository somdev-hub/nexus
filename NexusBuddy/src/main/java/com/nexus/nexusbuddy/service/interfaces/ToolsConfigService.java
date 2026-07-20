package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.payload.ToolsConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsConfigResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for Tools Config operations.
 * Manages tool configurations for NexusBuddy clients.
 */
public interface ToolsConfigService {

    /**
     * Create a new tool configuration.
     * 
     * @param request ToolsConfigRequest with tool details
     * @return Created ToolsConfigResponse
     */
    ResponseEntity<?> createToolsConfig(ToolsConfigRequest request);

    /**
     * Get tool configuration by ID.
     * 
     * @param toolsConfigId Tool configuration ID
     * @return ToolsConfigResponse
     */
    ResponseEntity<?> getToolsConfigById(Long toolsConfigId);

    /**
     * Get all tool configurations.
     * 
     * @return List of all ToolsConfigResponse
     */
    ResponseEntity<?> getAllToolsConfigs();

    /**
     * Get only active tool configurations.
     * 
     * @return List of active ToolsConfigResponse
     */
    ResponseEntity<?> getActiveToolsConfigs();

    /**
     * Get tool configurations by client config ID.
     * 
     * @param clientConfigId Client configuration ID
     * @return List of ToolsConfigResponse
     */
    ResponseEntity<?> getToolsConfigsByClientConfigId(Long clientConfigId);

    /**
     * Update tool configuration.
     * 
     * @param toolsConfigId Tool configuration ID
     * @param request Updated tool configuration data
     * @return Updated ToolsConfigResponse
     */
    ResponseEntity<?> updateToolsConfig(Long toolsConfigId, ToolsConfigRequest request);

    /**
     * Deactivate tool configuration (soft delete).
     * 
     * @param toolsConfigId Tool configuration ID
     * @return Success response
     */
    ResponseEntity<?> deactivateToolsConfig(Long toolsConfigId);
}