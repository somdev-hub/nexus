package com.nexus.pms.repository;

import com.nexus.pms.model.entities.ClientPaymentTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for ClientPaymentTypes entity.
 * Manages CRUD operations for client payment type configurations.
 */
@Repository
public interface ClientPaymentTypesRepository extends JpaRepository<ClientPaymentTypes, Long> {

    /**
     * Find all payment types for a specific client.
     *
     * @param clientMasterId The client master ID
     * @return List of payment types for the client
     */
    List<ClientPaymentTypes> findByClientMaster_ClientMasterId(Long clientMasterId);

    /**
     * Delete all payment types for a specific client.
     *
     * @param clientMasterId The client master ID
     */
    void deleteByClientMaster_ClientMasterId(Long clientMasterId);
}
