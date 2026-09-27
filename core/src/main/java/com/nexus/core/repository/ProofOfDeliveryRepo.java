package com.nexus.core.repository;

import com.nexus.core.entities.ProofOfDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProofOfDeliveryRepo extends JpaRepository<ProofOfDelivery, Long> {

    @Query("SELECT p FROM ProofOfDelivery p WHERE p.podId = :id AND p.shipment.logisticsOrg.accountId = :orgId")
    Optional<ProofOfDelivery> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("SELECT p FROM ProofOfDelivery p WHERE p.shipment.shipmentId = :shipmentId")
    Optional<ProofOfDelivery> findByShipmentId(@Param("shipmentId") Long shipmentId);

    @Query("""
            SELECT p FROM ProofOfDelivery p
            WHERE p.shipment.logisticsOrg.accountId = :orgId
            AND (:search IS NULL OR LOWER(p.receivedBy) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ProofOfDelivery> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("search") String search,
            Pageable pageable);
}
