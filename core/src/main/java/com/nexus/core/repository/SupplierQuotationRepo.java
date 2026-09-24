package com.nexus.core.repository;

import com.nexus.core.entities.SupplierQuotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.Optional;

@Repository
public interface SupplierQuotationRepo extends JpaRepository<SupplierQuotation, Long> {

    Optional<SupplierQuotation> findByQuotationIdAndSupplierOrgAccountId(Long quotationId, Long orgId);

    Optional<SupplierQuotation> findByQuotationNumberAndSupplierOrgAccountId(String quotationNumber, Long orgId);

    @Query("""
            SELECT q FROM SupplierQuotation q
            WHERE q.supplierOrg.accountId = :orgId
            AND (:status IS NULL OR q.status = :status)
            AND (:buyerOrgId IS NULL OR q.buyerOrg.accountId = :buyerOrgId)
            AND (:quotationNumber IS NULL OR q.quotationNumber = :quotationNumber)
            AND (CAST(:validFrom AS date) IS NULL OR q.validFrom >= :validFrom)
            AND (CAST(:validTo AS date) IS NULL OR q.validTo <= :validTo)
            """)
    Page<SupplierQuotation> findByOrgWithFilters(
            @Param("orgId") Long orgId,
            @Param("status") SupplierQuotation.QuotationStatus status,
            @Param("buyerOrgId") Long buyerOrgId,
            @Param("quotationNumber") String quotationNumber,
            @Param("validFrom") Date validFrom,
            @Param("validTo") Date validTo,
            Pageable pageable);
}
