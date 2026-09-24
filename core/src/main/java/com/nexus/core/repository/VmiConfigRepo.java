package com.nexus.core.repository;

import com.nexus.core.entities.VmiConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VmiConfigRepo extends JpaRepository<VmiConfig, Long> {

    Optional<VmiConfig> findByVmiIdAndSupplierOrgAccountId(Long vmiId, Long orgId);

    @Query("""
            SELECT v FROM VmiConfig v
            WHERE v.supplierOrg.accountId = :orgId
            AND (:retailerOrgId IS NULL OR v.retailerOrg.accountId = :retailerOrgId)
            AND (:warehouseId IS NULL OR v.warehouse.warehouseId = :warehouseId)
            AND (:materialId IS NULL OR v.material.materialId = :materialId)
            AND (:autoReplenish IS NULL OR v.autoReplenish = :autoReplenish)
            """)
    Page<VmiConfig> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("retailerOrgId") Long retailerOrgId,
            @Param("warehouseId") Long warehouseId,
            @Param("materialId") Long materialId,
            @Param("autoReplenish") Boolean autoReplenish,
            Pageable pageable);
}
