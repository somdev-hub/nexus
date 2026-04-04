package com.nexus.pms.service.interfaces;

import com.nexus.pms.model.entities.ClientMaster;
import com.nexus.pms.payload.ClientMasterRequest;

import java.util.List;

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
    ClientMaster createClient(ClientMasterRequest request);

    /**
     * Get client by ID.
     * 
     * @param clientMasterId Client ID
     * @return ClientMaster entity
     */
    ClientMaster getClientById(Long clientMasterId);

    /**
     * Get all clients (active and inactive).
     * 
     * @return List of all ClientMaster entities
     */
    List<ClientMaster> getAllClients();

    /**
     * Get only active clients.
     * 
     * @return List of active ClientMaster entities
     */
    List<ClientMaster> getActiveClients();

    /**
     * Update client name and active status.
     * 
     * @param clientMasterId Client ID
     * @param request        Updated client data
     * @return Updated ClientMaster entity
     */
    ClientMaster updateClient(Long clientMasterId, ClientMasterRequest request);

    /**
     * Deactivate a client (soft delete).
     * 
     * @param clientMasterId Client ID
     * @return Deactivated ClientMaster entity (isActive = false)
     */
    ClientMaster deactivateClient(Long clientMasterId);
}
