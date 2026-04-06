package com.nexus.pms.service.implementations;

import com.nexus.pms.exception.ResourceNotFoundException;
import com.nexus.pms.model.entities.ClientMaster;
import com.nexus.pms.model.entities.ClientPaymentTypes;
import com.nexus.pms.payload.ClientMasterRequest;
import com.nexus.pms.repository.ClientRepository;
import com.nexus.pms.repository.ClientPaymentTypesRepository;
import com.nexus.pms.service.interfaces.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
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
    private final ClientPaymentTypesRepository clientPaymentTypesRepository;

    @Override
    public ResponseEntity<?> createClient(ClientMaster request) {
        if (ObjectUtils.isEmpty(request)) {
            return ResponseEntity.badRequest().body("Invalid input: Client data is required");

        }
        try {
            ClientMaster saveclient = clientRepository.save(request);
            if (!ObjectUtils.isEmpty(request.getClientPaymentTypes())) {
                List<ClientPaymentTypes> paymentTypes = request.getClientPaymentTypes().stream()
                        .map(pt -> {
                            pt.setClientMaster(saveclient);
                            return pt;
                        })
                        .toList();
                clientPaymentTypesRepository.saveAll(paymentTypes);
            }
            return ResponseEntity.status(201).body(saveclient);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("An error occurred while creating client: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getClientById(Long clientMasterId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClientById'");
    }

    @Override
    public ResponseEntity<?> getAllClients() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllClients'");
    }

    @Override
    public ResponseEntity<?> getActiveClients() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getActiveClients'");
    }

    @Override
    public ResponseEntity<?> updateClient(Long clientMasterId, ClientMasterRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateClient'");
    }

}
