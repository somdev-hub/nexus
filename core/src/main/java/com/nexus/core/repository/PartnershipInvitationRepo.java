package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.PartnershipInvitation;
import com.nexus.core.entities.PartnershipInvitationStatus;

@Repository
public interface PartnershipInvitationRepo extends JpaRepository<PartnershipInvitation, Long> {

	Optional<List<PartnershipInvitation>> findByInvitingOrgAccountId(Long orgId);

	Optional<List<PartnershipInvitation>> findByInvitedOrgAccountId(Long orgId);

	Optional<List<PartnershipInvitation>> findByInvitingOrgAccountIdAndStatus(Long orgId,
			PartnershipInvitationStatus status);

	Optional<List<PartnershipInvitation>> findByInvitedOrgAccountIdAndStatus(Long orgId,
			PartnershipInvitationStatus status);

	Optional<PartnershipInvitation> findByInvitationIdAndInvitingOrgAccountId(Long invitationId, Long orgId);

	Optional<PartnershipInvitation> findByInvitationIdAndInvitedOrgAccountId(Long invitationId, Long orgId);

	@Query("SELECT i FROM PartnershipInvitation i WHERE i.invitedOrg.accountId = :orgId AND i.status = 'PENDING' AND i.expiresAt > CURRENT_TIMESTAMP AND i.isActive = true")
	Optional<List<PartnershipInvitation>> findPendingInvitationsForOrg(@Param("orgId") Long orgId);
}