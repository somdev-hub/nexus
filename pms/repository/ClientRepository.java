package com.nexus.pms.repository;

import com.nexus.pms.model.entities.ClientMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ClientMaster entity.
 * Manages CRUD operations and custom queries for client configuration.
 */
@Repository
public interface ClientRepository extends JpaRepository<ClientMaster, Long> {

    /**
     * Find client by unique client code.
     *
     * @param clientCode The client code (e.g., "HR", "CORE")
     * @return ClientMaster if found
     */
    Optional<ClientMaster> findByClientCode(String clientCode);

    /**
     * Check if client code exists.
     *
     * @param clientCode The client code to check
     * @return true if exists, false otherwise
     */
    boolean existsByClientCode(String clientCode);

    /**
     * Check if client code exists excluding a specific client ID.
     *
     * @param clientCode The client code
     * @param excludeId  The client ID to exclude
     * @return true if exists (excluding the specified ID), false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClientMaster c WHERE c.clientCode = :clientCode AND c.clientMasterId != :excludeId")
    boolean existsByClientCodeExcludingId(@Param("clientCode") String clientCode, @Param("excludeId") Long excludeId);

    /**
     * Get all active clients.
     *
     * @return List of active ClientMaster records
     */
    @Query("SELECT c FROM ClientMaster c WHERE c.isActive = true ORDER BY c.clientName ASC")
    List<ClientMaster> findAllActive();

    /**
     * Get all clients ordered by name.
     *
     * @return List of all ClientMaster records
     */
    @Query("SELECT c FROM ClientMaster c ORDER BY c.clientName ASC")
    List<ClientMaster> findAllOrdered();
}
