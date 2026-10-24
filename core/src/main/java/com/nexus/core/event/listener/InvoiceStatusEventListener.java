package com.nexus.core.event.listener;

import com.nexus.core.event.publisher.InvoiceStatusChangePublisher;
import com.nexus.core.model.entities.Invoice;
import com.nexus.core.model.enums.InvoiceStatus;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InvoiceStatusEventListener {

    private final InvoiceStatusChangePublisher publisher;

    public InvoiceStatusEventListener(@Lazy InvoiceStatusChangePublisher publisher) {
        this.publisher = publisher;
    }

    @PostLoad
    public void onPostLoad(Invoice invoice) {
        invoice.setStatusBeforeChange(invoice.getStatus());
    }

    @PrePersist
    public void onPrePersist(Invoice invoice) {
        invoice.setStatusBeforeChange(null);
    }

    @PostUpdate
    public void onPostUpdate(Invoice invoice) {
        InvoiceStatus previous = invoice.getStatusBeforeChange();
        InvoiceStatus current = invoice.getStatus();
        if (previous == null || current == null || previous == current) {
            return;
        }
        try {
            publisher.publish(invoice, previous);
        } catch (Exception e) {
            log.error("Failed to publish an invoice status change event for invoice {} — writing a minimal fallback row",
                    invoice.getInvoiceId(), e);
            try {
                publisher.publishMinimal(invoice, previous);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for invoice {}", invoice.getInvoiceId(), fallbackError);
            }
        }
    }
}