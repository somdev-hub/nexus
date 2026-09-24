package com.nexus.core.repository;

import com.nexus.core.entities.CollaborativeForecast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.Optional;

@Repository
public interface CollaborativeForecastRepo extends JpaRepository<CollaborativeForecast, Long> {

    Optional<CollaborativeForecast> findByForecastIdAndSupplierOrgAccountId(Long forecastId, Long orgId);

    @Query("""
            SELECT f FROM CollaborativeForecast f
            WHERE f.supplierOrg.accountId = :orgId
            AND (:retailerOrgId IS NULL OR f.retailerOrg.accountId = :retailerOrgId)
            AND (:catalogId IS NULL OR f.catalog.catalogId = :catalogId)
            AND (:status IS NULL OR f.status = :status)
            AND (CAST(:periodStart AS date) IS NULL OR f.periodStart >= :periodStart)
            AND (CAST(:periodEnd AS date) IS NULL OR f.periodEnd <= :periodEnd)
            """)
    Page<CollaborativeForecast> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("retailerOrgId") Long retailerOrgId,
            @Param("catalogId") Long catalogId,
            @Param("status") CollaborativeForecast.ForecastStatus status,
            @Param("periodStart") Date periodStart,
            @Param("periodEnd") Date periodEnd,
            Pageable pageable);
}
