package com.nexus.core.repository;

import com.nexus.core.model.enums.QuoteStatus;
import com.nexus.core.model.entities.ShipmentQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ShipmentQuoteRepo extends JpaRepository<ShipmentQuote, Long> {

    @Query("SELECT q FROM ShipmentQuote q WHERE q.quoteId = :id AND q.logisticsOrg.accountId = :orgId")
    Optional<ShipmentQuote> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("""
            SELECT q FROM ShipmentQuote q
            WHERE q.logisticsOrg.accountId = :orgId
            AND (:shipmentId IS NULL OR q.shipment.shipmentId = :shipmentId)
            AND (:status IS NULL OR q.status = :status)
            AND (:search IS NULL OR LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ShipmentQuote> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("shipmentId") Long shipmentId,
            @Param("status") QuoteStatus status,
            @Param("search") String search,
            Pageable pageable);
}
