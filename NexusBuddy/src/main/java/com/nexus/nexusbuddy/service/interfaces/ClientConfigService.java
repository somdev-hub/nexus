package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.payload.ClientConfigRequest;
import com.nexus.nexusbuddy.payload.ClientConfigResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for Client Config operations.
 * Manages client configurations for NexusBuddy tools.
 */
public interface ClientConfigService {

    /**
     * Create a new client configuration.
     * 
     * @param request ClientConfigRequest with client details
     * @return Created ClientConfigResponse
     */
    ResponseEntity<?> createClientConfig(ClientConfigRequest request);

    /**
     * Get client configuration by ID.
     * 
     * @param clientConfigId Client configuration ID
     * @return ClientConfigResponse
     */
    ResponseEntity<?> getClientConfigById(Long clientConfigId);

    /**
     * Get all client configurations.
     * 
     * @return List of all ClientConfigResponse
     */
    ResponseEntity<?> getAllClientConfigs();

    /**
     * Get only active client configurations.
     * 
     * @return List of active ClientConfigResponse
     */
    ResponseEntity<?> getActiveClientConfigs();

    /**
     * Update client configuration.
     * 
     * @param clientConfigId Client configuration ID
     * @param request Updated client configuration data
     * @return Updated ClientConfigResponse
     */
    ResponseEntity<?> updateClientConfig(Long clientConfigId, ClientConfigRequest request);

    /**
     * Deactivate client configuration (soft delete).
     * 
     * @param clientConfigId Client configuration ID
     * @return Success response
     */
    ResponseEntity<?> deactivateClientConfig(Long clientConfigId);
}