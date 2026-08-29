package com.nexus.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nexus.core.entities.GoodsReceiptLineItem;

@Repository
public interface GoodsReceiptLineItemRepo extends JpaRepository<GoodsReceiptLineItem, Long> {

	Optional<GoodsReceiptLineItem> findByPoLineItemLineItemId(Long lineItemId);

}