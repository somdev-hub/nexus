package com.nexus.core.service.implementations;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.AdvanceShipmentNotice;
import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.PurchaseOrder;
import com.nexus.core.model.entities.PurchaseOrderLineItem;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.PartnershipStatus;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.AdvanceShipmentNoticeRepo;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.GoodsReceiptRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.SupplierOrderService;
import com.nexus.core.utils.RouteCapacityMatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierOrderServiceImpl implements SupplierOrderService {

    private final PurchaseOrderRepo poRepo;
    private final ShipmentRepo shipmentRepo;
    private final AdvanceShipmentNoticeRepo asnRepo;
    private final GoodsReceiptRepo goodsReceiptRepo;
    private final AccountRepository accountRepo;
    private final PartnershipRepo partnershipRepo;
    private final CapacityForecastRepo capacityForecastRepo;
    private final com.nexus.core.service.interfaces.CapacityAvailabilityService capacityAvailability;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderById(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(id, orgId);
        return ResponseEntity.ok(withShipmentProgress(PurchaseOrderServiceImpl.toDto(po), po));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllOrders(String status, String poNumber, Long buyerOrgId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrderStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = PurchaseOrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Preserve legacy behavior: unknown status matches nothing -> empty page (200).
                return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
            }
        }
        String poNum = poNumber;
        var page = poRepo.findSupplierVisibleOrders(orgId, st, poNum, buyerOrgId, pageable);
        var dtos = page.getContent().stream()
                .map(po -> withShipmentProgress(PurchaseOrderServiceImpl.toDto(po), po))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> acknowledgeOrder(Long id, Date confirmedDeliveryDate, String notes) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(id, orgId);
        if (po.getStatus() != PurchaseOrderStatus.SENT_TO_SUPPLIER) {
            throw new ValidationException("Only SENT_TO_SUPPLIER orders can be acknowledged. Current: " + po.getStatus());
        }
        // Gate: the order can only go forward when every catalog-backed line
        // is covered by the supplier's capacity calendar for the delivery
        // period. Capacity is reserved on success (released on cancel).
        java.sql.Date deliveryDate = confirmedDeliveryDate != null ? confirmedDeliveryDate
                : po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate()
                        : po.getRequestedDeliveryDate();
        reserveOrderCapacity(po, deliveryDate);
        po.setStatus(PurchaseOrderStatus.ACKNOWLEDGED);
        po.setAcknowledgedAt(Timestamp.valueOf(LocalDateTime.now()));
        po.setConfirmedDeliveryDate(deliveryDate);
        po.setSupplierNotes(notes);
        po.setAcknowledgedBy(String.valueOf(orgId));
        PurchaseOrder saved = poRepo.save(po);
        return ResponseEntity.ok(withShipmentProgress(PurchaseOrderServiceImpl.toDto(saved), saved));
    }

    /**
     * Reserve the order's catalog-backed lines against the supplier's
     * capacity calendar for the delivery date, aggregated per catalog item.
     * Capacity is committed the moment the order is acknowledged (and
     * released again if the order is later cancelled).
     */
    private void reserveOrderCapacity(PurchaseOrder po, java.sql.Date deliveryDate) {
        if (po.getLineItems() == null) {
            return;
        }
        // Group lines by catalog item to check the summed demand per item.
        Map<Long, Double> demandedByCatalog = new java.util.LinkedHashMap<>();
        for (PurchaseOrderLineItem line : po.getLineItems()) {
            if (line.getCatalogId() == null || line.getQuantityOrdered() == null || line.getQuantityOrdered() <= 0) {
                continue; // free-text lines are not capacity-gated
            }
            demandedByCatalog.merge(line.getCatalogId(), line.getQuantityOrdered(), Double::sum);
        }
        Long supplierOrgId = po.getSupplierOrg() != null ? po.getSupplierOrg().getAccountId() : null;
        if (supplierOrgId == null) {
            return; // legacy POs without a supplier org reference cannot be matched
        }
        for (Map.Entry<Long, Double> demand : demandedByCatalog.entrySet()) {
            capacityAvailability.reserveCapacity(
                    supplierOrgId,
                    "ORDER",
                    po.getPurchaseOrderId(),
                    demand.getKey(),
                    null, // PO lines carry no variant; unrestricted capacity rows apply
                    demand.getValue(),
                    deliveryDate,
                    deliveryDate,
                    "catalog item " + demand.getKey());
        }
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
        return ResponseEntity.ok(withShipmentProgress(PurchaseOrderServiceImpl.toDto(saved), saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createPartialShipment(Long purchaseOrderId, Map<String, Object> shipmentDto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(purchaseOrderId, orgId);
        if (po.getStatus() != PurchaseOrderStatus.ACKNOWLEDGED
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED
                && po.getStatus() != PurchaseOrderStatus.AWAITING_PICKUP
                && po.getStatus() != PurchaseOrderStatus.PICKED_UP) {
            throw new ValidationException("Order must be ACKNOWLEDGED, PARTIALLY_RECEIVED, AWAITING_PICKUP or PICKED_UP to create partial shipment");
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
        if (shippedQty != null && orderedQty != null && shippedQty > orderedQty) {
            throw new ValidationException("Shipped quantity (" + shippedQty
                    + ") cannot exceed the ordered quantity (" + orderedQty + ")");
        }
        double backordered = Math.max(0, orderedQty - shippedQty);
        shipment.setBackorderedQuantity(backordered);
        shipment.setShippedQuantity(shippedQty);

        if (shipmentDto.containsKey("trackingNumber")) shipment.setTrackingNumber(shipmentDto.get("trackingNumber").toString());
        if (shipmentDto.containsKey("carrierName")) shipment.setCarrierName(shipmentDto.get("carrierName").toString());
        if (shipmentDto.containsKey("notes")) shipment.setNotes(shipmentDto.get("notes").toString());
        if (shipmentDto.containsKey("pickupLocation")) shipment.setPickupLocation(shipmentDto.get("pickupLocation").toString());
        if (shipmentDto.containsKey("deliveryLocation")) shipment.setDeliveryLocation(shipmentDto.get("deliveryLocation").toString());
        // Freight costing captured at handover time (supplier-quoted).
        if (shipmentDto.get("freightCost") != null) shipment.setFreightCost(toDouble(shipmentDto.get("freightCost")));
        if (shipmentDto.get("currency") != null && !shipmentDto.get("currency").toString().isBlank()) {
            shipment.setCurrency(shipmentDto.get("currency").toString());
        }
        if (shipmentDto.containsKey("freightTerms") && shipmentDto.get("freightTerms") != null
                && !shipmentDto.get("freightTerms").toString().isBlank()) {
            shipment.setFreightTerms(shipmentDto.get("freightTerms").toString());
        }
        // Planned dates default sensibly when the caller omits them: pickup
        // today, delivery per the PO's expected (else requested) date.
        if (shipmentDto.get("pickupDate") != null && !shipmentDto.get("pickupDate").toString().isBlank()) {
            shipment.setPickupDate(Date.valueOf(shipmentDto.get("pickupDate").toString()));
        } else {
            shipment.setPickupDate(new Date(System.currentTimeMillis()));
        }
        if (shipmentDto.get("deliveryDate") != null && !shipmentDto.get("deliveryDate").toString().isBlank()) {
            shipment.setDeliveryDate(Date.valueOf(shipmentDto.get("deliveryDate").toString()));
        } else if (po.getExpectedDeliveryDate() != null) {
            shipment.setDeliveryDate(po.getExpectedDeliveryDate());
        } else if (po.getRequestedDeliveryDate() != null) {
            shipment.setDeliveryDate(po.getRequestedDeliveryDate());
        }

        // Package facts: totals derive from per-package values when the
        // caller leaves them empty (packages × weight, packages × L×W×H).
        Integer totalPackages = toInt(shipmentDto.get("totalPackages"));
        Double packageWeight = toDouble(shipmentDto.get("packageWeight"));
        Double packageLength = toDouble(shipmentDto.get("packageLength"));
        Double packageWidth = toDouble(shipmentDto.get("packageWidth"));
        Double packageHeight = toDouble(shipmentDto.get("packageHeight"));
        String packageDimensionUom = shipmentDto.get("packageDimensionUom") != null
                ? shipmentDto.get("packageDimensionUom").toString() : "M";
        if (totalPackages != null) shipment.setTotalPackages(totalPackages);
        if (packageWeight != null) shipment.setPackageWeight(packageWeight);
        if (packageLength != null) shipment.setPackageLength(packageLength);
        if (packageWidth != null) shipment.setPackageWidth(packageWidth);
        if (packageHeight != null) shipment.setPackageHeight(packageHeight);
        shipment.setPackageDimensionUom(packageDimensionUom);
        if (shipmentDto.get("totalWeight") != null) {
            shipment.setTotalWeight(toDouble(shipmentDto.get("totalWeight")));
        } else if (totalPackages != null && packageWeight != null) {
            shipment.setTotalWeight(Math.round(totalPackages * packageWeight * 100.0) / 100.0);
        }
        if (shipmentDto.get("totalVolume") != null) {
            shipment.setTotalVolume(toDouble(shipmentDto.get("totalVolume")));
        } else if (totalPackages != null && packageLength != null && packageWidth != null
                && packageHeight != null) {
            double toMeters = "FT".equalsIgnoreCase(packageDimensionUom) ? 0.3048 : 1.0;
            double cbm = totalPackages * packageLength * toMeters * packageWidth * toMeters
                    * packageHeight * toMeters;
            shipment.setTotalVolume(Math.round(cbm * 100.0) / 100.0);
        }

        // Supplier-owned delivery: hand the shipment to a partnered logistics
        // org at creation time. Requires an ACTIVE supplier-logistics
        // partnership; the shipment is then BOOKED immediately.
        boolean handedOver = false;
        Object partnershipIdRaw = shipmentDto.get("partnershipId");
        Object logisticsOrgIdRaw = shipmentDto.get("logisticsOrgId");
        boolean wantsHandover = (partnershipIdRaw != null && !partnershipIdRaw.toString().isBlank())
                || (logisticsOrgIdRaw != null && !logisticsOrgIdRaw.toString().isBlank());
        if (wantsHandover) {
            Partnership partnership = resolveLogisticsPartnership(orgId,
                    logisticsOrgIdRaw != null && !logisticsOrgIdRaw.toString().isBlank()
                            ? Long.valueOf(logisticsOrgIdRaw.toString())
                            : null,
                    partnershipIdRaw);
            // The logistics side is always the partnership counterparty, so a
            // caller can never hand a shipment over to its own org.
            Long logisticsOrgId = counterpartyOf(partnership, orgId);
            if (logisticsOrgId == null) {
                throw new ValidationException("Partnership does not involve your organization");
            }
            if (logisticsOrgIdRaw != null && !logisticsOrgIdRaw.toString().isBlank()) {
                Long requested = Long.valueOf(logisticsOrgIdRaw.toString());
                if (!requested.equals(logisticsOrgId) && !requested.equals(orgId)) {
                    throw new ValidationException("logisticsOrgId does not match the selected partnership");
                }
            }
            Account logisticsOrg = accountRepo.findById(logisticsOrgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", logisticsOrgId));
            shipment.setLogisticsOrg(logisticsOrg);
            shipment.setPartnership(partnership);
            shipment.setStatus(ShipmentStatus.BOOKED);
            // Persist the transport mode from the matched route capacity —
            // supplier shipments rarely carry one themselves and the load
            // board reads it from here first.
            CapacityForecast route = matchPartnershipRoute(partnership, logisticsOrgId,
                    shipment.getPickupLocation(), shipment.getDeliveryLocation());
            if (route != null && route.getTransportMode() != null) {
                shipment.setShipmentMode(route.getTransportMode());
            }
            handedOver = true;
        }

        Shipment saved = shipmentRepo.save(shipment);

        // counterparty-visible trade document: auto-create ASN row for this partial shipment
        AdvanceShipmentNotice asn = new AdvanceShipmentNotice();
        asn.setAsnNumber("ASN-" + System.currentTimeMillis());
        asn.setPurchaseOrder(po);
        asn.setBuyerOrg(buyerOrg);
        asn.setSupplierOrg(supplierOrg);
        asn.setShipmentId(saved.getShipmentId());
        asn.setStatus("SENT");
        if (shipmentDto.containsKey("notes") && shipmentDto.get("notes") != null) asn.setNotes(shipmentDto.get("notes").toString());
        asnRepo.save(asn);

        // PO follows the physical flow, never jumps to RECEIVED here:
        // handed-over shipments wait for pickup, and RECEIVED stays
        // reserved for the retailer's goods receipt.
        if (handedOver) {
            if (po.getStatus() == PurchaseOrderStatus.ACKNOWLEDGED
                    || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                po.setStatus(PurchaseOrderStatus.AWAITING_PICKUP);
            }
        } else if (backordered > 0) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        } else {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
        }
        poRepo.save(po);

        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("shipmentId", saved.getShipmentId());
        resp.put("shipmentNumber", saved.getShipmentNumber());
        resp.put("purchaseOrderId", po.getPurchaseOrderId());
        resp.put("backorderedQuantity", backordered);
        resp.put("status", saved.getStatus().name());
        resp.put("handedOverToLogistics", handedOver);
        resp.put("logisticsOrgId",
                saved.getLogisticsOrg() != null ? saved.getLogisticsOrg().getAccountId() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPartialShipments(Long purchaseOrderId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = findSupplierOrder(purchaseOrderId, orgId);
        var page = shipmentRepo.findByPurchaseOrderPurchaseOrderIdAndIsPartialShipmentTrue(po.getPurchaseOrderId(), pageable);
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(page.getContent(), pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBackorderedOrders(Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var page = poRepo.findSupplierVisibleOrders(orgId, PurchaseOrderStatus.PARTIALLY_RECEIVED, null, null, pageable);
        List<com.nexus.core.payload.PurchaseOrderDto> dtos = page.getContent().stream()
                .map(po -> withShipmentProgress(PurchaseOrderServiceImpl.toDto(po), po))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOrderSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<PurchaseOrder> orders = poRepo.findSupplierVisibleOrdersList(orgId);
        long total = orders.size();
        long pending = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.SENT_TO_SUPPLIER).count();
        long acknowledged = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.ACKNOWLEDGED).count();
        long partial = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED).count();
        long awaitingPickup = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.AWAITING_PICKUP).count();
        long pickedUp = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.PICKED_UP).count();
        long received = orders.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.RECEIVED).count();
        java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("total", total);
        summary.put("pendingAck", pending);
        summary.put("acknowledged", acknowledged);
        summary.put("partiallyReceived", partial);
        summary.put("awaitingPickup", awaitingPickup);
        summary.put("pickedUp", pickedUp);
        summary.put("received", received);
        return ResponseEntity.ok(summary);
    }

    private PurchaseOrder findSupplierOrder(Long id, Long orgId) {
        return poRepo.findSupplierVisibleOrderById(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", id));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSupplierGoodsReceipts(String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        com.nexus.core.model.enums.GoodsReceiptStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = com.nexus.core.model.enums.GoodsReceiptStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
            }
        }
        var page = goodsReceiptRepo.findSharedWithSupplierOrg(orgId, st, pageable);
        var dtos = page.getContent().stream()
                .map(GoodsReceiptServiceImpl::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(dtos, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSupplierGoodsReceipt(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        com.nexus.core.model.entities.GoodsReceipt gr =
                goodsReceiptRepo.findSharedByIdAndSupplierOrg(id, orgId);
        if (gr == null) {
            throw new ResourceNotFoundException("GoodsReceipt", "goodsReceiptId", id);
        }
        return ResponseEntity.ok(GoodsReceiptServiceImpl.toDto(gr));
    }

    /**
     * Shipment info for the supplier orders table, in one pass over the
     * PO's partial shipments: total shipped / remaining (drives the "hide
     * Partial Ship when fully shipped" rule) plus the latest dispatch refs
     * (powers the ASN/Shipment column + POD viewer without a second
     * round-trip).
     */
    private com.nexus.core.payload.PurchaseOrderDto withShipmentProgress(
            com.nexus.core.payload.PurchaseOrderDto dto, PurchaseOrder po) {
        double ordered = 0.0;
        if (po.getLineItems() != null) {
            ordered = po.getLineItems().stream()
                    .mapToDouble(li -> li.getQuantityOrdered() != null ? li.getQuantityOrdered() : 0)
                    .sum();
        }
        try {
            List<Shipment> shipments = shipmentRepo
                    .findByPurchaseOrderPurchaseOrderIdAndIsPartialShipmentTrue(
                            po.getPurchaseOrderId(), Pageable.unpaged())
                    .getContent();
            double shipped = shipments.stream()
                    .mapToDouble(s -> s.getShippedQuantity() != null ? s.getShippedQuantity() : 0)
                    .sum();
            dto.setTotalShippedQuantity(shipped);
            dto.setRemainingQuantity(Math.max(0, ordered - shipped));
            shipments.stream()
                    .filter(s -> s.getShipmentId() != null)
                    .max(java.util.Comparator.comparing(Shipment::getShipmentId))
                    .ifPresent(latest -> {
                        dto.setShipmentId(latest.getShipmentId());
                        dto.setShipmentNumber(latest.getShipmentNumber());
                    });
        } catch (Exception e) {
            log.warn("Skipping shipment progress for PO {}: {}", po.getPurchaseOrderId(), e.getMessage());
        }
        try {
            asnRepo.findByPurchaseOrderPurchaseOrderId(po.getPurchaseOrderId()).stream()
                    .filter(a -> a.getAsnId() != null)
                    .max(java.util.Comparator.comparing(
                            com.nexus.core.model.entities.AdvanceShipmentNotice::getAsnId))
                    .ifPresent(latest -> dto.setAsnNumber(latest.getAsnNumber()));
        } catch (Exception e) {
            log.warn("Skipping ASN ref for PO {}: {}", po.getPurchaseOrderId(), e.getMessage());
        }
        return dto;
    }

    private static Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInt(Object v) {
        Double d = toDouble(v);
        return d != null ? (int) Math.round(d) : null;
    }

    // Mode for a handed-over shipment, resolved from route capacities:
    // the linked route for short-term, the partnership's private lanes
    // for long-term, falling back to the logistics org's lanes.
    private CapacityForecast matchPartnershipRoute(Partnership partnership, Long logisticsOrgId,
            String pickup, String delivery) {
        if (partnership.getLinkedCapacityForecastId() != null) {
            CapacityForecast linked = capacityForecastRepo
                    .findById(partnership.getLinkedCapacityForecastId()).orElse(null);
            if (linked != null && linked.getTransportMode() != null
                    && RouteCapacityMatcher.routeCovers(linked, pickup, delivery)) {
                return linked;
            }
        }
        java.util.List<CapacityForecast> privates = capacityForecastRepo
                .findByPartnershipPartnershipId(partnership.getPartnershipId(),
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        CapacityForecast hit = RouteCapacityMatcher.matchWithMode(privates, pickup, delivery);
        if (hit != null) return hit;
        java.util.List<CapacityForecast> orgLanes = capacityForecastRepo
                .findByOrgWithFilters(logisticsOrgId, null, null,
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        hit = RouteCapacityMatcher.matchWithMode(orgLanes, pickup, delivery);
        if (hit != null) return hit;
        return RouteCapacityMatcher.matchWithMode(capacityForecastRepo
                .findMarketplaceAvailabilities(null,
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent(), pickup, delivery);
    }

    private Long counterpartyOf(Partnership p, Long ownOrgId) {        if (p.getPrimaryOrg() != null && !p.getPrimaryOrg().getAccountId().equals(ownOrgId)) {
            return p.getPrimaryOrg().getAccountId();
        }
        if (p.getSecondaryOrg() != null && !p.getSecondaryOrg().getAccountId().equals(ownOrgId)) {
            return p.getSecondaryOrg().getAccountId();
        }
        return null;
    }

    // Supplier-owned delivery: only an ACTIVE supplier-logistics partnership
    // permits handover of a shipment to the logistics org. When a
    // partnershipId is supplied, the logistics side is derived as the
    // partnership counterparty so callers cannot hand over to themselves.
    private Partnership resolveLogisticsPartnership(Long supplierOrgId, Long logisticsOrgId, Object partnershipIdRaw) {
        if (partnershipIdRaw != null && !partnershipIdRaw.toString().isBlank()) {
            Long pid = Long.valueOf(partnershipIdRaw.toString());
            Partnership p = partnershipRepo.findById(pid)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", pid));
            if (p.getStatus() != PartnershipStatus.ACTIVE) {
                throw new ValidationException("Partnership is not ACTIVE: " + p.getStatus());
            }
            boolean involvesSupplier = (p.getPrimaryOrg() != null && p.getPrimaryOrg().getAccountId().equals(supplierOrgId))
                    || (p.getSecondaryOrg() != null && p.getSecondaryOrg().getAccountId().equals(supplierOrgId));
            if (!involvesSupplier) {
                throw new ValidationException("Partnership does not involve your organization");
            }
            return p;
        }
        java.util.List<Partnership> mine = new java.util.ArrayList<>();
        mine.addAll(partnershipRepo.findByPrimaryOrgAccountId(supplierOrgId,
                org.springframework.data.domain.Pageable.ofSize(200)).getContent());
        mine.addAll(partnershipRepo.findBySecondaryOrgAccountId(supplierOrgId,
                org.springframework.data.domain.Pageable.ofSize(200)).getContent());
        for (Partnership p : mine) {
            if (p.getStatus() != PartnershipStatus.ACTIVE) continue;
            if (!"LOGISTICS".equals(p.getPartnershipType())) continue;
            Long counter = null;
            if (p.getPrimaryOrg() != null && !p.getPrimaryOrg().getAccountId().equals(supplierOrgId)) {
                counter = p.getPrimaryOrg().getAccountId();
            } else if (p.getSecondaryOrg() != null && !p.getSecondaryOrg().getAccountId().equals(supplierOrgId)) {
                counter = p.getSecondaryOrg().getAccountId();
            }
            if (logisticsOrgId.equals(counter)) return p;
        }
        throw new ValidationException("No ACTIVE logistics partnership with org " + logisticsOrgId
                + ". Send a proposal from the logistics marketplace first.");
    }

    // Kept for reference / fallback parity checks; live reads use the scoped
    // PurchaseOrderRepo.findSupplierVisible* JPQL above expressing the same predicate.
    private boolean isSupplierOrder(PurchaseOrder po, Long orgId) {
        if (po.getSupplierOrg() != null && po.getSupplierOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getSecondaryOrg() != null && po.getPartnership().getSecondaryOrg().getAccountId().equals(orgId)) return true;
        if (po.getPartnership() != null && po.getPartnership().getPrimaryOrg() != null && po.getPartnership().getPrimaryOrg().getAccountId().equals(orgId)) return true;
        // fallback: if no supplierOrg mapping, allow supplier to see SENT_TO_SUPPLIER orders where buyer != supplier
        // For MVP, supplier can see orders where status is SENT_TO_SUPPLIER and not its own buyer org
        return po.getSupplierOrg() == null && po.getStatus() == PurchaseOrderStatus.SENT_TO_SUPPLIER;
    }
}
