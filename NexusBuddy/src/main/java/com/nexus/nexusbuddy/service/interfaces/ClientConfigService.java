package com.nexus.nexusbuddy.service.interfaces;

import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.payload.ClientConfigRequest;
import com.nexus.nexusbuddy.payload.ClientConfigResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for Client Config operations.
 * Provides CRUD operations for client configurations.
 */
public interface ClientConfigService {

    /**
     * Create a new client configuration.
     * 
     * @param request Client configuration details
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
     * Get all client configurations with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of ClientConfigResponse
     */
    ResponseEntity<Page<ClientConfigResponse>> getAllClientConfigs(int page, int size, String sortBy, String sortDir);

    /**
     * Get only active client configurations with pagination.
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction (asc/desc)
     * @return Paginated list of active ClientConfigResponse
     */
    ResponseEntity<Page<ClientConfigResponse>> getActiveClientConfigs(int page, int size, String sortBy, String sortDir);

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

    /**
     * Find client configurations by allowed users list containing a domain.
     * 
     * @param domain Domain to search for (e.g., "localhost:3001")
     * @return List of matching ClientConfig entities
     */
    List<ClientConfig> findByAllowedUsersListContaining(String domain);
}