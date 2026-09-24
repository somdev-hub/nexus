package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.SupplierOrderService;

import lombok.RequiredArgsConstructor;

import java.sql.Date;
import java.util.Map;

@RestController
@RequestMapping("/core/supplier/orders")
@RequiredArgsConstructor
public class SupplierOrderController {

    private final SupplierOrderService orderService;

    @GetMapping("/{id}")
    @LogActivity("Get Supplier Order")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/all")
    @LogActivity("Get All Supplier Orders")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String poNumber,
            @RequestParam(required = false) Long buyerOrgId,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.getAllOrders(status, poNumber, buyerOrgId, pageable);
    }

    @PutMapping("/{id}/acknowledge")
    @LogActivity("Acknowledge Supplier Order")
    public ResponseEntity<?> acknowledgeOrder(@PathVariable Long id,
            @RequestParam(required = false) Date confirmedDeliveryDate,
            @RequestParam(required = false) String notes) {
        return orderService.acknowledgeOrder(id, confirmedDeliveryDate, notes);
    }

    @PutMapping("/{id}/fulfillment")
    @LogActivity("Update Supplier Order Fulfillment")
    public ResponseEntity<?> updateFulfillment(@PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return orderService.updateOrderFulfillment(id, updates);
    }

    @PostMapping("/{purchaseOrderId}/partial-shipment")
    @LogActivity("Create Partial Shipment")
    public ResponseEntity<?> createPartialShipment(@PathVariable Long purchaseOrderId,
            @RequestBody Map<String, Object> shipmentDto) {
        return orderService.createPartialShipment(purchaseOrderId, shipmentDto);
    }

    @GetMapping("/{purchaseOrderId}/partial-shipments")
    @LogActivity("Get Partial Shipments")
    public ResponseEntity<?> getPartialShipments(@PathVariable Long purchaseOrderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.getPartialShipments(purchaseOrderId, pageable);
    }

    @GetMapping("/backordered")
    @LogActivity("Get Backordered Orders")
    public ResponseEntity<?> getBackorderedOrders(@PageableDefault(size = 20) Pageable pageable) {
        return orderService.getBackorderedOrders(pageable);
    }

    @GetMapping("/summary")
    @LogActivity("Get Supplier Order Summary")
    public ResponseEntity<?> getSummary() {
        return orderService.getOrderSummary();
    }
}
