package com.nexus.core.repository;

import com.nexus.core.model.entities.MaintenanceRecord;
import com.nexus.core.model.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface MaintenanceRecordRepo extends JpaRepository<MaintenanceRecord, Long> {

    @Query("""
            SELECT m FROM MaintenanceRecord m
            WHERE m.asset.logisticsOrg.accountId = :orgId
            AND (:assetId IS NULL OR m.asset.assetId = :assetId)
            AND (:status IS NULL OR m.status = :status)
            AND (:isBreakdown IS NULL OR m.isBreakdown = :isBreakdown)
            AND (:search IS NULL OR LOWER(m.maintenanceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(m.maintenanceType) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<MaintenanceRecord> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("assetId") Long assetId,
            @Param("status") MaintenanceStatus status,
            @Param("isBreakdown") Boolean isBreakdown,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.maintenanceId = :id AND m.asset.logisticsOrg.accountId = :orgId")
    Optional<MaintenanceRecord> findByIdAndOrg(@Param("id") Long id, @Param("orgId") Long orgId);
}
