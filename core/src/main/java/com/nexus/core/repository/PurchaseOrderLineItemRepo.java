package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.PurchaseOrderLineItem;
public interface PurchaseOrderLineItemRepo extends JpaRepository<PurchaseOrderLineItem, Long> {

	List<PurchaseOrderLineItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);

	Optional<PurchaseOrderLineItem> findByLineItemIdAndPurchaseOrderPurchaseOrderId(Long lineItemId,
			Long purchaseOrderId);

	Optional<PurchaseOrderLineItem> findByLineItemId(Long lineItemId);

	Page<PurchaseOrderLineItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId, Pageable pageable);
}