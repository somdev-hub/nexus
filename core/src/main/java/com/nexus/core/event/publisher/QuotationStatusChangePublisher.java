package com.nexus.core.event.publisher;

import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.DomainEventType;
import com.nexus.core.model.entities.SupplierQuotation;
import com.nexus.core.security.OrganizationContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class QuotationStatusChangePublisher {

    private final DomainEventPublisher publisher;

    public QuotationStatusChangePublisher(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(SupplierQuotation quotation, SupplierQuotation.QuotationStatus previousStatus) {
        if (quotation == null || quotation.getQuotationId() == null) {
            return;
        }

        Long actorOrgId = OrganizationContextHolder.getCurrentOrganizationId();
        String actorOrgType = OrganizationContextHolder.getCurrentOrganizationType();

        Long buyerOrgId = quotation.getBuyerOrg() != null ? quotation.getBuyerOrg().getAccountId() : null;
        Long supplierOrgId = quotation.getSupplierOrg() != null ? quotation.getSupplierOrg().getAccountId() : null;

        Long targetOrgId;
        if (actorOrgId != null && actorOrgId.equals(buyerOrgId)) {
            targetOrgId = supplierOrgId;
        } else {
            targetOrgId = buyerOrgId;
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", quotation.getStatus() != null ? quotation.getStatus().name() : null);
        if (quotation.getQuotationNumber() != null) {
            facts.put("quotationNumber", quotation.getQuotationNumber());
        }

        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.QUOTATION_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                actorOrgId,
                actorOrgType,
                buyerOrgId,
                supplierOrgId,
                targetOrgId,
                "SUPPLIER_QUOTATION",
                quotation.getQuotationId(),
                quotation.getQuotationNumber(),
                facts);

        publisher.publish(event);
    }

    public void publishMinimal(SupplierQuotation quotation, SupplierQuotation.QuotationStatus previousStatus) {
        if (quotation == null) return;
        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", quotation.getStatus() != null ? quotation.getStatus().name() : null);
        if (quotation.getQuotationNumber() != null) {
            facts.put("quotationNumber", quotation.getQuotationNumber());
        }
        Long buyerOrgId = quotation.getBuyerOrg() != null ? quotation.getBuyerOrg().getAccountId() : null;
        Long supplierOrgId = quotation.getSupplierOrg() != null ? quotation.getSupplierOrg().getAccountId() : null;
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.QUOTATION_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                OrganizationContextHolder.getCurrentOrganizationId(),
                OrganizationContextHolder.getCurrentOrganizationType(),
                buyerOrgId,
                supplierOrgId,
                buyerOrgId,
                "SUPPLIER_QUOTATION",
                quotation.getQuotationId(),
                quotation.getQuotationNumber(),
                facts);
        publisher.publish(event);
    }
}