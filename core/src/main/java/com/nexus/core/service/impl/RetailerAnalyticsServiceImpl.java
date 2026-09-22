package com.nexus.core.service.impl;

import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.PurchaseOrderStatus;
import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.ShipmentStatus;
import com.nexus.core.payload.RetailerAnalyticsDto;
import com.nexus.core.repository.*;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.RetailerAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetailerAnalyticsServiceImpl implements RetailerAnalyticsService {

    private final PurchaseOrderRepo purchaseOrderRepo;
    private final ShipmentRepo shipmentRepo;
    private final StockRepo stockRepo;
    private final SupplierPerformanceRepo supplierPerformanceRepo;
    private final PartnershipRepo partnershipRepo;
    private final GoodsReceiptRepo goodsReceiptRepo;
    private final InvoiceRepo invoiceRepo;
    private final FreightInvoiceRepo freightInvoiceRepo;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getExecutiveDashboard() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();

        // Spend: sum of totalAmount for all POs (non-cancelled)
        var allPos = purchaseOrderRepo.findByBuyerOrgAccountId(orgId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        // Fallback if paged query not optimal – fetch via list method
        if (allPos.isEmpty()) {
            // Try alternative: find by orgIdAndStatus not cancelled
        }
        BigDecimal totalSpend = allPos.stream()
                .filter(po -> po.getStatus() != PurchaseOrderStatus.CANCELLED)
                .map(po -> po.getTotalAmount() != null ? BigDecimal.valueOf(po.getTotalAmount()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long openPoCount = allPos.stream()
                .filter(po -> Set.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.PENDING_APPROVAL, PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.SENT_TO_SUPPLIER).contains(po.getStatus()))
                .count();

        // Inbound shipments: shipments for retailer org not in terminal states
        // Use repo method for active shipments – count manually from all
        // We do not have org-scoped findAll, so filter by retailerOrg
        // Simplistic: use shipmentRepo.findAll then filter (acceptable for hobby project)
        var allShipments = shipmentRepo.findAll().stream()
                .filter(s -> s.getRetailerOrg() != null && s.getRetailerOrg().getAccountId().equals(orgId))
                .toList();
        long inboundCount = allShipments.stream()
                .filter(s -> !Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED, ShipmentStatus.CLOSED).contains(s.getStatus()))
                .count();

        // OTIF: delivered on time vs total delivered
        long delivered = allShipments.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count();
        long onTime = allShipments.stream()
                .filter(s -> s.getStatus() == ShipmentStatus.DELIVERED && s.getActualArrival() != null && s.getEstimatedArrival() != null && !s.getActualArrival().after(s.getEstimatedArrival()))
                .count();
        double otif = delivered > 0 ? (onTime * 100.0 / delivered) : 0;

        // Supplier performance avg
        var perfList = supplierPerformanceRepo.findAll().stream()
                .filter(p -> p.getAccount() != null && p.getAccount().getAccountId().equals(orgId))
                .toList();
        double avgPerf = perfList.stream()
                .mapToDouble(p -> p.getOverallScore() != null ? p.getOverallScore().doubleValue() : 0)
                .average().orElse(0);

        long totalSuppliers = perfList.stream().map(p -> p.getSupplier() != null ? p.getSupplier().getSupplierId() : null).filter(Objects::nonNull).distinct().count();
        long activePartnerships = partnershipRepo.findAll().stream()
                .filter(pt -> pt.getPrimaryOrg() != null && pt.getPrimaryOrg().getAccountId().equals(orgId) && pt.getStatus() != null && pt.getStatus().name().equals("ACTIVE"))
                .count();

        Double invVal = stockRepo.getTotalInventoryValue(orgId);
        double inventoryValue = invVal != null ? invVal : 0;

        var dashboard = RetailerAnalyticsDto.DashboardDto.builder()
                .totalSpend(totalSpend)
                .openPoCount(openPoCount)
                .inboundShipmentCount(inboundCount)
                .otifPercentage(Math.round(otif * 100.0) / 100.0)
                .avgSupplierPerformance(Math.round(avgPerf * 100.0) / 100.0)
                .totalSuppliers(totalSuppliers)
                .activePartnerships(activePartnerships)
                .inventoryValue(inventoryValue)
                .spendTrend(buildSpendTrend(allPos))
                .bottleneckAlerts(buildBottlenecks(allPos, allShipments))
                .build();
        return ResponseEntity.ok(dashboard);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSpendAnalytics(String groupBy, String period) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var allPos = purchaseOrderRepo.findByBuyerOrgAccountId(orgId, org.springframework.data.domain.Pageable.unpaged()).getContent()
                .stream().filter(po -> po.getStatus() != PurchaseOrderStatus.CANCELLED).toList();

        Map<String, BigDecimal> bySupplier = allPos.stream()
                .collect(Collectors.groupingBy(
                        po -> po.getSupplier() != null ? po.getSupplier().getBusinessName() : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, po -> po.getTotalAmount() != null ? BigDecimal.valueOf(po.getTotalAmount()) : BigDecimal.ZERO, BigDecimal::add)
                ));

        Map<String, BigDecimal> byCategory = allPos.stream()
                .collect(Collectors.groupingBy(
                        po -> {
                            if (po.getSupplier() != null && po.getSupplier().getCategory() != null) return po.getSupplier().getCategory();
                            return "Uncategorized";
                        },
                        Collectors.reducing(BigDecimal.ZERO, po -> po.getTotalAmount() != null ? BigDecimal.valueOf(po.getTotalAmount()) : BigDecimal.ZERO, BigDecimal::add)
                ));

        Map<String, BigDecimal> byMonth = allPos.stream()
                .collect(Collectors.groupingBy(
                        po -> po.getCreatedAt() != null ? String.format("%tY-%tm", po.getCreatedAt(), po.getCreatedAt()) : "Unknown",
                        Collectors.reducing(BigDecimal.ZERO, po -> po.getTotalAmount() != null ? BigDecimal.valueOf(po.getTotalAmount()) : BigDecimal.ZERO, BigDecimal::add)
                ));

        BigDecimal total = bySupplier.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = allPos.isEmpty() ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(allPos.size()), 2, java.math.RoundingMode.HALF_UP);

        var dto = RetailerAnalyticsDto.SpendAnalyticsDto.builder()
                .spendBySupplier(bySupplier)
                .spendByCategory(byCategory)
                .spendByMonth(byMonth)
                .totalSpend(total)
                .avgOrderValue(avg)
                .totalOrders(allPos.size())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSupplyChainVisibility(Long purchaseOrderId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgAccountId(purchaseOrderId, orgId)
                .orElseThrow(() -> new com.nexus.core.exception.ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", purchaseOrderId));

        // Shipment linked via stops referenceType PURCHASE_ORDER
        var shipments = shipmentRepo.findByPurchaseOrderId(purchaseOrderId);
        Shipment shipment = shipments.isEmpty() ? null : shipments.get(0);

        var visibility = RetailerAnalyticsDto.SupplyChainVisibilityDto.builder()
                .purchaseOrderNumber(po.getPoNumber())
                .poStatus(po.getStatus() != null ? po.getStatus().name() : "UNKNOWN")
                .shipmentNumber(shipment != null ? shipment.getShipmentNumber() : null)
                .shipmentStatus(shipment != null && shipment.getStatus() != null ? shipment.getStatus().name() : null)
                .trackingTimeline(shipment != null ? buildTrackingTimeline(shipment) : List.of())
                .milestones(buildMilestones(po, shipment))
                .bottlenecks(buildBottlenecksForPo(po, shipment))
                .totalLeadTimeDays(calculateLeadTime(po, shipment))
                .build();

        // Goods receipt / invoice if exists
        var grsOpt = goodsReceiptRepo.findByPurchaseOrderPurchaseOrderId(purchaseOrderId);
        if (grsOpt.isPresent() && !grsOpt.get().isEmpty()) visibility.setGoodsReceiptNumber(grsOpt.get().get(0).getGrNumber());
        var invsOpt = invoiceRepo.findByPurchaseOrderPurchaseOrderId(purchaseOrderId);
        if (invsOpt.isPresent() && !invsOpt.get().isEmpty()) {
            visibility.setInvoiceNumber(invsOpt.get().get(0).getInvoiceNumber());
            visibility.setInvoiceStatus(invsOpt.get().get(0).getStatus() != null ? invsOpt.get().get(0).getStatus().name() : null);
        }
        if (shipment != null) {
            var freight = freightInvoiceRepo.findAll().stream()
                    .filter(fi -> fi.getShipment() != null && fi.getShipment().getShipmentId().equals(shipment.getShipmentId()))
                    .findFirst().orElse(null);
            if (freight != null) visibility.setFreightInvoiceNumber(freight.getInvoiceNumber());
        }
        return ResponseEntity.ok(visibility);
    }

    @Override
    public ResponseEntity<?> getSpendBySupplier() {
        return getSpendAnalytics("supplier", null);
    }

    @Override
    public ResponseEntity<?> getSpendByCategory() {
        return getSpendAnalytics("category", null);
    }

    private List<Map<String, Object>> buildSpendTrend(List<PurchaseOrder> pos) {
        Map<String, BigDecimal> byMonth = pos.stream()
                .filter(po -> po.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        po -> String.format("%tY-%tm", po.getCreatedAt(), po.getCreatedAt()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, po -> po.getTotalAmount() != null ? BigDecimal.valueOf(po.getTotalAmount()) : BigDecimal.ZERO, BigDecimal::add)
                ));
        return byMonth.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("period", e.getKey());
                    m.put("spend", e.getValue());
                    return m;
                }).toList();
    }

    private List<Map<String, Object>> buildBottlenecks(List<PurchaseOrder> pos, List<Shipment> shipments) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        long pendingApproval = pos.stream().filter(po -> po.getStatus() == PurchaseOrderStatus.PENDING_APPROVAL).count();
        if (pendingApproval > 5) alerts.add(Map.of("type", "APPROVAL_BACKLOG", "count", pendingApproval, "severity", "HIGH"));
        long exceptionShipments = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.EXCEPTION).count();
        if (exceptionShipments > 0) alerts.add(Map.of("type", "SHIPMENT_EXCEPTIONS", "count", exceptionShipments, "severity", "MEDIUM"));
        long overdue = shipments.stream().filter(s -> s.getEstimatedArrival() != null && s.getEstimatedArrival().before(new java.sql.Timestamp(System.currentTimeMillis())) && s.getStatus() != ShipmentStatus.DELIVERED).count();
        if (overdue > 0) alerts.add(Map.of("type", "OVERDUE_SHIPMENTS", "count", overdue, "severity", "HIGH"));
        return alerts;
    }

    private List<Map<String, Object>> buildTrackingTimeline(Shipment shipment) {
        if (shipment.getTrackingEvents() == null) return List.of();
        return shipment.getTrackingEvents().stream()
                .sorted(Comparator.comparing(e -> e.getEventTimestamp()))
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("eventType", e.getEventType() != null ? e.getEventType().name() : "UNKNOWN");
                    m.put("timestamp", e.getEventTimestamp());
                    m.put("location", e.getLocation());
                    m.put("description", e.getDescription());
                    return m;
                }).toList();
    }

    private List<Map<String, Object>> buildMilestones(PurchaseOrder po, Shipment shipment) {
        List<Map<String, Object>> milestones = new ArrayList<>();
        milestones.add(Map.of("stage", "PO_CREATED", "timestamp", po.getCreatedAt() != null ? po.getCreatedAt() : "", "status", po.getStatus() != null ? po.getStatus().name() : ""));
        if (shipment != null) {
            milestones.add(Map.of("stage", "SHIPMENT", "timestamp", shipment.getCreatedAt() != null ? shipment.getCreatedAt() : "", "status", shipment.getStatus() != null ? shipment.getStatus().name() : ""));
            if (shipment.getActualDeparture() != null) milestones.add(Map.of("stage", "DEPARTED", "timestamp", shipment.getActualDeparture()));
            if (shipment.getActualArrival() != null) milestones.add(Map.of("stage", "DELIVERED", "timestamp", shipment.getActualArrival()));
        }
        return milestones;
    }

    private List<Map<String, Object>> buildBottlenecksForPo(PurchaseOrder po, Shipment shipment) {
        List<Map<String, Object>> b = new ArrayList<>();
        if (po.getStatus() == PurchaseOrderStatus.PENDING_APPROVAL) b.add(Map.of("stage", "PO_APPROVAL", "delay", "Pending approval"));
        if (shipment != null && shipment.getStatus() == ShipmentStatus.EXCEPTION) b.add(Map.of("stage", "SHIPMENT", "delay", "Exception"));
        if (shipment != null && shipment.getEstimatedArrival() != null && shipment.getEstimatedArrival().before(new java.sql.Timestamp(System.currentTimeMillis())) && shipment.getStatus() != ShipmentStatus.DELIVERED) {
            b.add(Map.of("stage", "DELIVERY", "delay", "Overdue"));
        }
        return b;
    }

    private double calculateLeadTime(PurchaseOrder po, Shipment shipment) {
        if (po.getCreatedAt() == null) return 0;
        java.sql.Timestamp end = null;
        if (shipment != null && shipment.getActualArrival() != null) end = shipment.getActualArrival();
        else if (shipment != null && shipment.getEstimatedArrival() != null) end = shipment.getEstimatedArrival();
        else return 0;
        long diff = end.getTime() - po.getCreatedAt().getTime();
        return Math.round((diff / (1000.0 * 60 * 60 * 24)) * 10.0) / 10.0;
    }
}
