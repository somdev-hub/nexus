package com.nexus.core.repository;

import com.nexus.core.entities.DeliveryAppointment;
import com.nexus.core.entities.DeliveryAppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
public interface DeliveryAppointmentRepo extends JpaRepository<DeliveryAppointment, Long> {

    Optional<DeliveryAppointment> findByAppointmentIdAndRetailerOrgAccountId(Long id, Long orgId);

    @Query("""
            SELECT da FROM DeliveryAppointment da
            WHERE da.retailerOrg.accountId = :orgId
            AND (:status IS NULL OR da.status = :status)
            AND (:warehouseId IS NULL OR da.warehouse.warehouseId = :warehouseId)
            AND (:shipmentId IS NULL OR da.shipment.shipmentId = :shipmentId)
            AND (CAST(:fromDate AS timestamp) IS NULL OR da.scheduledStart >= :fromDate)
            AND (CAST(:toDate AS timestamp) IS NULL OR da.scheduledEnd <= :toDate)
            """)
    Page<DeliveryAppointment> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") DeliveryAppointmentStatus status,
            @Param("warehouseId") Long warehouseId,
            @Param("shipmentId") Long shipmentId,
            @Param("fromDate") Timestamp fromDate,
            @Param("toDate") Timestamp toDate,
            Pageable pageable);

    @Query("SELECT COUNT(da) FROM DeliveryAppointment da WHERE da.retailerOrg.accountId = :orgId AND da.status = :status")
    long countByOrgAndStatus(@Param("orgId") Long orgId, @Param("status") DeliveryAppointmentStatus status);
}
