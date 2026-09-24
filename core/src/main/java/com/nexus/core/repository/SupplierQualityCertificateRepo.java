package com.nexus.core.repository;

import com.nexus.core.entities.SupplierQualityCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierQualityCertificateRepo extends JpaRepository<SupplierQualityCertificate, Long> {

    Optional<SupplierQualityCertificate> findByCertificateIdAndSupplierOrgAccountId(Long certificateId, Long orgId);

    @Query("""
            SELECT c FROM SupplierQualityCertificate c
            WHERE c.supplierOrg.accountId = :orgId
            AND (:purchaseOrderId IS NULL OR c.purchaseOrder.purchaseOrderId = :purchaseOrderId)
            AND (:shipmentId IS NULL OR c.shipment.shipmentId = :shipmentId)
            AND (:catalogId IS NULL OR c.catalog.catalogId = :catalogId)
            AND (:certificateType IS NULL OR c.certificateType = :certificateType)
            """)
    Page<SupplierQualityCertificate> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("shipmentId") Long shipmentId,
            @Param("catalogId") Long catalogId,
            @Param("certificateType") SupplierQualityCertificate.CertificateType certificateType,
            Pageable pageable);
}
