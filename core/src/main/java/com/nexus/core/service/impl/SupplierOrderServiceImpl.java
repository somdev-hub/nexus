package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.ShipmentStatus;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.SupplierOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierOrderServiceImpl implements SupplierOrderService {

    private final PurchaseOrderRepo poRepo;
    private final ShipmentRepo shipmentRepo;
    private final AccountRepository accountRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderById(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(id, orgId);
        return ResponseEntity.ok(modelMapper.map(po, com.nexus.core.payload.PurchaseOrderDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllOrders(String status, String poNumber, Long buyerOrgId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        // supplierOrg query - fallback to partnership or supplierOrg field
        List<PurchaseOrder> all = poRepo.findAll().stream()
                .filter(po -> isSupplierOrder(po, orgId))
                .filter(po -> status == null || po.getStatus().name().equalsIgnoreCase(status))
                .filter(po -> poNumber == null || (po.getPoNumber() != null && po.getPoNumber().toLowerCase().contains(poNumber.toLowerCase())))
                .filter(po -> buyerOrgId == null || (po.getBuyerOrg() != null && po.getBuyerOrg().getAccountId().equals(buyerOrgId)))
                .collect(Collectors.toList());

        // manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<com.nexus.core.payload.PurchaseOrderDto> dtos = all.subList(Math.min(start, all.size()), end).stream()
                .map(po -> modelMapper.map(po, com.nexus.core.payload.PurchaseOrderDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(dtos, pageable, all.size()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> acknowledgeOrder(Long id, Date confirmedDeliveryDate, String notes) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(id, orgId);
        if (po.getStatus() != PurchaseOrderStatus.SENT_TO_SUPPLIER) {
            throw new ValidationException("Only SENT_TO_SUPPLIER orders can be acknowledged. Current: " + po.getStatus());
        }
        po.setStatus(PurchaseOrderStatus.ACKNOWLEDGED);
        po.setAcknowledgedAt(Timestamp.valueOf(LocalDateTime.now()));
        po.setConfirmedDeliveryDate(confirmedDeliveryDate != null ? confirmedDeliveryDate : po.getExpectedDeliveryDate());
        po.setSupplierNotes(notes);
        po.setAcknowledgedBy(String.valueOf(orgId));
        PurchaseOrder saved = poRepo.save(po);
        return ResponseEntity.ok(modelMapper.map(saved, com.nexus.core.payload.PurchaseOrderDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateOrderFulfillment(Long id, Map<String, Object> updates) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(id, orgId);
        // consolidated update: confirmedDeliveryDate, supplierNotes, expectedDeliveryDate
        if (updates.containsKey("confirmedDeliveryDate") && updates.get("confirmedDeliveryDate") != null) {
            String dateStr = updates.get("confirmedDeliveryDate").toString();
            po.setConfirmedDeliveryDate(Date.valueOf(dateStr));
        }
        if (updates.containsKey("supplierNotes")) po.setSupplierNotes((String) updates.get("supplierNotes"));
        if (updates.containsKey("expectedDeliveryDate") && updates.get("expectedDeliveryDate") != null) {
            String dateStr = updates.get("expectedDeliveryDate").toString();
            po.setExpectedDeliveryDate(Date.valueOf(dateStr));
        }
        if (updates.containsKey("status")) {
            String s = updates.get("status").toString();
            try {
                PurchaseOrderStatus target = PurchaseOrderStatus.valueOf(s.toUpperCase());
                // allow supplier to move ACKNOWLEDGED -> PARTIALLY_RECEIVED / RECEIVED via fulfillment
                if ((target == PurchaseOrderStatus.PARTIALLY_RECEIVED || target == PurchaseOrderStatus.RECEIVED) && po.getStatus() == PurchaseOrderStatus.ACKNOWLEDGED) {
                    po.setStatus(target);
                } else {
                    throw new ValidationException("Supplier cannot transition to " + target);
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + s));
            }
        }
        PurchaseOrder saved = poRepo.save(po);
        return ResponseEntity.ok(modelMapper.map(saved, com.nexus.core.payload.PurchaseOrderDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createPartialShipment(Long purchaseOrderId, Map<String, Object> shipmentDto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(purchaseOrderId, orgId);
        if (po.getStatus() != PurchaseOrderStatus.ACKNOWLEDGED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ValidationException("Order must be ACKNOWLEDGED or PARTIALLY_RECEIVED to create partial shipment");
        }

        Account supplierOrg = accountRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));
        Account buyerOrg = po.getBuyerOrg();

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber("SHP-" + System.currentTimeMillis());
        shipment.setSupplierOrg(supplierOrg);
        shipment.setRetailerOrg(buyerOrg);
        shipment.setPurchaseOrder(po);
        shipment.setStatus(ShipmentStatus.DRAFT);
        shipment.setIsPartialShipment(true);

        // backordered calculation
        Double orderedQty = po.getLineItems().stream().mapToDouble(li -> li.getQuantityOrdered() != null ? li.getQuantityOrdered() : 0).sum();
        Double shippedQty = shipmentDto.containsKey("shippedQuantity") ? Double.valueOf(shipmentDto.get("shippedQuantity").toString()) : 0.0;
        double backordered = Math.max(0, orderedQty - shippedQty);
        shipment.setBackorderedQuantity(backordered);

        if (shipmentDto.containsKey("trackingNumber")) shipment.setTrackingNumber(shipmentDto.get("trackingNumber").toString());
        if (shipmentDto.containsKey("carrierName")) shipment.setCarrierName(shipmentDto.get("carrierName").toString());
        if (shipmentDto.containsKey("notes")) shipment.setNotes(shipmentDto.get("notes").toString());

        Shipment saved = shipmentRepo.save(shipment);

        // update PO status to PARTIALLY_RECEIVED if backordered >0 else RECEIVED
        if (backordered > 0) po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        else po.setStatus(PurchaseOrderStatus.RECEIVED);
        poRepo.save(po);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "shipmentId", saved.getShipmentId(),
                "shipmentNumber", saved.getShipmentNumber(),
                "purchaseOrderId", po.getPurchaseOrderId(),
                "backorderedQuantity", backordered,
                "status", saved.getStatus().name()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPartialShipments(Long purchaseOrderId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(purchaseOrderId, orgId);
        List<Shipment> shipments = shipmentRepo.findAll().stream()
                .filter(s -> s.getPurchaseOrder() != null && s.getPurchaseOrder().getPurchaseOrderId().equals(po.getPurchaseOrderId()))
                .filter(s -> Boolean.TRUE.equals(s.getIsPartialShipment()))
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), shipments.size());
        List<Shipment> pageContent = shipments.subList(Math.min(start, shipments.size()), end);
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(pageContent, pageable, shipments.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBackorderedOrders(Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<PurchaseOrder> backordered = poRepo.findAll().stream()
                .filter(po -> isSupplierOrder(po, orgId))
                .filter(po -> po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), backordered.size());
        List<com.nexus.core.payload.PurchaseOrderDto> dtos = backordered.subList(Math.min(start, backordered.size()), end).stream()
                .map(po -> modelMapper.map(po, com.nexus.core.payload.PurchaseOrderDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(dtos, pageable, backordered.size()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<PurchaseOrder> orders = poRepo.findAll().stream().filter(po -> isSupplierOrder(po, orgId)).collect(Collectors.toList());
        long total = orders.size();
        long pending = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.SENT_TO_SUPPLIER).count();
        long acknowledged = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.ACKNOWLEDGED).count();
        long partial = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED).count();
        long received = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.RECEIVED).count();
        return ResponseEntity.ok(Map.of("total", total, "pendingAck", pending, "acknowledged", acknowledged, "partiallyReceived", partial, "received", received));
    }

    private PurchaseOrder findSupplierOrder(Long id, Long orgId) {
        PurchaseOrder po = poRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));
        if (!isSupplierOrder(po, orgId)) {
            throw new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id);
        }
        return po;
    }

    private boolean isSupplierOrder(PurchaseOrder po, Long orgId) {
        if (po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getSecondaryOrg() != null && po.getPartnership().getSecondaryOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getPrimaryOrg() != null && po.getPartnership().getPrimaryOrg().getAccountId().equals(orgId)) return true;
        // fallback: if no supplierOrg mapping, allow supplier to see SENT_TO_SUPPLIER orders where buyer != supplier
        // For MVP, supplier can see orders where status is SENT_TO_SUPPLIER and not its own buyer org
        return po.getSupplierOrg() == null && po.getStatus() == PurchaseOrderStatus.SENT_TO_SUPPLIER;
    }
}
