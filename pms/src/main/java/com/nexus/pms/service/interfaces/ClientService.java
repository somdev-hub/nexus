package com.nexus.pms.service.interfaces;

import com.nexus.pms.model.entities.ClientMaster;
import com.nexus.pms.payload.ClientMasterRequest;

import java.util.List;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for Client Master (Microservice Registry).
 * 
 * ClientMaster represents microservices that use PMS:
 * - HR microservice uses PMS for salary payments
 * - CORE microservice uses PMS for product payments
 * - etc.
 * 
 * This service provides simple CRUD operations for registering/managing
 * microservices.
 */
public interface ClientService {

    /**
     * Register a new microservice client.
     * 
     * @param request ClientMasterRequest with clientName
     * @return Created ClientMaster entity
     */
    ResponseEntity<?> createClient(ClientMaster request);

    /**
     * Get client by ID.
     * 
     * @param clientMasterId Client ID
     * @return ClientMaster entity
     */
    ResponseEntity<?> getClientById(Long clientMasterId);

    /**
     * Get all clients (active and inactive).
     * 
     * @return List of all ClientMaster entities
     */
    ResponseEntity<?> getAllClients();

    /**
     * Get only active clients.
     * 
     * @return List of active ClientMaster entities
     */
    ResponseEntity<?> getActiveClients();

    /**
     * Update client name and active status.
     * 
     * @param clientMasterId Client ID
     * @param request        Updated client data
     * @return Updated ClientMaster entity
     */
    ResponseEntity<?> updateClient(Long clientMasterId, ClientMasterRequest request);

}
