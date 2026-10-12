package com.nexus.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexus.core.model.entities.AdvanceShipmentNotice;

public interface AdvanceShipmentNoticeRepo extends JpaRepository<AdvanceShipmentNotice, Long> {

    List<AdvanceShipmentNotice> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);

    // All ASNs touching this org (buyer, supplier, or partnership side),
    // newest first for the inbox view.
    @Query("""
            SELECT a FROM AdvanceShipmentNotice a
            LEFT JOIN a.purchaseOrder po
            LEFT JOIN po.partnership p
            WHERE a.buyerOrg.accountId = :orgId
               OR a.supplierOrg.accountId = :orgId
               OR p.primaryOrg.accountId = :orgId
               OR p.secondaryOrg.accountId = :orgId
            ORDER BY a.createdAt DESC
            """)
    Page<AdvanceShipmentNotice> findVisibleToOrg(@Param("orgId") Long orgId, Pageable pageable);
}
