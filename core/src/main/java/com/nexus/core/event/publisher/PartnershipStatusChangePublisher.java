package com.nexus.core.event.publisher;

import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.DomainEventType;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.enums.PartnershipStatus;
import com.nexus.core.security.OrganizationContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PartnershipStatusChangePublisher {

    private final DomainEventPublisher publisher;

    public PartnershipStatusChangePublisher(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Partnership partnership, PartnershipStatus previousStatus) {
        if (partnership == null || partnership.getPartnershipId() == null) {
            return;
        }

        Long actorOrgId = OrganizationContextHolder.getCurrentOrganizationId();
        String actorOrgType = OrganizationContextHolder.getCurrentOrganizationType();

        Long primaryOrgId = partnership.getPrimaryOrg() != null ? partnership.getPrimaryOrg().getAccountId() : null;
        Long secondaryOrgId = partnership.getSecondaryOrg() != null ? partnership.getSecondaryOrg().getAccountId() : null;

        Long targetOrgId;
        if (actorOrgId != null && actorOrgId.equals(primaryOrgId)) {
            targetOrgId = secondaryOrgId;
        } else {
            targetOrgId = primaryOrgId;
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", partnership.getStatus() != null ? partnership.getStatus().name() : null);
        if (partnership.getPartnershipType() != null) {
            facts.put("partnershipType", partnership.getPartnershipType());
        }
        if (partnership.getPrimaryOrgName() != null) {
            facts.put("primaryOrgName", partnership.getPrimaryOrgName());
        }
        if (partnership.getSecondaryOrgName() != null) {
            facts.put("secondaryOrgName", partnership.getSecondaryOrgName());
        }

        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.PARTNERSHIP_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                actorOrgId,
                actorOrgType,
                primaryOrgId,
                secondaryOrgId,
                targetOrgId,
                "PARTNERSHIP",
                partnership.getPartnershipId(),
                null,
                facts);

        publisher.publish(event);
    }

    public void publishMinimal(Partnership partnership, PartnershipStatus previousStatus) {
        if (partnership == null) return;
        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", partnership.getStatus() != null ? partnership.getStatus().name() : null);
        Long primaryOrgId = partnership.getPrimaryOrg() != null ? partnership.getPrimaryOrg().getAccountId() : null;
        Long secondaryOrgId = partnership.getSecondaryOrg() != null ? partnership.getSecondaryOrg().getAccountId() : null;
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.PARTNERSHIP_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                OrganizationContextHolder.getCurrentOrganizationId(),
                OrganizationContextHolder.getCurrentOrganizationType(),
                primaryOrgId,
                secondaryOrgId,
                primaryOrgId,
                "PARTNERSHIP",
                partnership.getPartnershipId(),
                null,
                facts);
        publisher.publish(event);
    }
}