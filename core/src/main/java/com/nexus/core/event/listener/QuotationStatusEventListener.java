package com.nexus.core.event.listener;

import com.nexus.core.event.publisher.QuotationStatusChangePublisher;
import com.nexus.core.model.entities.SupplierQuotation;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QuotationStatusEventListener {

    private final QuotationStatusChangePublisher publisher;

    public QuotationStatusEventListener(@Lazy QuotationStatusChangePublisher publisher) {
        this.publisher = publisher;
    }

    @PostLoad
    public void onPostLoad(SupplierQuotation quotation) {
        quotation.setStatusBeforeChange(quotation.getStatus());
    }

    @PrePersist
    public void onPrePersist(SupplierQuotation quotation) {
        quotation.setStatusBeforeChange(null);
    }

    @PostUpdate
    public void onPostUpdate(SupplierQuotation quotation) {
        SupplierQuotation.QuotationStatus previous = quotation.getStatusBeforeChange();
        SupplierQuotation.QuotationStatus current = quotation.getStatus();
        if (previous == null || current == null || previous == current) {
            return;
        }
        try {
            publisher.publish(quotation, previous);
        } catch (Exception e) {
            log.error("Failed to publish a quotation status change event for quotation {} — writing a minimal fallback row",
                    quotation.getQuotationId(), e);
            try {
                publisher.publishMinimal(quotation, previous);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for quotation {}", quotation.getQuotationId(), fallbackError);
            }
        }
    }
}