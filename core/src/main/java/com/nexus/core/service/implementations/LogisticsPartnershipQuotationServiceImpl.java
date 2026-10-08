package com.nexus.core.service.implementations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.exception.ValidationException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.entities.LogisticsPartnershipQuotation;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.entities.PartnershipInvitation;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.PartnershipInvitationStatus;
import com.nexus.core.model.enums.PartnershipQuotationStatus;
import com.nexus.core.model.enums.PartnershipStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.payload.LogisticsPartnershipQuotationDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.LogisticsPartnershipQuotationRepo;
import com.nexus.core.repository.PartnershipInvitationRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.AccountDirectory;
import com.nexus.core.service.interfaces.LogisticsPartnershipQuotationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsPartnershipQuotationServiceImpl implements LogisticsPartnershipQuotationService {

    private final LogisticsPartnershipQuotationRepo quotationRepo;
    private final PartnershipInvitationRepo invitationRepo;
    private final PartnershipRepo partnershipRepo;
    private final CapacityForecastRepo capacityRepo;
    private final ShipmentRepo shipmentRepo;
    private final AccountRepository accountRepo;
    private final AccountDirectory accountDirectory;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static boolean isUnitizedUnit(String unit) {
        String u = norm(unit).toUpperCase();
        return "PALLETS".equals(u) || "SHIPPING_CONTAINER".equals(u) || "FREIGHT_CONTAINER".equals(u);
    }

    private static boolean isBlankNum(Object v) {
        if (v == null) return true;
        if (v instanceof Number n) return !(n.doubleValue() > 0);
        try {
            return !(Double.parseDouble(v.toString()) > 0);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static boolean laneMatches(String lane, String want) {
        String a = norm(lane);
        String b = norm(want);
        return !a.isEmpty() && !b.isEmpty() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    private List<Map<String, Object>> parseLines(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            throw new ValidationException("routeLinesJson is not a valid JSON array");
        }
    }

    // Manual mapping: ModelMapper cannot disambiguate *Id fields across
    // the joined relations (same failure mode as the invitation/account
    // mappings elsewhere), so scalar fields are copied explicitly.
    private LogisticsPartnershipQuotationDto toDto(LogisticsPartnershipQuotation q) {
        LogisticsPartnershipQuotationDto dto = new LogisticsPartnershipQuotationDto();
        dto.setQuotationId(q.getQuotationId());
        dto.setQuotationNumber(q.getQuotationNumber());
        dto.setStatus(q.getStatus());
        dto.setRouteLinesJson(q.getRouteLinesJson());
        dto.setTotalAmount(q.getTotalAmount());
        dto.setCurrency(q.getCurrency());
        dto.setValidityStart(q.getValidityStart());
        dto.setValidityEnd(q.getValidityEnd());
        dto.setTerms(q.getTerms());
        if (q.getInvitation() != null) dto.setInvitationId(q.getInvitation().getInvitationId());
        if (q.getSupplierOrg() != null) {
            dto.setSupplierOrgId(q.getSupplierOrg().getAccountId());
            dto.setSupplierOrgName(q.getSupplierOrg().getName());
        }
        if (q.getLogisticsOrg() != null) {
            dto.setLogisticsOrgId(q.getLogisticsOrg().getAccountId());
            dto.setLogisticsOrgName(q.getLogisticsOrg().getName());
        }
        if (q.getPartnership() != null) dto.setPartnershipId(q.getPartnership().getPartnershipId());
        return dto;
    }

    private LogisticsPartnershipQuotation findVisible(Long id, Long orgId) {
        LogisticsPartnershipQuotation q = quotationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsPartnershipQuotation", "quotationId", id));
        boolean mine = (q.getSupplierOrg() != null && q.getSupplierOrg().getAccountId().equals(orgId))
                || (q.getLogisticsOrg() != null && q.getLogisticsOrg().getAccountId().equals(orgId));
        if (!mine) throw new ValidationException("Quotation does not involve your organization");
        return q;
    }

    private Partnership requireLogisticsPartnership(Long partnershipId, Long orgId) {
        Partnership p = partnershipRepo.findById(partnershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));
        boolean mine = (p.getPrimaryOrg() != null && p.getPrimaryOrg().getAccountId().equals(orgId))
                || (p.getSecondaryOrg() != null && p.getSecondaryOrg().getAccountId().equals(orgId));
        if (!mine) throw new ValidationException("Partnership does not involve your organization");
        if (!"LOGISTICS".equals(p.getPartnershipType())) {
            throw new ValidationException("Not a logistics partnership");
        }
        return p;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createQuotation(LogisticsPartnershipQuotationDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        if (dto.getInvitationId() == null) {
            throw new ValidationException("invitationId (the long-term proposal) is required");
        }
        PartnershipInvitation invitation = invitationRepo.findById(dto.getInvitationId())
                .orElseThrow(() -> new ResourceNotFoundException("PartnershipInvitation", "invitationId",
                        dto.getInvitationId()));
        if (!"SUPPLIER_LOGISTICS".equals(invitation.getPartnershipContext())
                || !"LONG_TERM".equals(invitation.getPartnershipTermType())) {
            throw new ValidationException("Quotations can only answer long-term logistics proposals");
        }
        if (invitation.getInvitedOrg() == null || !invitation.getInvitedOrg().getAccountId().equals(orgId)) {
            throw new ValidationException("Only the invited logistics organization can raise a quotation");
        }
        if (invitation.getStatus() != PartnershipInvitationStatus.PENDING
                && invitation.getStatus() != PartnershipInvitationStatus.ACCEPTED) {
            throw new ValidationException("Proposal is no longer open for quotation: " + invitation.getStatus());
        }
        List<Map<String, Object>> lines = parseLines(dto.getRouteLinesJson());
        if (lines.isEmpty()) {
            throw new ValidationException("At least one route line (from/to) is required");
        }
        for (Map<String, Object> line : lines) {
            Object from = line.get("fromLane") != null ? line.get("fromLane") : line.get("from");
            Object to = line.get("toLane") != null ? line.get("toLane") : line.get("to");
            if (from == null || from.toString().isBlank() || to == null || to.toString().isBlank()) {
                throw new ValidationException("Every route line needs from and to lanes");
            }
            // Unitized units (pallets/containers) require proper per-unit
            // dimensions on the quotation line itself, so the later route
            // capacity inherits them by default.
            Object unitRaw = line.get("capacityUnit");
            if (unitRaw != null && isUnitizedUnit(unitRaw.toString())
                    && (isBlankNum(line.get("unitLength")) || isBlankNum(line.get("unitWidth"))
                            || isBlankNum(line.get("unitHeight")))) {
                throw new ValidationException("Route " + from + " → " + to
                        + " uses a container/pallet unit and requires unit length, width and height");
            }
        }

        // First quotation engages the proposal: mark it accepted and anchor
        // a DRAFT partnership for the private routes. It comes into effect
        // (ACTIVE) only after all agreed routes are added.
        Partnership partnership = partnershipRepo.findByInvitationId(invitation.getInvitationId()).orElse(null);
        if (partnership == null) {
            partnership = new Partnership();
            partnership.setPrimaryOrg(invitation.getInvitingOrg());
            partnership.setSecondaryOrg(invitation.getInvitedOrg());
            partnership.setPartnershipType("LOGISTICS");
            partnership.setPartnershipTerm(invitation.getProposedTerms());
            partnership.setPartnershipTermType("LONG_TERM");
            partnership.setValidityStart(invitation.getValidityStart());
            partnership.setValidityEnd(invitation.getValidityEnd());
            partnership.setDesiredRoutesJson(invitation.getDesiredRoutesJson());
            partnership.setDesiredCapacity(invitation.getDesiredCapacity());
            partnership.setDesiredCapacityUnit(invitation.getDesiredCapacityUnit());
            partnership.setStatus(PartnershipStatus.DRAFT);
            partnership.setStartDate(Timestamp.valueOf(LocalDateTime.now()));
            partnership.setInvitationId(invitation.getInvitationId());
            partnership = partnershipRepo.save(partnership);
        }
        invitation.setStatus(PartnershipInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(Timestamp.valueOf(LocalDateTime.now()));
        invitationRepo.save(invitation);

        Account supplierOrg = invitation.getInvitingOrg();
        Account logisticsOrg = accountDirectory.getOrCreateAccount(orgId);
        LogisticsPartnershipQuotation q = new LogisticsPartnershipQuotation();
        q.setQuotationNumber("LQ-" + System.currentTimeMillis());
        q.setInvitation(invitation);
        q.setSupplierOrg(supplierOrg);
        q.setLogisticsOrg(logisticsOrg);
        q.setPartnership(partnership);
        q.setStatus(PartnershipQuotationStatus.SUBMITTED);
        q.setRouteLinesJson(dto.getRouteLinesJson());
        q.setTotalAmount(dto.getTotalAmount());
        q.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
        q.setValidityStart(dto.getValidityStart() != null ? dto.getValidityStart() : invitation.getValidityStart());
        q.setValidityEnd(dto.getValidityEnd() != null ? dto.getValidityEnd() : invitation.getValidityEnd());
        q.setTerms(dto.getTerms());
        LogisticsPartnershipQuotation saved = quotationRepo.save(q);
        log.info("Logistics org {} raised quotation {} for proposal {}", orgId, saved.getQuotationNumber(),
                invitation.getInvitationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyQuotations(String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        PartnershipQuotationStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = PartnershipQuotationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(new PageImpl<>(List.of(), pageable, 0));
            }
        }
        Page<LogisticsPartnershipQuotation> page = quotationRepo.findVisibleToOrg(orgId, pageable);
        List<LogisticsPartnershipQuotationDto> rows = new ArrayList<>();
        for (LogisticsPartnershipQuotation q : page.getContent()) {
            if (st != null && q.getStatus() != st) continue;
            rows.add(toDto(q));
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getQuotation(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        return ResponseEntity.ok(toDto(findVisible(id, orgId)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> respondToQuotation(Long id, Map<String, Object> body) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        LogisticsPartnershipQuotation q = findVisible(id, orgId);
        if (q.getSupplierOrg() == null || !q.getSupplierOrg().getAccountId().equals(orgId)) {
            throw new ValidationException("Only the supplier can review this quotation");
        }
        if (q.getStatus() != PartnershipQuotationStatus.SUBMITTED) {
            throw new ValidationException("Only SUBMITTED quotations can be reviewed. Current: " + q.getStatus());
        }
        Object raw = body != null ? body.get("action") : null;
        String action = raw != null ? raw.toString().trim().toUpperCase() : "";
        if ("ACCEPT".equals(action)) {
            q.setStatus(PartnershipQuotationStatus.ACCEPTED);
        } else if ("REJECT".equals(action)) {
            q.setStatus(PartnershipQuotationStatus.REJECTED);
        } else {
            throw new ValidationException("action must be ACCEPT or REJECT");
        }
        q.setRespondedAt(Timestamp.valueOf(LocalDateTime.now()));
        return ResponseEntity.ok(toDto(quotationRepo.save(q)));
    }

    private List<CapacityForecast> privateRoutesOf(Long partnershipId, Pageable pageable) {
        return capacityRepo.findByPartnershipPartnershipId(partnershipId, pageable).getContent();
    }

    @Override
    @Transactional
    public ResponseEntity<?> activatePartnership(Long partnershipId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Partnership p = requireLogisticsPartnership(partnershipId, orgId);
        if (!"LONG_TERM".equals(p.getPartnershipTermType())) {
            throw new ValidationException("Only long-term partnerships activate through route completion");
        }
        if (p.getStatus() == PartnershipStatus.ACTIVE) {
            throw new ValidationException("Partnership is already active");
        }
        if (p.getStatus() == PartnershipStatus.TERMINATED) {
            throw new ValidationException("Terminated partnerships cannot be activated");
        }
        // Every wanted route must have a private capacity before the
        // partnership comes into effect.
        List<Map<String, Object>> wanted = new ArrayList<>();
        if (p.getInvitationId() != null) {
            var inv = invitationRepo.findByInvitationId(p.getInvitationId()).orElse(null);
            if (inv != null && inv.getDesiredRoutesJson() != null && !inv.getDesiredRoutesJson().isBlank()) {
                try {
                    List<Map<String, Object>> parsed = objectMapper.readValue(inv.getDesiredRoutesJson(),
                            new TypeReference<List<Map<String, Object>>>() {
                            });
                    if (parsed != null) wanted.addAll(parsed);
                } catch (Exception e) {
                    throw new ValidationException("Stored wanted routes are not valid JSON");
                }
            }
        }
        List<CapacityForecast> privates = privateRoutesOf(partnershipId, Pageable.ofSize(500));
        List<String> missing = new ArrayList<>();
        for (Map<String, Object> w : wanted) {
            String from = w.get("from") != null ? w.get("from").toString() : "";
            String to = w.get("to") != null ? w.get("to").toString() : "";
            boolean covered = privates.stream().anyMatch(c ->
                    laneMatches(c.getOriginLane(), from) && laneMatches(c.getDestinationLane(), to));
            if (!covered) missing.add(from + " → " + to);
        }
        if (!missing.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "All agreed routes must be added first",
                    "missingRoutes", missing));
        }
        p.setStatus(PartnershipStatus.ACTIVE);
        partnershipRepo.save(p);
        return ResponseEntity.ok(Map.of(
                "partnershipId", p.getPartnershipId(),
                "status", "ACTIVE",
                "routesAdded", privates.size()));
    }

    private Map<String, Object> privateRouteRow(CapacityForecast c, Long partnershipId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("forecastId", c.getForecastId());
        row.put("partnershipId", partnershipId);
        row.put("logisticsOrgId",
                c.getLogisticsOrg() != null ? c.getLogisticsOrg().getAccountId() : null);
        row.put("logisticsOrgName",
                c.getLogisticsOrg() != null ? c.getLogisticsOrg().getName() : null);
        row.put("originLane", c.getOriginLane());
        row.put("destinationLane", c.getDestinationLane());
        row.put("equipmentType", c.getEquipmentType() != null ? c.getEquipmentType().name() : null);
        row.put("periodStart", c.getPeriodStart() != null ? c.getPeriodStart().toString() : null);
        row.put("periodEnd", c.getPeriodEnd() != null ? c.getPeriodEnd().toString() : null);
        row.put("availableCapacity", c.getAvailableCapacity());
        row.put("bookedCapacity", c.getBookedCapacity());
        row.put("capacityUnit", c.getCapacityUnit() != null ? c.getCapacityUnit().name() : null);
        row.put("unitPrice", c.getUnitPrice());
        row.put("currency", c.getCurrency());
        row.put("unitLength", c.getUnitLength());
        row.put("unitWidth", c.getUnitWidth());
        row.put("unitHeight", c.getUnitHeight());
        row.put("dimensionUom", c.getDimensionUom());
        row.put("unitVolume", c.getUnitVolume());
        row.put("volumeUom", c.getVolumeUom());
        return row;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPartnershipRoutes(Long partnershipId, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        requireLogisticsPartnership(partnershipId, orgId);
        Page<CapacityForecast> page = capacityRepo.findByPartnershipPartnershipId(partnershipId, pageable);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CapacityForecast c : page.getContent()) {
            rows.add(privateRouteRow(c, partnershipId));
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, page.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyPrivateRoutes(Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        List<Partnership> mine = new ArrayList<>();
        mine.addAll(partnershipRepo.findByPrimaryOrgAccountId(orgId, Pageable.ofSize(200)).getContent());
        mine.addAll(partnershipRepo.findBySecondaryOrgAccountId(orgId, Pageable.ofSize(200)).getContent());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Partnership p : mine) {
            if (!"LOGISTICS".equals(p.getPartnershipType())) continue;
            for (CapacityForecast c : privateRoutesOf(p.getPartnershipId(), Pageable.ofSize(200))) {
                rows.add(privateRouteRow(c, p.getPartnershipId()));
            }
        }
        return ResponseEntity.ok(new PageImpl<>(rows, pageable, rows.size()));
    }

    private List<Shipment> shipmentsOf(Long partnershipId) {
        return shipmentRepo.findByPartnershipPartnershipId(partnershipId, Pageable.ofSize(500)).getContent();
    }

    private static final List<ShipmentStatus> TERMINAL = List.of(
            ShipmentStatus.DELIVERED, ShipmentStatus.PARTIALLY_DELIVERED,
            ShipmentStatus.CLOSED, ShipmentStatus.CANCELLED);

    private static final List<ShipmentStatus> PRE_SHIPPING = List.of(
            ShipmentStatus.DRAFT, ShipmentStatus.PENDING_APPROVAL,
            ShipmentStatus.APPROVED, ShipmentStatus.BOOKED, ShipmentStatus.ASSIGNED);

    @Override
    @Transactional
    public ResponseEntity<?> terminateAsSupplier(Long partnershipId, Map<String, Object> body) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Partnership p = requireLogisticsPartnership(partnershipId, orgId);
        if (p.getStatus() == PartnershipStatus.TERMINATED) {
            throw new ValidationException("Partnership is already terminated");
        }
        // Supplier may terminate only when no handover is in transit:
        // every shipment is either never handed over (DRAFT) or delivered.
        List<Shipment> active = new ArrayList<>();
        for (Shipment s : shipmentsOf(partnershipId)) {
            if (!TERMINAL.contains(s.getStatus()) && s.getStatus() != ShipmentStatus.DRAFT) {
                active.add(s);
            }
        }
        if (!active.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot terminate while handovers are still in transit",
                    "blockingShipments", active.stream().map(Shipment::getShipmentNumber).toList()));
        }
        p.setStatus(PartnershipStatus.TERMINATED);
        partnershipRepo.save(p);
        return ResponseEntity.ok(Map.of("partnershipId", p.getPartnershipId(), "status", "TERMINATED"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> terminateAsLogistics(Long partnershipId, Map<String, Object> body) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Partnership p = requireLogisticsPartnership(partnershipId, orgId);
        if (p.getStatus() == PartnershipStatus.TERMINATED) {
            throw new ValidationException("Partnership is already terminated");
        }
        List<Shipment> shipments = shipmentsOf(partnershipId);
        // Termination is allowed any time before shipping starts: nothing
        // may be picked up or beyond.
        List<Shipment> inMotion = new ArrayList<>();
        for (Shipment s : shipments) {
            if (!TERMINAL.contains(s.getStatus()) && !PRE_SHIPPING.contains(s.getStatus())) {
                inMotion.add(s);
            }
        }
        if (!inMotion.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot terminate while shipments are already moving",
                    "blockingShipments", inMotion.stream().map(Shipment::getShipmentNumber).toList()));
        }
        // Handed-over loads pending on the load board go back to the
        // supplier as prepared (DRAFT) shipments.
        List<String> released = new ArrayList<>();
        for (Shipment s : shipments) {
            if (s.getStatus() == ShipmentStatus.BOOKED || s.getStatus() == ShipmentStatus.ASSIGNED) {
                s.setStatus(ShipmentStatus.DRAFT);
                s.setLogisticsOrg(null);
                s.setAssignedDriverId(null);
                s.setAssignedAssetId(null);
                if (s.getNotes() == null) {
                    s.setNotes("Released back to supplier on partnership termination");
                }
                shipmentRepo.save(s);
                released.add(s.getShipmentNumber());
            }
        }
        p.setStatus(PartnershipStatus.TERMINATED);
        partnershipRepo.save(p);
        return ResponseEntity.ok(Map.of(
                "partnershipId", p.getPartnershipId(),
                "status", "TERMINATED",
                "releasedShipments", released));
    }
}
