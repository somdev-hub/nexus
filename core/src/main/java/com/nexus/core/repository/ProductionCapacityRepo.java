package com.nexus.core.repository;

import com.nexus.core.model.entities.ProductionCapacityCalendar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.sql.Date;
import java.util.Optional;
public interface ProductionCapacityRepo extends JpaRepository<ProductionCapacityCalendar, Long> {

    Optional<ProductionCapacityCalendar> findByCapacityIdAndSupplierOrgAccountId(Long capacityId, Long orgId);

    @Query("""
            SELECT pc FROM ProductionCapacityCalendar pc
            WHERE pc.supplierOrg.accountId = :orgId
            AND (:productLine IS NULL OR pc.productLine = :productLine)
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
}
