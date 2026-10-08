package com.nexus.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nexus.core.model.entities.LogisticsPartnershipQuotation;

public interface LogisticsPartnershipQuotationRepo extends JpaRepository<LogisticsPartnershipQuotation, Long> {

    @Query("SELECT q FROM LogisticsPartnershipQuotation q WHERE q.supplierOrg.accountId = :orgId OR q.logisticsOrg.accountId = :orgId")
    Page<LogisticsPartnershipQuotation> findVisibleToOrg(@Param("orgId") Long orgId, Pageable pageable);

    List<LogisticsPartnershipQuotation> findByInvitationInvitationId(Long invitationId);

    List<LogisticsPartnershipQuotation> findByPartnershipPartnershipId(Long partnershipId);
}
