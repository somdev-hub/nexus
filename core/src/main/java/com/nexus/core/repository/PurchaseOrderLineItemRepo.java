package com.nexus.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.PurchaseOrderLineItem;

@Repository
public interface PurchaseOrderLineItemRepo extends JpaRepository<PurchaseOrderLineItem, Long> {

	List<PurchaseOrderLineItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);

	Optional<PurchaseOrderLineItem> findByLineItemIdAndPurchaseOrderPurchaseOrderId(Long lineItemId,
			Long purchaseOrderId);

	Optional<PurchaseOrderLineItem> findByLineItemId(Long lineItemId);

	Page<PurchaseOrderLineItem> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId, Pageable pageable);
}