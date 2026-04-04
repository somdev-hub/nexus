package com.nexus.pms.controller;

import com.nexus.pms.model.entities.ClientMaster;
import com.nexus.pms.payload.ClientMasterRequest;
import com.nexus.pms.service.interfaces.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for Client Master (Microservice Registry).
 * 
 * Simple admin API to register/manage microservices using PMS.
 * Example: HR service registers as client, CORE service as another client.
 * 
 * ENDPOINTS:
 * - POST /api/admin/clients Create new client
 * - GET /api/admin/clients/{id} Get client by ID
 * - GET /api/admin/clients List all clients
 * - GET /api/admin/clients/active List active clients only
 * - PUT /api/admin/clients/{id} Update client
 * - DELETE /api/admin/clients/{id} Deactivate client
 */
@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;

    /**
     * Create a new client (register a microservice).
     * POST /api/admin/clients
     * 
     * @param request Client name
     * @return 201 Created with client details
     */
    @PostMapping
    public ResponseEntity<ClientMaster> createClient(@Valid @RequestBody ClientMasterRequest request) {
        log.info("Creating new client: {}", request.getClientName());
        try {
            ClientMaster client = clientService.createClient(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(client);
        } catch (Exception e) {
            log.error("Error creating client: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get client by ID.
     * GET /api/admin/clients/{clientMasterId}
     * 
     * @param clientMasterId Client ID
     * @return 200 OK with client details, or 404 Not Found
     */
    @GetMapping("/{clientMasterId}")
    public ResponseEntity<ClientMaster> getClientById(@PathVariable Long clientMasterId) {
        log.info("Fetching client with ID: {}", clientMasterId);
        try {
            ClientMaster client = clientService.getClientById(clientMasterId);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            log.error("Client not found: {}", clientMasterId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all clients.
     * GET /api/admin/clients
     * 
     * @return 200 OK with list of all clients
     */
    @GetMapping
    public ResponseEntity<List<ClientMaster>> getAllClients() {
        log.info("Fetching all clients");
        try {
            List<ClientMaster> clients = clientService.getAllClients();
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            log.error("Error fetching clients: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get only active clients.
     * GET /api/admin/clients/active
     * 
     * @return 200 OK with list of active clients
     */
    @GetMapping("/active")
    public ResponseEntity<List<ClientMaster>> getActiveClients() {
        log.info("Fetching active clients");
        try {
            List<ClientMaster> clients = clientService.getActiveClients();
            return ResponseEntity.ok(clients);
        } catch (Exception e) {
            log.error("Error fetching active clients: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update client.
     * PUT /api/admin/clients/{clientMasterId}
     * 
     * @param clientMasterId Client ID
     * @param request        Updated client data
     * @return 200 OK with updated client, or 404 Not Found
     */
    @PutMapping("/{clientMasterId}")
    public ResponseEntity<ClientMaster> updateClient(
            @PathVariable Long clientMasterId,
            @Valid @RequestBody ClientMasterRequest request) {
        log.info("Updating client with ID: {}", clientMasterId);
        try {
            ClientMaster client = clientService.updateClient(clientMasterId, request);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            log.error("Error updating client: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deactivate client (soft delete).
     * DELETE /api/admin/clients/{clientMasterId}
     * 
     * @param clientMasterId Client ID
     * @return 200 OK with deactivated client, or 404 Not Found
     */
    @DeleteMapping("/{clientMasterId}")
    public ResponseEntity<ClientMaster> deactivateClient(@PathVariable Long clientMasterId) {
        log.info("Deactivating client: {}", clientMasterId);
        try {
            ClientMaster client = clientService.deactivateClient(clientMasterId);
            return ResponseEntity.ok(client);
        } catch (Exception e) {
            log.error("Error deactivating client: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
