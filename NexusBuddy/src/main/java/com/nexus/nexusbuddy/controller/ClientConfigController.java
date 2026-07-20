package com.nexus.nexusbuddy.controller;

import com.nexus.nexusbuddy.annotation.LogActivity;
import com.nexus.nexusbuddy.payload.ClientConfigRequest;
import com.nexus.nexusbuddy.service.interfaces.ClientConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
     * Get all client configurations.
     * GET /nexusbuddy/admin/client-configs
     * 
     * @return 200 OK with list of all client configurations
     */
    @GetMapping
    @LogActivity("GET_ALL_CLIENT_CONFIGS")
    public ResponseEntity<?> getAllClientConfigs() {
        log.info("Fetching all client configs");
        return clientConfigService.getAllClientConfigs();
    }

    /**
     * Get only active client configurations.
     * GET /nexusbuddy/admin/client-configs/active
     * 
     * @return 200 OK with list of active client configurations
     */
    @GetMapping("/active")
    @LogActivity("GET_ACTIVE_CLIENT_CONFIGS")
    public ResponseEntity<?> getActiveClientConfigs() {
        log.info("Fetching active client configs");
        return clientConfigService.getActiveClientConfigs();
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
}