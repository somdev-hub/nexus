package com.nexus.core.repository;

import com.nexus.core.entities.IncidentStatus;
import com.nexus.core.entities.IncidentType;
import com.nexus.core.entities.ShipmentIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentIncidentRepo extends JpaRepository<ShipmentIncident, Long> {

    @Query("SELECT i FROM ShipmentIncident i WHERE i.incidentId = :id AND i.shipment.logisticsOrg.accountId = :orgId")
    Optional<ShipmentIncident> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("""
            SELECT i FROM ShipmentIncident i
            WHERE i.shipment.logisticsOrg.accountId = :orgId
            AND (:shipmentId IS NULL OR i.shipment.shipmentId = :shipmentId)
            AND (:incidentType IS NULL OR i.incidentType = :incidentType)
            AND (:status IS NULL OR i.status = :status)
            AND (:search IS NULL OR LOWER(i.incidentNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ShipmentIncident> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("shipmentId") Long shipmentId,
            @Param("incidentType") IncidentType incidentType,
            @Param("status") IncidentStatus status,
            @Param("search") String search,
            Pageable pageable);
}
