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
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.payload.PartnershipInvitationDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.PartnershipInvitationRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.PartnershipInvitationService;
import com.nexus.core.service.interfaces.SupplierLogisticsMarketplaceService;

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
    private final AccountRepository accountRepo;
    private final PartnershipInvitationService invitationService;

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
        row.put("periodStart", c.getPeriodStart() != null ? c.getPeriodStart().toString() : null);
        row.put("periodEnd", c.getPeriodEnd() != null ? c.getPeriodEnd().toString() : null);
        row.put("availableCapacity", c.getAvailableCapacity());
        row.put("bookedCapacity", c.getBookedCapacity());
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
        if (body.get("pickupDate") != null) shipment.setPickupDate(Date.valueOf(body.get("pickupDate").toString()));
        if (body.get("deliveryDate") != null) shipment.setDeliveryDate(Date.valueOf(body.get("deliveryDate").toString()));
        if (body.get("notes") != null) shipment.setNotes(body.get("notes").toString());
        shipment.setStatus(ShipmentStatus.BOOKED);

        Shipment saved = shipmentRepo.save(shipment);
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
            row.put("deliveryLocation", s.getDeliveryLocation());
            row.put("trackingNumber", s.getTrackingNumber());
            rows.add(row);
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, page.getTotalElements()));
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

    // Self-healing: partnerships accepted before acceptance started
    // establishing them directly were left DRAFT with no portal path to
    // ACTIVE. An ACCEPTED supplier-logistics invitation means established.
    private void healAcceptedPartnership(Partnership p) {
        if (p.getStatus() != PartnershipStatus.DRAFT) return;
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
