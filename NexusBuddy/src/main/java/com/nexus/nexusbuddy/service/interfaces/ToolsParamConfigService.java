package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.payload.ToolsParamConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsParamConfigResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for Tools Param Config operations.
 * Manages tool parameter configurations for NexusBuddy tools.
 */
public interface ToolsParamConfigService {

    /**
     * Create a new tool parameter configuration.
     * 
     * @param request ToolsParamConfigRequest with parameter details
     * @return Created ToolsParamConfigResponse
     */
    ResponseEntity<?> createToolsParamConfig(ToolsParamConfigRequest request);

    /**
     * Get tool parameter configuration by ID.
     * 
     * @param toolsParamConfigId Tool parameter configuration ID
     * @return ToolsParamConfigResponse
     */
    ResponseEntity<?> getToolsParamConfigById(Long toolsParamConfigId);

    /**
     * Get all tool parameter configurations.
     * 
     * @return List of all ToolsParamConfigResponse
     */
    ResponseEntity<?> getAllToolsParamConfigs();

    /**
     * Get only active tool parameter configurations.
     * 
     * @return List of active ToolsParamConfigResponse
     */
    ResponseEntity<?> getActiveToolsParamConfigs();

    /**
     * Get tool parameter configurations by tool config ID.
     * 
     * @param toolsConfigId Tool configuration ID
     * @return List of ToolsParamConfigResponse
     */
    ResponseEntity<?> getToolsParamConfigsByToolsConfigId(Long toolsConfigId);

    /**
     * Update tool parameter configuration.
     * 
     * @param toolsParamConfigId Tool parameter configuration ID
     * @param request Updated tool parameter configuration data
     * @return Updated ToolsParamConfigResponse
     */
    ResponseEntity<?> updateToolsParamConfig(Long toolsParamConfigId, ToolsParamConfigRequest request);

    /**
     * Deactivate tool parameter configuration (soft delete).
     * 
     * @param toolsParamConfigId Tool parameter configuration ID
     * @return Success response
     */
    ResponseEntity<?> deactivateToolsParamConfig(Long toolsParamConfigId);
}