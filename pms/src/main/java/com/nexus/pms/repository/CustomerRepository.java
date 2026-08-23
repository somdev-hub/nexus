package com.nexus.pms.repository;

import com.nexus.pms.model.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Customer entity.
 * Provides CRUD operations for customer/payee management.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by source system and source customer ID.
     */
    @Query("SELECT c FROM Customer c WHERE c.sourceSystem.clientMasterId = :clientMasterId AND c.sourceSystemId = :sourceId")
    Optional<Customer> findBySourceSystem(@Param("clientMasterId") Long clientMasterId,
            @Param("sourceId") Long sourceId);

    /**
     * Find customer by email.
     */
    Optional<Customer> findByCustomerEmail(String email);

    /**
     * Verify customer is active and not blocked.
     */
    @Query("SELECT c FROM Customer c WHERE c.customerId = :customerId AND c.isActive = true AND c.isBlocked = false")
    Optional<Customer> findActiveCustomer(@Param("customerId") Long customerId);
}
