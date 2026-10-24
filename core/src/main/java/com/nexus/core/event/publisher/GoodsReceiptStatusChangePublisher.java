package com.nexus.core.event.publisher;

import com.nexus.core.event.DomainEvent;
import com.nexus.core.event.DomainEventType;
import com.nexus.core.model.entities.GoodsReceipt;
import com.nexus.core.model.enums.GoodsReceiptStatus;
import com.nexus.core.security.OrganizationContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GoodsReceiptStatusChangePublisher {

    private final DomainEventPublisher publisher;

    public GoodsReceiptStatusChangePublisher(DomainEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(GoodsReceipt goodsReceipt, GoodsReceiptStatus previousStatus) {
        if (goodsReceipt == null || goodsReceipt.getGoodsReceiptId() == null) {
            return;
        }

        Long actorOrgId = OrganizationContextHolder.getCurrentOrganizationId();
        String actorOrgType = OrganizationContextHolder.getCurrentOrganizationType();

        // Buyer org from the purchase order
        Long buyerOrgId = null;
        if (goodsReceipt.getPurchaseOrder() != null && goodsReceipt.getPurchaseOrder().getBuyerOrg() != null) {
            buyerOrgId = goodsReceipt.getPurchaseOrder().getBuyerOrg().getAccountId();
        }

        // Supplier org from the supplier
        Long supplierOrgId = null;
        if (goodsReceipt.getSupplier() != null && goodsReceipt.getSupplier().getSupplierOrgAccountId() != null) {
            supplierOrgId = goodsReceipt.getSupplier().getSupplierOrgAccountId();
        }

        // Target is the counterparty of the actor
        Long targetOrgId;
        if (actorOrgId != null && actorOrgId.equals(buyerOrgId)) {
            targetOrgId = supplierOrgId;
        } else {
            targetOrgId = buyerOrgId;
        }

        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", goodsReceipt.getStatus() != null ? goodsReceipt.getStatus().name() : null);
        if (goodsReceipt.getGrNumber() != null) {
            facts.put("grNumber", goodsReceipt.getGrNumber());
        }

        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.GOODS_RECEIPT_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                actorOrgId,
                actorOrgType,
                buyerOrgId,
                supplierOrgId,
                targetOrgId,
                "GOODS_RECEIPT",
                goodsReceipt.getGoodsReceiptId(),
                goodsReceipt.getGrNumber(),
                facts);

        publisher.publish(event);
    }

    public void publishMinimal(GoodsReceipt goodsReceipt, GoodsReceiptStatus previousStatus) {
        if (goodsReceipt == null) return;
        Map<String, Object> facts = new HashMap<>();
        facts.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
        facts.put("newStatus", goodsReceipt.getStatus() != null ? goodsReceipt.getStatus().name() : null);
        if (goodsReceipt.getGrNumber() != null) {
            facts.put("grNumber", goodsReceipt.getGrNumber());
        }
        Long buyerOrgId = (goodsReceipt.getPurchaseOrder() != null && goodsReceipt.getPurchaseOrder().getBuyerOrg() != null)
                ? goodsReceipt.getPurchaseOrder().getBuyerOrg().getAccountId() : null;
        Long supplierOrgId = null;
        if (goodsReceipt.getSupplier() != null && goodsReceipt.getSupplier().getSupplierOrgAccountId() != null) {
            supplierOrgId = goodsReceipt.getSupplier().getSupplierOrgAccountId();
        }
        DomainEvent event = new DomainEvent(
                UUID.randomUUID().toString(),
                DomainEventType.GOODS_RECEIPT_STATUS_CHANGED,
                1,
                java.time.Instant.now().toString(),
                OrganizationContextHolder.getCurrentOrganizationId(),
                OrganizationContextHolder.getCurrentOrganizationType(),
                buyerOrgId,
                supplierOrgId,
                supplierOrgId,
                "GOODS_RECEIPT",
                goodsReceipt.getGoodsReceiptId(),
                goodsReceipt.getGrNumber(),
                facts);
        publisher.publish(event);
    }
}