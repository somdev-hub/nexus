package com.nexus.core.repository;

import com.nexus.core.entities.SupplierPriceTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierPriceTierRepo extends JpaRepository<SupplierPriceTier, Long> {

    Optional<SupplierPriceTier> findByTierIdAndCatalogSupplierOrgAccountId(Long tierId, Long orgId);

    @Query("""
            SELECT t FROM SupplierPriceTier t
            WHERE t.catalog.supplierOrg.accountId = :orgId
            AND (:catalogId IS NULL OR t.catalog.catalogId = :catalogId)
            AND (:customerSegment IS NULL OR t.customerSegment = :customerSegment)
            AND (:contractId IS NULL OR t.contract.contractId = :contractId)
            AND (CAST(:validFrom AS date) IS NULL OR t.validFrom >= :validFrom)
            AND (CAST(:validTo AS date) IS NULL OR t.validTo <= :validTo)
            """)
    Page<SupplierPriceTier> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("catalogId") Long catalogId,
            @Param("customerSegment") String customerSegment,
            @Param("contractId") Long contractId,
            @Param("validFrom") java.sql.Date validFrom,
            @Param("validTo") java.sql.Date validTo,
            Pageable pageable);
}
