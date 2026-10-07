package com.nexus.core.repository;

import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.enums.FleetAssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CapacityForecastRepo extends JpaRepository<CapacityForecast, Long> {

    @Query("SELECT c FROM CapacityForecast c WHERE c.forecastId = :id AND c.logisticsOrg.accountId = :orgId")
    Optional<CapacityForecast> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("""
            SELECT c FROM CapacityForecast c
            WHERE c.logisticsOrg.accountId = :orgId
            AND (:equipmentType IS NULL OR c.equipmentType = :equipmentType)
            AND (:search IS NULL OR LOWER(c.originLane) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.destinationLane) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<CapacityForecast> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("equipmentType") FleetAssetType equipmentType,
            @Param("search") String search,
            Pageable pageable);

    // Supplier logistics marketplace: all active routing capacities across
    // logistics orgs so suppliers can compare lanes/periods before proposing.
    @Query("""
            SELECT c FROM CapacityForecast c
            WHERE c.isActive = true
            AND (:search IS NULL OR LOWER(c.originLane) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(c.destinationLane) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<CapacityForecast> findMarketplaceAvailabilities(
            @Param("search") String search,
            Pageable pageable);
}
