package com.nexus.core.event.publisher;

import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.DomainEventType;
import com.nexus.core.model.entities.Invoice;
import com.nexus.core.model.enums.InvoiceStatus;
import com.nexus.core.security.OrganizationContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoiceStatusChangePublisher {

    private final DomainEventPublisher publisher;

    public InvoiceStatusChangePublisher(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Invoice invoice, InvoiceStatus previousStatus) {
        if (invoice == null || invoice.getInvoiceId() == null) {
            return;
        }

        Long actorOrgId = OrganizationContextHolder.getCurrentOrganizationId();
        String actorOrgType = OrganizationContextHolder.getCurrentOrganizationType();

        Long buyerOrgId = (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getBuyerOrg() != null)
                ? invoice.getPurchaseOrder().getBuyerOrg().getAccountId() : null;
        Long supplierOrgId = null;
        if (invoice.getSupplier() != null && invoice.getSupplier().getSupplierOrgAccountId() != null) {
            supplierOrgId = invoice.getSupplier().getSupplierOrgAccountId();
        }

        Long targetOrgId;
        if (actorOrgId != null && actorOrgId.equals(buyerOrgId)) {
            targetOrgId = supplierOrgId;
        } else {
            targetOrgId = buyerOrgId;
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", invoice.getStatus() != null ? invoice.getStatus().name() : null);
        if (invoice.getInvoiceNumber() != null) {
            facts.put("invoiceNumber", invoice.getInvoiceNumber());
        }
        if (invoice.getTotalAmount() != null) {
            facts.put("totalAmount", invoice.getTotalAmount().toString());
        }

        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.INVOICE_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                actorOrgId,
                actorOrgType,
                buyerOrgId,
                supplierOrgId,
                targetOrgId,
                "INVOICE",
                invoice.getInvoiceId(),
                invoice.getInvoiceNumber(),
                facts);

        publisher.publish(event);
    }

    public void publishMinimal(Invoice invoice, InvoiceStatus previousStatus) {
        if (invoice == null) return;
        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", invoice.getStatus() != null ? invoice.getStatus().name() : null);
        if (invoice.getInvoiceNumber() != null) {
            facts.put("invoiceNumber", invoice.getInvoiceNumber());
        }
        Long buyerOrgId = (invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getBuyerOrg() != null)
                ? invoice.getPurchaseOrder().getBuyerOrg().getAccountId() : null;
        Long supplierOrgId = null;
        if (invoice.getSupplier() != null && invoice.getSupplier().getSupplierOrgAccountId() != null) {
            supplierOrgId = invoice.getSupplier().getSupplierOrgAccountId();
        }
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.INVOICE_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                OrganizationContextHolder.getCurrentOrganizationId(),
                OrganizationContextHolder.getCurrentOrganizationType(),
                buyerOrgId,
                supplierOrgId,
                buyerOrgId,
                "INVOICE",
                invoice.getInvoiceId(),
                invoice.getInvoiceNumber(),
                facts);
        publisher.publish(event);
    }
}