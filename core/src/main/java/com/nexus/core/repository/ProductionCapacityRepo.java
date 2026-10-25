package com.nexus.core.repository;

import com.nexus.core.model.entities.ProductionCapacityCalendar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
public interface ProductionCapacityRepo extends JpaRepository<ProductionCapacityCalendar, Long> {

    Optional<ProductionCapacityCalendar> findByCapacityIdAndSupplierOrgAccountId(Long capacityId, Long orgId);

    @Query("""
            SELECT pc FROM ProductionCapacityCalendar pc
            WHERE pc.supplierOrg.accountId = :orgId
            AND (
                :productLine IS NULL
                OR pc.productLine = :productLine
                OR pc.catalog.category = :productLine
            )
            AND (:shift IS NULL OR pc.shift = :shift)
            AND (CAST(:periodStart AS date) IS NULL OR pc.periodStart >= :periodStart)
            AND (CAST(:periodEnd AS date) IS NULL OR pc.periodEnd <= :periodEnd)
            """)
    Page<ProductionCapacityCalendar> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("productLine") String productLine,
            @Param("shift") String shift,
            @Param("periodStart") Date periodStart,
            @Param("periodEnd") Date periodEnd,
            Pageable pageable);

    /**
     * Capacity rows of an org matching a specific catalog variant restriction
     * (rows with no variant apply to all variants of the item) and overlapping
     * the window, which still have slack, in chronological order. All
     * parameters must be non-null — callers without a variant restriction use
     * {@link #findMatchedCapacityAllVariants}, and callers without a window
     * pass sentinel bounds (see CapacityAvailabilityServiceImpl).
     */
    @Query("""
            SELECT pc FROM ProductionCapacityCalendar pc
            WHERE pc.supplierOrg.accountId = :orgId
            AND pc.catalog.catalogId = :catalogId
            AND (pc.variantId IS NULL OR pc.variantId = :variantId)
            AND pc.periodEnd >= :periodStart
            AND pc.periodStart <= :periodEnd
            AND (pc.availableCapacity - COALESCE(pc.allocatedCapacity, 0)) > 0
            ORDER BY pc.periodStart ASC
            """)
    List<ProductionCapacityCalendar> findMatchedCapacity(
            @Param("orgId") Long orgId,
            @Param("catalogId") Long catalogId,
            @Param("variantId") Long variantId,
            @Param("periodStart") Date periodStart,
            @Param("periodEnd") Date periodEnd);

    /**
     * Same as {@link #findMatchedCapacity} but with no variant restriction:
     * every capacity row of the catalog item matches. Used when the caller has
     * not narrowed the request to a particular variant. All parameters must be
     * non-null.
     */
    @Query("""
            SELECT pc FROM ProductionCapacityCalendar pc
            WHERE pc.supplierOrg.accountId = :orgId
            AND pc.catalog.catalogId = :catalogId
            AND pc.periodEnd >= :periodStart
            AND pc.periodStart <= :periodEnd
            AND (pc.availableCapacity - COALESCE(pc.allocatedCapacity, 0)) > 0
            ORDER BY pc.periodStart ASC
            """)
    List<ProductionCapacityCalendar> findMatchedCapacityAllVariants(
            @Param("orgId") Long orgId,
            @Param("catalogId") Long catalogId,
            @Param("periodStart") Date periodStart,
            @Param("periodEnd") Date periodEnd);
}
