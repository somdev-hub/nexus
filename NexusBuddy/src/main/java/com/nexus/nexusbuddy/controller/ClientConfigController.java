package com.nexus.nexusbuddy.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.model.entities.ClientConfig;
import com.nexus.nexusbuddy.payload.ClientConfigRequest;
import com.nexus.nexusbuddy.payload.ClientConfigResponse;
import com.nexus.nexusbuddy.service.interfaces.ClientConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Client Config management.
 * Provides CRUD operations for client configurations.
 */
@RestController
@RequestMapping("/nexusbuddy/admin/client-configs")
@RequiredArgsConstructor
@Slf4j
public class ClientConfigController {

    private final ClientConfigService clientConfigService;
    private final ModelMapper modelMapper;

    /**
     * Create a new client configuration.
     * POST /nexusbuddy/admin/client-configs
     * 
     * @param request Client configuration details
     * @return 201 Created with client configuration details
     */
    @PostMapping
    @LogActivity("CREATE_CLIENT_CONFIG")
    public ResponseEntity<?> createClientConfig(@Valid @RequestBody ClientConfigRequest request) {
        log.info("Creating new client config: {}", request.getClientName());
        return clientConfigService.createClientConfig(request);
    }

    /**
     * Get client configuration by ID.
     * GET /nexusbuddy/admin/client-configs/{clientConfigId}
     * 
     * @param clientConfigId Client configuration ID
     * @return 200 OK with client configuration details, or 404 Not Found
     */
    @GetMapping("/{clientConfigId}")
    @LogActivity("GET_CLIENT_CONFIG_BY_ID")
    public ResponseEntity<?> getClientConfigById(@PathVariable Long clientConfigId) {
        log.info("Fetching client config with ID: {}", clientConfigId);
        return clientConfigService.getClientConfigById(clientConfigId);
    }

    /**
     * Get all client configurations with pagination.
     * GET /nexusbuddy/admin/client-configs
     * 
     * @param page Page number (0-based, default 0)
     * @param size Page size (default 20)
     * @param sortBy Sort field (default clientConfigId)
     * @param sortDir Sort direction asc/desc (default asc)
     * @return 200 OK with paginated list of client configurations
     */
    @GetMapping
    @LogActivity("GET_ALL_CLIENT_CONFIGS")
    public ResponseEntity<Page<ClientConfigResponse>> getAllClientConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "clientConfigId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Fetching all client configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        return clientConfigService.getAllClientConfigs(page, size, sortBy, sortDir);
    }

    /**
     * Get only active client configurations with pagination.
     * GET /nexusbuddy/admin/client-configs/active
     * 
     * @param page Page number (0-based, default 0)
     * @param size Page size (default 20)
     * @param sortBy Sort field (default clientConfigId)
     * @param sortDir Sort direction asc/desc (default asc)
     * @return 200 OK with paginated list of active client configurations
     */
    @GetMapping("/active")
    @LogActivity("GET_ACTIVE_CLIENT_CONFIGS")
    public ResponseEntity<Page<ClientConfigResponse>> getActiveClientConfigs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "clientConfigId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Fetching active client configs with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        return clientConfigService.getActiveClientConfigs(page, size, sortBy, sortDir);
    }

    /**
     * Update client configuration.
     * PUT /nexusbuddy/admin/client-configs/{clientConfigId}
     * 
     * @param clientConfigId Client configuration ID
     * @param request Updated client configuration data
     * @return 200 OK with updated client configuration, or 404 Not Found
     */
    @PutMapping("/{clientConfigId}")
    @LogActivity("UPDATE_CLIENT_CONFIG")
    public ResponseEntity<?> updateClientConfig(
            @PathVariable Long clientConfigId,
            @Valid @RequestBody ClientConfigRequest request) {
        log.info("Updating client config with ID: {}", clientConfigId);
        return clientConfigService.updateClientConfig(clientConfigId, request);
    }

    /**
     * Deactivate client configuration (soft delete).
     * DELETE /nexusbuddy/admin/client-configs/{clientConfigId}
     * 
     * @param clientConfigId Client configuration ID
     * @return 200 OK with success message, or 404 Not Found
     */
    @DeleteMapping("/{clientConfigId}")
    @LogActivity("DEACTIVATE_CLIENT_CONFIG")
    public ResponseEntity<?> deactivateClientConfig(@PathVariable Long clientConfigId) {
        log.info("Deactivating client config with ID: {}", clientConfigId);
        return clientConfigService.deactivateClientConfig(clientConfigId);
    }

    /**
     * Find client configurations by allowed users list containing a domain.
     * GET /nexusbuddy/admin/client-configs/by-domain?domain=localhost:3001
     * 
     * @param domain Domain to search for (e.g., "localhost:3001")
     * @return 200 OK with list of matching client configurations
     */
    @GetMapping("/by-domain")
    @LogActivity("GET_CLIENT_CONFIGS_BY_DOMAIN")
    public ResponseEntity<List<ClientConfigResponse>> getClientConfigsByDomain(
            @RequestParam String domain) {
        log.info("Fetching client configs by domain: {}", domain);
        List<ClientConfig> configs = clientConfigService.findByAllowedUsersListContaining(domain);
        List<ClientConfigResponse> responses = configs.stream()
                .map(config -> modelMapper.map(config, ClientConfigResponse.class))
                .toList();
        return ResponseEntity.ok(responses);
    }
}