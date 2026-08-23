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
@RequestMapping("/pms/admin/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;

    /**
     * Create a new client (register a microservice).
     * POST /pms/admin/clients
     * 
     * Accepts both ClientMaster entity and ClientMasterRequest DTO.
     * 
     * Example 1 - Simple client:
     * {
     * "clientName": "Human Resources System",
     * "clientCode": "HR",
     * "isActive": true
     * }
     * 
     * Example 2 - Client with payment types:
     * {
     * "clientName": "Employee Payroll System",
     * "clientCode": "PAYROLL",
     * "isActive": true,
     * "paymentTypes": [
     * {
     * "clientPaymentTypeName": "Salary Payments",
     * "recipient": "CUSTOMER",
     * "description": "Monthly salary disbursement"
     * }
     * ]
     * }
     * 
     * @param request Client details (ClientMaster or ClientMasterRequest)
     * @return 201 Created with client details
     */
    @PostMapping
    public ResponseEntity<?> createClient(@Valid @RequestBody ClientMaster request) {
        log.info("Creating new client: {}", request.getClientName());
        try {
            return clientService.createClient(request);
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
    public ResponseEntity<?> getClientById(@PathVariable Long clientMasterId) {
        log.info("Fetching client with ID: {}", clientMasterId);
        try {
            return clientService.getClientById(clientMasterId);
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
    public ResponseEntity<?> getAllClients() {
        log.info("Fetching all clients");
        try {
            return clientService.getAllClients();
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
    public ResponseEntity<?> getActiveClients() {
        log.info("Fetching active clients");
        try {
            return clientService.getActiveClients();
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
    public ResponseEntity<?> updateClient(
            @PathVariable Long clientMasterId,
            @Valid @RequestBody ClientMasterRequest request) {
        log.info("Updating client with ID: {}", clientMasterId);
        try {
            return clientService.updateClient(clientMasterId, request);
        } catch (Exception e) {
            log.error("Error updating client: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
