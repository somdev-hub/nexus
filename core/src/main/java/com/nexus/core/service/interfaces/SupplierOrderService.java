package com.nexus.core.service.interfaces;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface SupplierOrderService {

    ResponseEntity<?> getOrderById(Long id);

    ResponseEntity<?> getAllOrders(String status, String poNumber, Long buyerOrgId, Pageable pageable);

    ResponseEntity<?> acknowledgeOrder(Long id, java.sql.Date confirmedDeliveryDate, String notes);

    ResponseEntity<?> updateOrderFulfillment(Long id, Map<String, Object> updates);

    ResponseEntity<?> createPartialShipment(Long purchaseOrderId, Map<String, Object> shipmentDto);

    ResponseEntity<?> getPartialShipments(Long purchaseOrderId, Pageable pageable);

    ResponseEntity<?> getBackorderedOrders(Pageable pageable);

    ResponseEntity<?> getOrderSummary();

    // Goods receipts the retailer shared for validation/invoicing.
    ResponseEntity<?> getSupplierGoodsReceipts(String status, Pageable pageable);

    ResponseEntity<?> getSupplierGoodsReceipt(Long id);
}
