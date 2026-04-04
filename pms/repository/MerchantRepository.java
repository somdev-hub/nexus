package com.nexus.pms.repository;

import com.nexus.pms.model.entities.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Merchant entity.
 * Provides CRUD operations for merchant management.
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    /**
     * Find merchant by email.
     */
    Optional<Merchant> findByEmail(String email);

    /**
     * Find merchant by source system and source merchant ID.
     */
    @Query("SELECT m FROM Merchant m WHERE m.sourceSystem.clientMasterId = :clientMasterId AND m.sourceSystemId = :sourceId")
    Optional<Merchant> findBySourceSystem(@Param("clientMasterId") Long clientMasterId,
            @Param("sourceId") Long sourceId);

    /**
     * Find merchant and initialize lazy relationships.
     */
    @Query("SELECT DISTINCT m FROM Merchant m LEFT JOIN FETCH m.merchantMembers WHERE m.merchantId = :merchantId")
    Optional<Merchant> findByIdWithMembers(@Param("merchantId") Long merchantId);
}
