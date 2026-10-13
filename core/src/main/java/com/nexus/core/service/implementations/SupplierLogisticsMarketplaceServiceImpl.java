package com.nexus.core.service.implementations;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.PartnershipInvitationStatus;
import com.nexus.core.model.enums.PartnershipStatus;
import com.nexus.core.model.enums.PurchaseOrderStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.payload.PartnershipInvitationDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.PartnershipInvitationRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.LogisticsExecutionService;
import com.nexus.core.service.interfaces.PartnershipInvitationService;
import com.nexus.core.service.interfaces.SupplierLogisticsMarketplaceService;
import com.nexus.core.utils.RouteCapacityMatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierLogisticsMarketplaceServiceImpl implements SupplierLogisticsMarketplaceService {

    private final CapacityForecastRepo capacityRepo;
    private final PartnershipRepo partnershipRepo;
    private final PartnershipInvitationRepo invitationRepo;
    private final ShipmentRepo shipmentRepo;
    private final PurchaseOrderRepo purchaseOrderRepo;
    private final AccountRepository accountRepo;
    private final PartnershipInvitationService invitationService;
    private final LogisticsExecutionService executionService;

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Map<String, Object> capacityRow(CapacityForecast c) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("forecastId", c.getForecastId());
        Account org = c.getLogisticsOrg();
        row.put("logisticsOrgId", org != null ? org.getAccountId() : null);
        row.put("logisticsOrgName", org != null ? org.getName() : null);
        row.put("originLane", c.getOriginLane());
        row.put("destinationLane", c.getDestinationLane());
        row.put("equipmentType", c.getEquipmentType() != null ? c.getEquipmentType().name() : null);
        row.put("transportMode", c.getTransportMode() != null ? c.getTransportMode().name() : null);
        row.put("periodStart", c.getPeriodStart() != null ? c.getPeriodStart().toString() : null);
        row.put("periodEnd", c.getPeriodEnd() != null ? c.getPeriodEnd().toString() : null);
        row.put("availableCapacity", c.getAvailableCapacity());
        row.put("bookedCapacity", c.getBookedCapacity());
        row.put("unitPrice", c.getUnitPrice());
        row.put("currency", c.getCurrency());
        row.put("totalFuelRequired", c.getTotalFuelRequired());
        row.put("fuelPrice", c.getFuelPrice());
        row.put("fuelSurcharge", c.getFuelSurcharge());
        row.put("driverFees", c.getDriverFees());
        row.put("miscPrice", c.getMiscPrice());
        row.put("totalDistance", c.getTotalDistance());
        row.put("averageDeliveryTime", c.getAverageDeliveryTime());
        row.put("deliveryTimeUom", c.getDeliveryTimeUom());
        row.put("capacityUnit", c.getCapacityUnit() != null ? c.getCapacityUnit().name() : null);
        row.put("unitLength", c.getUnitLength());
        row.put("unitWidth", c.getUnitWidth());
        row.put("unitHeight", c.getUnitHeight());
        row.put("dimensionUom", c.getDimensionUom());
        row.put("unitVolume", c.getUnitVolume());
        row.put("volumeUom", c.getVolumeUom());
        row.put("notes", c.getNotes());
        return row;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAvailableLogistics(String search, Pageable pageable) {
        // Any authenticated supplier may browse routing capacities.
        OrganizationContextHolder.requireOrganizationId();
        Page<CapacityForecast> page = capacityRepo.findMarketplaceAvailabilities(blankToNull(search), pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CapacityForecast c : page.getContent()) {
            rows.add(capacityRow(c));
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> getMyLogisticsPartnerships(Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<Partnership> mine = new ArrayList<>();
        mine.addAll(partnershipRepo.findByPrimaryOrgAccountId(orgId, pageable).getContent());
        mine.addAll(partnershipRepo.findBySecondaryOrgAccountId(orgId, pageable).getContent());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Partnership p : mine) {
            if (!"LOGISTICS".equals(p.getPartnershipType())) continue;
            healAcceptedPartnership(p);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("partnershipId", p.getPartnershipId());
            row.put("primaryOrgId", p.getPrimaryOrg() != null ? p.getPrimaryOrg().getAccountId() : null);
            row.put("secondaryOrgId", p.getSecondaryOrg() != null ? p.getSecondaryOrg().getAccountId() : null);
            row.put("primaryOrgName", p.getPrimaryOrgName() != null ? p.getPrimaryOrgName()
                    : (p.getPrimaryOrg() != null ? p.getPrimaryOrg().getName() : null));
            row.put("secondaryOrgName", p.getSecondaryOrgName() != null ? p.getSecondaryOrgName()
                    : (p.getSecondaryOrg() != null ? p.getSecondaryOrg().getName() : null));
            row.put("partnershipType", p.getPartnershipType());
            row.put("partnershipTerm", p.getPartnershipTerm());
            row.put("partnershipTermType", p.getPartnershipTermType());
            row.put("validityStart", p.getValidityStart() != null ? p.getValidityStart().toString() : null);
            row.put("validityEnd", p.getValidityEnd() != null ? p.getValidityEnd().toString() : null);
            row.put("linkedCapacityForecastId", p.getLinkedCapacityForecastId());
            row.put("desiredRoutesJson", p.getDesiredRoutesJson());
            row.put("desiredCapacity", p.getDesiredCapacity());
            row.put("desiredCapacityUnit", p.getDesiredCapacityUnit());
            row.put("status", p.getStatus() != null ? p.getStatus().name() : null);
            row.put("startDate", p.getStartDate() != null ? p.getStartDate().toString() : null);
            row.put("endDate", p.getEndDate() != null ? p.getEndDate().toString() : null);
            rows.add(row);
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, rows.size()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createProposal(PartnershipInvitationDto invitationDto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        if (invitationDto.getInvitedOrg() == null) {
            throw new ValidationException("invitedOrg (logistics organization) is required");
        }

        String termType = invitationDto.getPartnershipTermType() != null
                ? invitationDto.getPartnershipTermType().trim().toUpperCase()
                : "SHORT_TERM";
        if (!"SHORT_TERM".equals(termType) && !"LONG_TERM".equals(termType)) {
            throw new ValidationException("partnershipTermType must be SHORT_TERM or LONG_TERM");
        }
        invitationDto.setPartnershipTermType(termType);
        invitationDto.setPartnershipContext("SUPPLIER_LOGISTICS");

        // Exclusivity: while a LONG_TERM partnership with this logistics org
        // is still valid, no new proposal may be sent.
        if (hasValidLongTermPartnership(orgId, invitationDto.getInvitedOrg())) {
            throw new ValidationException("A long-term partnership with this logistics partner is already active."
                    + " No new proposal can be sent while it remains valid.");
        }

        if ("SHORT_TERM".equals(termType)) {
            // Short-term: supplier picks a published route; validity is
            // fixed to that routing-capacity period.
            if (invitationDto.getLinkedCapacityForecastId() == null) {
                throw new ValidationException("SHORT_TERM proposals must select a route (linkedCapacityForecastId)");
            }
            CapacityForecast capacity = capacityRepo.findById(invitationDto.getLinkedCapacityForecastId())
                    .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId",
                            invitationDto.getLinkedCapacityForecastId()));
            if (!capacity.getLogisticsOrg().getAccountId().equals(invitationDto.getInvitedOrg())) {
                throw new ValidationException("Selected route does not belong to the invited logistics org");
            }
            Timestamp start = invitationDto.getValidityStart();
            Timestamp end = invitationDto.getValidityEnd();
            if (start == null || end == null) {
                throw new ValidationException("SHORT_TERM proposals require the route's fixed validity dates");
            }
            if (end.before(start)) {
                throw new ValidationException("validityEnd must be on or after validityStart");
            }
            Date periodStart = capacity.getPeriodStart();
            Date periodEnd = capacity.getPeriodEnd();
            if (periodStart != null && start.before(Timestamp.valueOf(periodStart.toLocalDate().atStartOfDay()))) {
                throw new ValidationException("SHORT_TERM validityStart is before the routing-capacity period");
            }
            if (periodEnd != null && end.after(Timestamp.valueOf(periodEnd.toLocalDate().atTime(23, 59, 59)))) {
                throw new ValidationException("SHORT_TERM validityEnd exceeds the routing-capacity period");
            }
        } else {
            // Long-term: supplier defines wanted routes/capacity/dates; no
            // published route is selected.
            invitationDto.setLinkedCapacityForecastId(null);
            if (invitationDto.getDesiredRoutesJson() == null
                    || invitationDto.getDesiredRoutesJson().isBlank()) {
                throw new ValidationException("LONG_TERM proposals require at least one wanted route (from/to)");
            }
            if (invitationDto.getValidityStart() == null || invitationDto.getValidityEnd() == null) {
                throw new ValidationException("LONG_TERM proposals require validityStart and validityEnd");
            }
            if (invitationDto.getValidityEnd().before(invitationDto.getValidityStart())) {
                throw new ValidationException("validityEnd must be on or after validityStart");
            }
            // The logistics partner extends its routing-capacity period on
            // acceptance (PUT /core/logistics/operations/capacity/{id}/extend-for-partnership).
        }

        log.info("Supplier {} proposing {} partnership to logistics org {}",
                orgId, termType, invitationDto.getInvitedOrg());
        return invitationService.createInvitation(invitationDto);
    }

    @Override
    @Transactional
    public ResponseEntity<?> handoverShipment(Long shipmentId, Map<String, Object> body) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
        if (shipment.getSupplierOrg() == null || !shipment.getSupplierOrg().getAccountId().equals(orgId)) {
            throw new ValidationException("Shipment does not belong to your supplier organization");
        }
        if (shipment.getStatus() != ShipmentStatus.DRAFT) {
            throw new ValidationException("Only DRAFT shipments can be handed over. Current: " + shipment.getStatus());
        }

        Partnership partnership;
        Long logisticsOrgId;
        if (body != null && body.get("partnershipId") != null
                && !body.get("partnershipId").toString().isBlank()) {
            // Partnership-selected handover: the logistics side is the
            // partnership counterparty, never the caller's own org.
            Long pid = Long.valueOf(body.get("partnershipId").toString());
            partnership = partnershipRepo.findById(pid)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", pid));
            if (partnership.getStatus() != PartnershipStatus.ACTIVE) {
                throw new ValidationException("Partnership is not ACTIVE: " + partnership.getStatus());
            }
            logisticsOrgId = counterpartyOf(partnership, orgId);
            if (logisticsOrgId == null) {
                throw new ValidationException("Partnership does not involve your organization");
            }
        } else {
            if (body == null || body.get("logisticsOrgId") == null) {
                throw new ValidationException("logisticsOrgId or partnershipId is required for handover");
            }
            logisticsOrgId = Long.valueOf(body.get("logisticsOrgId").toString());
            partnership = resolveActiveLogisticsPartnership(orgId, logisticsOrgId, null);
        }
        Account logisticsOrg = accountRepo.findById(logisticsOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", logisticsOrgId));

        shipment.setLogisticsOrg(logisticsOrg);
        shipment.setPartnership(partnership);
        if (body.get("pickupLocation") != null) shipment.setPickupLocation(body.get("pickupLocation").toString());
        if (body.get("deliveryLocation") != null) shipment.setDeliveryLocation(body.get("deliveryLocation").toString());
        if (body.get("pickupDate") != null && !body.get("pickupDate").toString().isBlank()) {
            shipment.setPickupDate(Date.valueOf(body.get("pickupDate").toString()));
        } else if (shipment.getPickupDate() == null) {
            shipment.setPickupDate(new Date(System.currentTimeMillis()));
        }
        if (body.get("deliveryDate") != null && !body.get("deliveryDate").toString().isBlank()) {
            shipment.setDeliveryDate(Date.valueOf(body.get("deliveryDate").toString()));
        } else if (shipment.getDeliveryDate() == null && shipment.getPurchaseOrder() != null) {
            if (shipment.getPurchaseOrder().getExpectedDeliveryDate() != null) {
                shipment.setDeliveryDate(shipment.getPurchaseOrder().getExpectedDeliveryDate());
            } else if (shipment.getPurchaseOrder().getRequestedDeliveryDate() != null) {
                shipment.setDeliveryDate(shipment.getPurchaseOrder().getRequestedDeliveryDate());
            }
        }
        if (body.get("notes") != null) shipment.setNotes(body.get("notes").toString());
        // Persist the transport mode from the matched route capacity so the
        // load board shows it even though the shipment carries none.
        if (shipment.getShipmentMode() == null) {
            String pickup = shipment.getPickupLocation() != null
                    ? shipment.getPickupLocation() : shipment.getPickupAddress();
            String delivery = shipment.getDeliveryLocation() != null
                    ? shipment.getDeliveryLocation() : shipment.getDeliveryAddress();
            com.nexus.core.model.entities.CapacityForecast route = null;
            if (partnership.getLinkedCapacityForecastId() != null) {
                route = capacityRepo.findById(partnership.getLinkedCapacityForecastId()).orElse(null);
                if (route != null && (route.getTransportMode() == null
                        || !com.nexus.core.utils.RouteCapacityMatcher.routeCovers(route, pickup, delivery))) {
                    route = null;
                }
            }
            if (route == null) {
                java.util.List<com.nexus.core.model.entities.CapacityForecast> privates = capacityRepo
                        .findByPartnershipPartnershipId(partnership.getPartnershipId(),
                                Pageable.unpaged())
                        .getContent();
                route = com.nexus.core.utils.RouteCapacityMatcher.matchWithMode(privates, pickup, delivery);
            }
            if (route != null && route.getTransportMode() != null) {
                shipment.setShipmentMode(route.getTransportMode());
            }
        }
        shipment.setStatus(ShipmentStatus.BOOKED);

        Shipment saved = shipmentRepo.save(shipment);
        // The linked PO waits for pickup instead of jumping to RECEIVED.
        if (saved.getPurchaseOrder() != null && saved.getPurchaseOrder().getPurchaseOrderId() != null) {
            purchaseOrderRepo.findById(saved.getPurchaseOrder().getPurchaseOrderId()).ifPresent(po -> {
                if (po.getStatus() == PurchaseOrderStatus.ACKNOWLEDGED
                        || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                    po.setStatus(PurchaseOrderStatus.AWAITING_PICKUP);
                    purchaseOrderRepo.save(po);
                }
            });
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("shipmentId", saved.getShipmentId());
        resp.put("shipmentNumber", saved.getShipmentNumber());
        resp.put("status", saved.getStatus().name());
        resp.put("logisticsOrgId", logisticsOrgId);
        resp.put("partnershipId", partnership.getPartnershipId());
        return ResponseEntity.ok(resp);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyShipments(String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        ShipmentStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = ShipmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, 0));
            }
        }
        Page<Shipment> page = shipmentRepo.findSupplierVisibleShipments(orgId, null, st, pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Shipment s : page.getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("shipmentId", s.getShipmentId());
            row.put("shipmentNumber", s.getShipmentNumber());
            row.put("status", s.getStatus() != null ? s.getStatus().name() : null);
            if (s.getPurchaseOrder() != null) {
                Map<String, Object> po = new LinkedHashMap<>();
                po.put("purchaseOrderId", s.getPurchaseOrder().getPurchaseOrderId());
                po.put("poNumber", s.getPurchaseOrder().getPoNumber());
                row.put("purchaseOrder", po);
                // Ordered total so clients can derive the shipped quantity
                // (ordered - backordered) without a second round-trip.
                double orderedTotal = 0.0;
                if (s.getPurchaseOrder().getLineItems() != null) {
                    orderedTotal = s.getPurchaseOrder().getLineItems().stream()
                            .mapToDouble(li -> li.getQuantityOrdered() != null ? li.getQuantityOrdered() : 0)
                            .sum();
                }
                row.put("orderedQuantity", orderedTotal);
            }
            if (s.getLogisticsOrg() != null) {
                Map<String, Object> lo = new LinkedHashMap<>();
                lo.put("accountId", s.getLogisticsOrg().getAccountId());
                lo.put("name", s.getLogisticsOrg().getName());
                row.put("logisticsOrg", lo);
            }
            if (s.getPartnership() != null) {
                row.put("partnership", Map.of("partnershipId", s.getPartnership().getPartnershipId()));
            }
            row.put("pickupLocation", s.getPickupLocation());
            row.put("pickupAddress", s.getPickupAddress());
            row.put("deliveryLocation", s.getDeliveryLocation());
            row.put("deliveryAddress", s.getDeliveryAddress());
            // Read-time fallbacks so legacy rows (created before planned
            // dates were defaulted) still show something sensible: pickup
            // falls back to creation day, delivery to the PO's expected
            // (else requested) date.
            String pickupDate = s.getPickupDate() != null ? s.getPickupDate().toString()
                    : (s.getCreatedAt() != null ? s.getCreatedAt().toLocalDateTime().toLocalDate().toString() : null);
            row.put("pickupDate", pickupDate);
            String deliveryDate = s.getDeliveryDate() != null ? s.getDeliveryDate().toString() : null;
            if (deliveryDate == null && s.getPurchaseOrder() != null) {
                if (s.getPurchaseOrder().getExpectedDeliveryDate() != null) {
                    deliveryDate = s.getPurchaseOrder().getExpectedDeliveryDate().toString();
                } else if (s.getPurchaseOrder().getRequestedDeliveryDate() != null) {
                    deliveryDate = s.getPurchaseOrder().getRequestedDeliveryDate().toString();
                }
            }
            row.put("deliveryDate", deliveryDate);
            row.put("trackingNumber", s.getTrackingNumber());
            row.put("carrierName", s.getCarrierName());
            row.put("carrierReference", s.getCarrierReference());
            row.put("shipmentMode", s.getShipmentMode() != null ? s.getShipmentMode().name() : null);
            row.put("incoterms", s.getIncoterms());
            // Package facts captured at partial-ship / handover time.
            row.put("totalPackages", s.getTotalPackages());
            row.put("packageWeight", s.getPackageWeight());
            row.put("packageLength", s.getPackageLength());
            row.put("packageWidth", s.getPackageWidth());
            row.put("packageHeight", s.getPackageHeight());
            row.put("packageDimensionUom", s.getPackageDimensionUom());
            row.put("totalWeight", s.getTotalWeight());
            row.put("totalVolume", s.getTotalVolume());
            row.put("freightCost", s.getFreightCost());
            row.put("actualFreightCost", s.getActualFreightCost());
            row.put("currency", s.getCurrency());
            row.put("freightTerms", s.getFreightTerms());
            // The freight cost lives on the logistics route capacity, not
            // on the shipment: attach the matched lane's pricing so the
            // supplier can see what the move costs.
            try {
                CapacityForecast lane = matchLaneForShipment(s);
                if (lane != null) {
                    Map<String, Object> laneCost = new LinkedHashMap<>();
                    laneCost.put("forecastId", lane.getForecastId());
                    laneCost.put("originLane", lane.getOriginLane());
                    laneCost.put("destinationLane", lane.getDestinationLane());
                    laneCost.put("unitPrice", lane.getUnitPrice());
                    laneCost.put("currency", lane.getCurrency());
                    laneCost.put("capacityUnit",
                            lane.getCapacityUnit() != null ? lane.getCapacityUnit().name() : null);
                    laneCost.put("fuelSurcharge", lane.getFuelSurcharge());
                    laneCost.put("driverFees", lane.getDriverFees());
                    laneCost.put("miscPrice", lane.getMiscPrice());
                    laneCost.put("totalDistance", lane.getTotalDistance());
                    row.put("laneCost", laneCost);
                }
            } catch (Exception e) {
                log.warn("Skipping lane costing for shipment {}: {}",
                        s.getShipmentId(), e.getMessage());
            }
            row.put("notes", s.getNotes());
            row.put("backorderedQuantity", s.getBackorderedQuantity());
            row.put("isPartialShipment", s.getIsPartialShipment());
            // Estimated departure falls back down the timeline that exists
            // (actual pickup, else creation) so legacy rows are not blank.
            String estimatedDeparture = s.getEstimatedDeparture() != null ? s.getEstimatedDeparture().toString()
                    : (s.getActualDeparture() != null ? s.getActualDeparture().toString()
                            : (s.getCreatedAt() != null ? s.getCreatedAt().toString() : null));
            row.put("estimatedDeparture", estimatedDeparture);
            row.put("actualDeparture", s.getActualDeparture() != null ? s.getActualDeparture().toString() : null);
            row.put("estimatedArrival", s.getEstimatedArrival() != null ? s.getEstimatedArrival().toString() : null);
            row.put("actualArrival", s.getActualArrival() != null ? s.getActualArrival().toString() : null);
            row.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            row.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
            rows.add(row);
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getShipmentPosition(Long shipmentId) {
        requireSupplierShipment(shipmentId);
        return executionService.getShipmentPosition(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getShipmentEta(Long shipmentId) {
        requireSupplierShipment(shipmentId);
        return executionService.getShipmentEta(shipmentId);
    }

    private void requireSupplierShipment(Long shipmentId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
        if (shipment.getSupplierOrg() == null || !shipment.getSupplierOrg().getAccountId().equals(orgId)) {
            throw new ValidationException("Shipment does not belong to your supplier organization");
        }
    }

    /**
     * Lane covering a shipment for costing display: the partnership's
     * linked route first, then its private lanes, then the logistics
     * org's lanes, then any public lane (same chain as handover).
     */
    private CapacityForecast matchLaneForShipment(Shipment s) {
        if (s.getPartnership() == null || s.getLogisticsOrg() == null
                || s.getLogisticsOrg().getAccountId() == null) {
            return null;
        }
        Long logisticsOrgId = s.getLogisticsOrg().getAccountId();
        String pickup = s.getPickupLocation() != null ? s.getPickupLocation() : s.getPickupAddress();
        String delivery = s.getDeliveryLocation() != null ? s.getDeliveryLocation() : s.getDeliveryAddress();
        Partnership p = s.getPartnership();
        if (p.getLinkedCapacityForecastId() != null) {
            CapacityForecast linked = capacityRepo.findById(p.getLinkedCapacityForecastId()).orElse(null);
            if (linked != null && RouteCapacityMatcher.routeCovers(linked, pickup, delivery)) {
                return linked;
            }
        }
        List<CapacityForecast> privates = capacityRepo
                .findByPartnershipPartnershipId(p.getPartnershipId(), Pageable.unpaged()).getContent();
        CapacityForecast hit = RouteCapacityMatcher.matchWithMode(privates, pickup, delivery);
        if (hit != null) return hit;
        List<CapacityForecast> orgLanes = capacityRepo
                .findByOrgWithFilters(logisticsOrgId, null, null, Pageable.unpaged()).getContent();
        hit = RouteCapacityMatcher.matchWithMode(orgLanes, pickup, delivery);
        if (hit != null) return hit;
        return RouteCapacityMatcher.matchWithMode(
                capacityRepo.findMarketplaceAvailabilities(null, Pageable.unpaged()).getContent(),
                pickup, delivery);
    }

    private Partnership resolveActiveLogisticsPartnership(Long supplierOrgId, Long logisticsOrgId, Object partnershipIdRaw) {
        if (partnershipIdRaw != null) {
            Long pid = Long.valueOf(partnershipIdRaw.toString());
            Partnership p = partnershipRepo.findById(pid)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", pid));
            if (p.getStatus() != PartnershipStatus.ACTIVE) {
                throw new ValidationException("Partnership is not ACTIVE: " + p.getStatus());
            }
            boolean involvesSupplier = (p.getPrimaryOrg() != null && p.getPrimaryOrg().getAccountId().equals(supplierOrgId))
                    || (p.getSecondaryOrg() != null && p.getSecondaryOrg().getAccountId().equals(supplierOrgId));
            boolean involvesLogistics = (p.getPrimaryOrg() != null && p.getPrimaryOrg().getAccountId().equals(logisticsOrgId))
                    || (p.getSecondaryOrg() != null && p.getSecondaryOrg().getAccountId().equals(logisticsOrgId));
            if (!involvesSupplier || !involvesLogistics) {
                throw new ValidationException("Partnership does not link your org with the selected logistics org");
            }
            return p;
        }
        // Find any ACTIVE logistics partnership between the two orgs.
        List<Partnership> mine = new ArrayList<>();
        mine.addAll(partnershipRepo.findByPrimaryOrgAccountId(supplierOrgId,
                Pageable.ofSize(200)).getContent());
        mine.addAll(partnershipRepo.findBySecondaryOrgAccountId(supplierOrgId,
                Pageable.ofSize(200)).getContent());
        for (Partnership p : mine) {
            if (p.getStatus() != PartnershipStatus.ACTIVE) continue;
            if (!"LOGISTICS".equals(p.getPartnershipType())) continue;
            Long counter = counterpartyOf(p, supplierOrgId);
            if (logisticsOrgId.equals(counter)) return p;
        }
        throw new ValidationException("No ACTIVE logistics partnership with org " + logisticsOrgId
                + ". Send a proposal from the logistics marketplace first.");
    }

    private Long counterpartyOf(Partnership p, Long ownOrgId) {
        if (p.getPrimaryOrg() != null && !p.getPrimaryOrg().getAccountId().equals(ownOrgId)) {
            return p.getPrimaryOrg().getAccountId();
        }
        if (p.getSecondaryOrg() != null && !p.getSecondaryOrg().getAccountId().equals(ownOrgId)) {
            return p.getSecondaryOrg().getAccountId();
        }
        return null;
    }

    // Self-healing: short-term partnerships accepted before acceptance
    // started establishing them directly were left DRAFT with no portal
    // path to ACTIVE. An ACCEPTED non-long-term invitation means
    // established. (Long-term partnerships activate through route
    // completion instead.)
    private void healAcceptedPartnership(Partnership p) {
        if (p.getStatus() != PartnershipStatus.DRAFT) return;
        if ("LONG_TERM".equals(p.getPartnershipTermType())) return;
        if (p.getInvitationId() == null) return;
        var inv = invitationRepo.findByInvitationId(p.getInvitationId()).orElse(null);
        if (inv == null) return;
        if (inv.getStatus() != PartnershipInvitationStatus.ACCEPTED) return;
        if (!"SUPPLIER_LOGISTICS".equals(inv.getPartnershipContext())) return;
        p.setStatus(PartnershipStatus.ACTIVE);
        partnershipRepo.save(p);
    }

    // A long-term partnership counts as still valid while it is ACTIVE and
    // its validity window (when set) has not ended.
    private boolean isStillValid(Partnership p) {
        if (p.getStatus() != PartnershipStatus.ACTIVE) return false;
        if (p.getValidityEnd() == null) return true;
        return !p.getValidityEnd().before(Timestamp.valueOf(LocalDateTime.now()));
    }

    private boolean hasValidLongTermPartnership(Long supplierOrgId, Long logisticsOrgId) {
        List<Partnership> mine = new ArrayList<>();
        mine.addAll(partnershipRepo.findByPrimaryOrgAccountId(supplierOrgId,
                Pageable.ofSize(200)).getContent());
        mine.addAll(partnershipRepo.findBySecondaryOrgAccountId(supplierOrgId,
                Pageable.ofSize(200)).getContent());
        for (Partnership p : mine) {
            if (!"LOGISTICS".equals(p.getPartnershipType())) continue;
            if (!"LONG_TERM".equals(p.getPartnershipTermType())) continue;
            if (!isStillValid(p)) continue;
            if (logisticsOrgId.equals(counterpartyOf(p, supplierOrgId))) return true;
        }
        return false;
    }
}
