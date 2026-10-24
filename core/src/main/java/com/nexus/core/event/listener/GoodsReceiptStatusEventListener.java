package com.nexus.core.event.listener;

import com.nexus.core.event.publisher.GoodsReceiptStatusChangePublisher;
import com.nexus.core.model.entities.GoodsReceipt;
import com.nexus.core.model.enums.GoodsReceiptStatus;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GoodsReceiptStatusEventListener {

    private final GoodsReceiptStatusChangePublisher publisher;

    public GoodsReceiptStatusEventListener(@Lazy GoodsReceiptStatusChangePublisher publisher) {
        this.publisher = publisher;
    }

    @PostLoad
    public void onPostLoad(GoodsReceipt goodsReceipt) {
        goodsReceipt.setStatusBeforeChange(goodsReceipt.getStatus());
    }

    @PrePersist
    public void onPrePersist(GoodsReceipt goodsReceipt) {
        goodsReceipt.setStatusBeforeChange(null);
    }

    @PostUpdate
    public void onPostUpdate(GoodsReceipt goodsReceipt) {
        GoodsReceiptStatus previous = goodsReceipt.getStatusBeforeChange();
        GoodsReceiptStatus current = goodsReceipt.getStatus();
        if (previous == null || current == null || previous == current) {
            return;
        }
        try {
            publisher.publish(goodsReceipt, previous);
        } catch (Exception e) {
            log.error("Failed to publish a goods receipt status change event for GR {} — writing a minimal fallback row",
                    goodsReceipt.getGoodsReceiptId(), e);
            try {
                publisher.publishMinimal(goodsReceipt, previous);
            } catch (Exception fallbackError) {
                log.error("Could not write a fallback outbox row for GR {}", goodsReceipt.getGoodsReceiptId(), fallbackError);
            }
        }
    }
}