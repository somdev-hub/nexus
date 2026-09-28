package com.nexus.core.repository;

import com.nexus.core.model.entities.FleetAsset;
import com.nexus.core.model.enums.FleetAssetStatus;
import com.nexus.core.model.enums.FleetAssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface FleetAssetRepo extends JpaRepository<FleetAsset, Long> {

    Optional<FleetAsset> findByAssetIdAndLogisticsOrgAccountId(Long id, Long orgId);

    Optional<FleetAsset> findByAssetNumberAndLogisticsOrgAccountId(String assetNumber, Long orgId);

    @Query("""
            SELECT a FROM FleetAsset a
            WHERE a.logisticsOrg.accountId = :orgId
            AND (:status IS NULL OR a.status = :status)
            AND (:assetType IS NULL OR a.assetType = :assetType)
            AND (:search IS NULL OR LOWER(a.assetNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(a.licensePlate) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(a.make) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(a.model) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<FleetAsset> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") FleetAssetStatus status,
            @Param("assetType") FleetAssetType assetType,
            @Param("search") String search,
            Pageable pageable);
}
