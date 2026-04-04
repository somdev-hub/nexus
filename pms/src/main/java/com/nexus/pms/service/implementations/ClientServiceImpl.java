package com.nexus.pms.service.implementations;

import com.nexus.pms.exception.ResourceNotFoundException;
import com.nexus.pms.model.entities.ClientMaster;
import com.nexus.pms.payload.ClientMasterRequest;
import com.nexus.pms.repository.ClientRepository;
import com.nexus.pms.service.interfaces.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Client Master (Microservice Registry).
 * 
 * Manages registration and lifecycle of microservices using PMS.
 * Examples:
 * - HR microservice registers to use PMS for salary payments
 * - CORE microservice registers to use PMS for product payments
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    public ClientMaster createClient(ClientMasterRequest request) {
        log.info("Creating new client: {}", request.getClientName());

        ClientMaster client = new ClientMaster();
        client.setClientName(request.getClientName());
        client.setIsActive(true);

        ClientMaster savedClient = clientRepository.save(client);
        log.info("Client created with ID: {}", savedClient.getClientMasterId());

        return savedClient;
    }

    @Override
    public ClientMaster getClientById(Long clientMasterId) {
        log.debug("Fetching client with ID: {}", clientMasterId);

        return clientRepository.findById(clientMasterId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientMasterId));
    }

    @Override
    public List<ClientMaster> getAllClients() {
        log.debug("Fetching all clients");
        return clientRepository.findAll();
    }

    @Override
    public List<ClientMaster> getActiveClients() {
        log.debug("Fetching active clients");
        return clientRepository.findAllActive();
    }

    @Override
    public ClientMaster updateClient(Long clientMasterId, ClientMasterRequest request) {
        log.info("Updating client with ID: {}", clientMasterId);

        ClientMaster client = getClientById(clientMasterId);

        if (request.getClientName() != null) {
            client.setClientName(request.getClientName());
        }

        if (request.getIsActive() != null) {
            client.setIsActive(request.getIsActive());
        }

        ClientMaster updatedClient = clientRepository.save(client);
        log.info("Client updated: {}", clientMasterId);

        return updatedClient;
    }

    @Override
    public ClientMaster deactivateClient(Long clientMasterId) {
        log.info("Deactivating client: {}", clientMasterId);

        ClientMaster client = getClientById(clientMasterId);
        client.setIsActive(false);

        ClientMaster deactivatedClient = clientRepository.save(client);
        log.info("Client deactivated: {}", clientMasterId);

        return deactivatedClient;
    }
}
