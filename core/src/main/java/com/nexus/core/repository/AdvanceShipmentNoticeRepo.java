package com.nexus.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nexus.core.model.entities.AdvanceShipmentNotice;

public interface AdvanceShipmentNoticeRepo extends JpaRepository<AdvanceShipmentNotice, Long> {

    List<AdvanceShipmentNotice> findByPurchaseOrderPurchaseOrderId(Long purchaseOrderId);
}
