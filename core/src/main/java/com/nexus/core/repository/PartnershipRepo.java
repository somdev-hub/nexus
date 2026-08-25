package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.Partnership;
import com.nexus.core.entities.PartnershipStatus;

@Repository
public interface PartnershipRepo extends JpaRepository<Partnership, Long> {

	Page<Partnership> findByPrimaryOrgAccountId(Long orgId, Pageable pageable);

	Optional<Partnership> findByPartnershipIdAndPrimaryOrgAccountId(Long partnershipId, Long orgId);

	// Find partnerships by status
	Page<Partnership> findByPrimaryOrgAccountIdAndStatus(Long orgId, PartnershipStatus status, Pageable pageable);

	// Find partnerships by secondary org (for supplier/logistics view)
	Page<Partnership> findBySecondaryOrgAccountId(Long orgId, Pageable pageable);

	Optional<Partnership> findByPartnershipIdAndSecondaryOrgAccountId(Long partnershipId, Long orgId);

	// Find active partnerships for a primary org
	@Query("SELECT p FROM Partnership p WHERE p.primaryOrg.accountId = :orgId AND p.status = 'ACTIVE' AND p.isActive = true")
	Page<Partnership> findActiveByPrimaryOrg(@Param("orgId") Long orgId, Pageable pageable);

	// Find partnerships by invitation ID
	Optional<Partnership> findByInvitationId(Long invitationId);

	// Find partnerships by agreement document ID
	Optional<Partnership> findByAgreementDocumentId(Long agreementDocumentId);
}