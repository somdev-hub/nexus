package com.nexus.core.repository;

import com.nexus.core.entities.FreightInvoice;
import com.nexus.core.entities.FreightInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreightInvoiceRepo extends JpaRepository<FreightInvoice, Long> {

    Optional<FreightInvoice> findByFreightInvoiceIdAndRetailerOrgAccountId(Long id, Long orgId);

    Optional<FreightInvoice> findByInvoiceNumberAndRetailerOrgAccountId(String invoiceNumber, Long orgId);

    @Query("""
            SELECT fi FROM FreightInvoice fi
            WHERE fi.retailerOrg.accountId = :orgId
            AND (:status IS NULL OR fi.status = :status)
            AND (:shipmentId IS NULL OR fi.shipment.shipmentId = :shipmentId)
            AND (:logisticsOrgId IS NULL OR fi.logisticsOrg.accountId = :logisticsOrgId)
            AND (CAST(:issuedStart AS date) IS NULL OR fi.issuedDate >= :issuedStart)
            AND (CAST(:issuedEnd AS date) IS NULL OR fi.issuedDate <= :issuedEnd)
            AND (CAST(:dueStart AS date) IS NULL OR fi.dueDate >= :dueStart)
            AND (CAST(:dueEnd AS date) IS NULL OR fi.dueDate <= :dueEnd)
            AND (:pmsStatus IS NULL OR fi.pmsStatus = :pmsStatus)
            """)
    Page<FreightInvoice> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") FreightInvoiceStatus status,
            @Param("shipmentId") Long shipmentId,
            @Param("logisticsOrgId") Long logisticsOrgId,
            @Param("issuedStart") java.sql.Date issuedStart,
            @Param("issuedEnd") java.sql.Date issuedEnd,
            @Param("dueStart") java.sql.Date dueStart,
            @Param("dueEnd") java.sql.Date dueEnd,
            @Param("pmsStatus") String pmsStatus,
            Pageable pageable);

    @Query("SELECT COUNT(fi) FROM FreightInvoice fi WHERE fi.retailerOrg.accountId = :orgId AND fi.status = :status")
    long countByOrgAndStatus(@Param("orgId") Long orgId, @Param("status") FreightInvoiceStatus status);

    @Query("SELECT SUM(fi.totalAmount) FROM FreightInvoice fi WHERE fi.retailerOrg.accountId = :orgId AND fi.status <> com.nexus.core.entities.FreightInvoiceStatus.CANCELLED")
    java.math.BigDecimal sumTotalByOrg(@Param("orgId") Long orgId);
}
