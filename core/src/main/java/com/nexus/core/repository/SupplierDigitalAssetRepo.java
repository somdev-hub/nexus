package com.nexus.core.repository;

import com.nexus.core.entities.SupplierDigitalAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierDigitalAssetRepo extends JpaRepository<SupplierDigitalAsset, Long> {

    Optional<SupplierDigitalAsset> findByAssetIdAndCatalogSupplierOrgAccountId(Long assetId, Long orgId);

    @Query("""
            SELECT a FROM SupplierDigitalAsset a
            WHERE a.catalog.supplierOrg.accountId = :orgId
            AND (:catalogId IS NULL OR a.catalog.catalogId = :catalogId)
            AND (:assetType IS NULL OR a.assetType = :assetType)
            """)
    Page<SupplierDigitalAsset> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("catalogId") Long catalogId,
            @Param("assetType") SupplierDigitalAsset.AssetType assetType,
            Pageable pageable);
}
