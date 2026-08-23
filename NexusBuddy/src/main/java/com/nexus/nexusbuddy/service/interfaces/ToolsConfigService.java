package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.model.entities.ToolsConfig;
import com.nexus.nexusbuddy.payload.ToolsConfigRequest;
import com.nexus.nexusbuddy.payload.ToolsConfigResponse;
import org.springframework.data.domain.Page;
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
     * Get all tool configurations with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of ToolsConfigResponse
     */
    ResponseEntity<Page<ToolsConfigResponse>> getAllToolsConfigs(int page, int size, String sortBy, String sortDir);

    /**
     * Get only active tool configurations with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of active ToolsConfigResponse
     */
    ResponseEntity<Page<ToolsConfigResponse>> getActiveToolsConfigs(int page, int size, String sortBy, String sortDir);

    /**
     * Get tool configurations by client config ID with pagination.
     * 
     * @param clientConfigId Client configuration ID
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of ToolsConfigResponse
     */
    ResponseEntity<Page<ToolsConfigResponse>> getToolsConfigsByClientConfigId(Long clientConfigId, int page, int size, String sortBy, String sortDir);

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