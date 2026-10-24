package com.nexus.core.event.listener;

import com.nexus.core.event.publisher.PartnershipStatusChangePublisher;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.enums.PartnershipStatus;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PartnershipStatusEventListener {

    private final PartnershipStatusChangePublisher publisher;

    public PartnershipStatusEventListener(@Lazy PartnershipStatusChangePublisher publisher) {
        this.publisher = publisher;
    }

    @PostLoad
    public void onPostLoad(Partnership partnership) {
        partnership.setStatusBeforeChange(partnership.getStatus());
    }

    @PrePersist
    public void onPrePersist(Partnership partnership) {
        partnership.setStatusBeforeChange(null);
    }

    @PostUpdate
    public void onPostUpdate(Partnership partnership) {
        PartnershipStatus previous = partnership.getStatusBeforeChange();
        PartnershipStatus current = partnership.getStatus();
        if (previous == null || current == null || previous == current) {
            return;
        }
        try {
            publisher.publish(partnership, previous);
        } catch (Exception e) {
            log.error("Failed to publish a partnership status change event for partnership {} — writing a minimal fallback row",
                    partnership.getPartnershipId(), e);
            try {
                publisher.publishMinimal(partnership, previous);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for partnership {}", partnership.getPartnershipId(), fallbackError);
            }
        }
    }
}