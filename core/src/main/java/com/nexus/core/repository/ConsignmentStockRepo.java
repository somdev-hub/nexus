package com.nexus.core.repository;

import com.nexus.core.entities.ConsignmentStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsignmentStockRepo extends JpaRepository<ConsignmentStock, Long> {

    Optional<ConsignmentStock> findByConsignmentIdAndSupplierOrgAccountId(Long consignmentId, Long orgId);

    @Query("""
            SELECT cs FROM ConsignmentStock cs
            WHERE cs.supplierOrg.accountId = :orgId
            AND (:retailerOrgId IS NULL OR cs.retailerOrg.accountId = :retailerOrgId)
            AND (:warehouseId IS NULL OR cs.warehouse.warehouseId = :warehouseId)
            AND (:materialId IS NULL OR cs.material.materialId = :materialId)
            """)
    Page<ConsignmentStock> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("retailerOrgId") Long retailerOrgId,
            @Param("warehouseId") Long warehouseId,
            @Param("materialId") Long materialId,
            Pageable pageable);
}
