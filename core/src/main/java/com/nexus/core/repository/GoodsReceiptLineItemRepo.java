package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.nexus.core.model.entities.GoodsReceiptLineItem;
public interface GoodsReceiptLineItemRepo extends JpaRepository<GoodsReceiptLineItem, Long> {

	Optional<GoodsReceiptLineItem> findByPoLineItemLineItemId(Long lineItemId);

}