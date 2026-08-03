package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.model.entities.ToolsParamConfig;
import com.nexus.nexusbuddy.payload.ToolsParamConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsParamConfigResponse;
import org.springframework.data.domain.Page;
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
     * Get all tool parameter configurations with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of all ToolsParamConfigResponse
     */
    ResponseEntity<Page<ToolsParamConfigResponse>> getAllToolsParamConfigs(int page, int size, String sortBy, String sortDir);

    /**
     * Get only active tool parameter configurations with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of active ToolsParamConfigResponse
     */
    ResponseEntity<Page<ToolsParamConfigResponse>> getActiveToolsParamConfigs(int page, int size, String sortBy, String sortDir);

    /**
     * Get tool parameter configurations by tool config ID with pagination.
     * 
     * @param toolsConfigId Tool configuration ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of ToolsParamConfigResponse
     */
    ResponseEntity<Page<ToolsParamConfigResponse>> getToolsParamConfigsByToolsConfigId(Long toolsConfigId, int page, int size, String sortBy, String sortDir);

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