package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.entities.CarrierPayable;
import com.nexus.core.model.entities.ConsolidationGroup;
import com.nexus.core.model.entities.Partnership;
import com.nexus.core.model.enums.ConsolidationStatus;
import com.nexus.core.model.enums.FleetAssetStatus;
import com.nexus.core.model.enums.FleetAssetType;
import com.nexus.core.model.enums.PayableStatus;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.CapacityForecastDto;
import com.nexus.core.payload.CarrierPayableDto;
import com.nexus.core.payload.ConsolidationGroupDto;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.CarrierPayableRepo;
import com.nexus.core.repository.ConsolidationGroupRepo;
import com.nexus.core.repository.FleetAssetRepo;
import com.nexus.core.repository.PartnershipRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.AccountDirectory;
import com.nexus.core.service.interfaces.LogisticsOperationsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsOperationsServiceImpl implements LogisticsOperationsService {

    private final ConsolidationGroupRepo groupRepo;
    private final CapacityForecastRepo capacityRepo;
    private final CarrierPayableRepo payableRepo;
    private final ShipmentRepo shipmentRepo;
    private final FleetAssetRepo assetRepo;
    private final PartnershipRepo partnershipRepo;
    private final AccountDirectory accountDirectory;
    private final ModelMapper modelMapper;

    private static final Map<ConsolidationStatus, Set<ConsolidationStatus>> GROUP_ALLOWED = Map.of(
            ConsolidationStatus.OPEN, Set.of(ConsolidationStatus.LOCKED, ConsolidationStatus.CANCELLED),
            ConsolidationStatus.LOCKED, Set.of(ConsolidationStatus.IN_TRANSIT, ConsolidationStatus.OPEN, ConsolidationStatus.CANCELLED),
            ConsolidationStatus.IN_TRANSIT, Set.of(ConsolidationStatus.DELIVERED, ConsolidationStatus.CANCELLED),
            ConsolidationStatus.DELIVERED, Set.of(),
            ConsolidationStatus.CANCELLED, Set.of());

    private static final Map<PayableStatus, Set<PayableStatus>> PAYABLE_ALLOWED = Map.of(
            PayableStatus.PENDING, Set.of(PayableStatus.APPROVED, PayableStatus.DISPUTED, PayableStatus.CANCELLED),
            PayableStatus.APPROVED, Set.of(PayableStatus.PAID, PayableStatus.DISPUTED, PayableStatus.CANCELLED),
            PayableStatus.DISPUTED, Set.of(PayableStatus.APPROVED, PayableStatus.CANCELLED),
            PayableStatus.PAID, Set.of(),
            PayableStatus.CANCELLED, Set.of());

    @Override
    @Transactional
    public ResponseEntity<?> createGroup(ConsolidationGroupDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        var group = modelMapper.map(dto, ConsolidationGroup.class);
        group.setGroupId(null);
        group.setLogisticsOrg(org);
        group.setGroupNumber("LCG-" + System.currentTimeMillis());
        group.setStatus(ConsolidationStatus.OPEN);
        if (group.getShipmentIds() == null) group.setShipmentIds(new ArrayList<>());
        recalculateTotals(group);
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(groupRepo.save(group), ConsolidationGroupDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroup(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var group = groupRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsolidationGroup", "groupId", id));
        return ResponseEntity.ok(modelMapper.map(group, ConsolidationGroupDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllGroups(String status, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        ConsolidationStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = ConsolidationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }
        Page<ConsolidationGroup> page = groupRepo.findByOrgWithFilters(orgId, st, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(g -> modelMapper.map(g, ConsolidationGroupDto.class)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateGroup(Long id, ConsolidationGroupDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = groupRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsolidationGroup", "groupId", id));
        if (existing.getStatus() != ConsolidationStatus.OPEN) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only OPEN groups can be edited"));
        }
        var org = existing.getLogisticsOrg();
        var number = existing.getGroupNumber();
        modelMapper.map(dto, existing);
        existing.setGroupId(id);
        existing.setLogisticsOrg(org);
        existing.setGroupNumber(number);
        existing.setStatus(ConsolidationStatus.OPEN);
        recalculateTotals(existing);
        return ResponseEntity.ok(modelMapper.map(groupRepo.save(existing), ConsolidationGroupDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionGroupStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var group = groupRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsolidationGroup", "groupId", id));
        ConsolidationStatus target;
        try {
            target = ConsolidationStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!GROUP_ALLOWED.getOrDefault(group.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + group.getStatus() + " -> " + target));
        }
        group.setStatus(target);
        if (params != null && params.get("notes") != null) group.setNotes(String.valueOf(params.get("notes")));
        return ResponseEntity.ok(modelMapper.map(groupRepo.save(group), ConsolidationGroupDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> addShipmentsToGroup(Long id, Map<String, Object> body) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var group = groupRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsolidationGroup", "groupId", id));
        if (group.getStatus() != ConsolidationStatus.OPEN) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only OPEN groups accept shipments"));
        }
        Object raw = body != null ? body.get("shipmentIds") : null;
        if (!(raw instanceof List<?> ids) || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "shipmentIds list is required"));
        }
        for (var rawId : ids) {
            Long shipmentId = Long.valueOf(String.valueOf(rawId));
            Shipment shipment = shipmentRepo.findById(shipmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
            if (!group.getShipmentIds().contains(shipmentId)) group.getShipmentIds().add(shipmentId);
        }
        recalculateTotals(group);
        return ResponseEntity.ok(modelMapper.map(groupRepo.save(group), ConsolidationGroupDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteGroup(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var group = groupRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsolidationGroup", "groupId", id));
        group.setIsActive(false);
        groupRepo.save(group);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> createCapacity(CapacityForecastDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        var capacity = new CapacityForecast();
        copyCapacityFields(dto, capacity);
        capacity.setForecastId(null);
        capacity.setLogisticsOrg(org);
        // Private partnership capacity: lane belongs to one long-term
        // partnership only (hidden from the public marketplace).
        if (dto.getPartnershipId() != null) {
            var partnership = partnershipRepo.findById(dto.getPartnershipId())
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId",
                            dto.getPartnershipId()));
            boolean involvesCaller = (partnership.getPrimaryOrg() != null
                    && partnership.getPrimaryOrg().getAccountId().equals(orgId))
                    || (partnership.getSecondaryOrg() != null
                            && partnership.getSecondaryOrg().getAccountId().equals(orgId));
            if (!involvesCaller) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Partnership does not involve your organization"));
            }
            if (!"LOGISTICS".equals(partnership.getPartnershipType())
                    || !"LONG_TERM".equals(partnership.getPartnershipTermType())) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Private capacities require a long-term logistics partnership"));
            }
            capacity.setPartnership(partnership);
        }
        var error = validateCapacitySpecs(capacity);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        deriveUnitVolume(capacity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCapacityDto(capacityRepo.save(capacity)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCapacity(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var capacity = capacityRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId", id));
        return ResponseEntity.ok(toCapacityDto(capacity));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllCapacities(String equipmentType, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        FleetAssetType et = null;
        if (equipmentType != null && !equipmentType.isBlank()) {
            try {
                et = FleetAssetType.valueOf(equipmentType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid equipmentType: " + equipmentType));
            }
        }
        Page<CapacityForecast> page = capacityRepo.findByOrgWithFilters(orgId, et, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(this::toCapacityDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateCapacity(Long id, CapacityForecastDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = capacityRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId", id));
        var org = existing.getLogisticsOrg();
        copyCapacityFields(dto, existing);
        existing.setForecastId(id);
        existing.setLogisticsOrg(org);
        var error = validateCapacitySpecs(existing);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        deriveUnitVolume(existing);
        return ResponseEntity.ok(toCapacityDto(capacityRepo.save(existing)));
    }

    // Manual mapping: ModelMapper cannot disambiguate *Id fields across
    // the joined relations (Partnership.partnershipId/invitationId/… all
    // match CapacityForecastDto.partnershipId), so both directions are
    // copied explicitly.
    private CapacityForecastDto toCapacityDto(CapacityForecast c) {
        CapacityForecastDto dto = new CapacityForecastDto();
        dto.setForecastId(c.getForecastId());
        dto.setOriginLane(c.getOriginLane());
        dto.setDestinationLane(c.getDestinationLane());
        dto.setEquipmentType(c.getEquipmentType());
        dto.setPeriodStart(c.getPeriodStart());
        dto.setPeriodEnd(c.getPeriodEnd());
        dto.setAvailableCapacity(c.getAvailableCapacity());
        dto.setBookedCapacity(c.getBookedCapacity());
        dto.setCapacityUnit(c.getCapacityUnit());
        dto.setUnitPrice(c.getUnitPrice());
        dto.setCurrency(c.getCurrency());
        dto.setPartnershipId(
                c.getPartnership() != null ? c.getPartnership().getPartnershipId() : null);
        dto.setUnitLength(c.getUnitLength());
        dto.setUnitWidth(c.getUnitWidth());
        dto.setUnitHeight(c.getUnitHeight());
        dto.setDimensionUom(c.getDimensionUom());
        dto.setUnitVolume(c.getUnitVolume());
        dto.setVolumeUom(c.getVolumeUom());
        dto.setNotes(c.getNotes());
        return dto;
    }

    private void copyCapacityFields(CapacityForecastDto dto, CapacityForecast target) {
        if (dto.getOriginLane() != null) target.setOriginLane(dto.getOriginLane());
        if (dto.getDestinationLane() != null) target.setDestinationLane(dto.getDestinationLane());
        if (dto.getEquipmentType() != null) target.setEquipmentType(dto.getEquipmentType());
        if (dto.getPeriodStart() != null) target.setPeriodStart(dto.getPeriodStart());
        if (dto.getPeriodEnd() != null) target.setPeriodEnd(dto.getPeriodEnd());
        if (dto.getAvailableCapacity() != null) target.setAvailableCapacity(dto.getAvailableCapacity());
        if (dto.getBookedCapacity() != null) target.setBookedCapacity(dto.getBookedCapacity());
        if (dto.getCapacityUnit() != null) target.setCapacityUnit(dto.getCapacityUnit());
        if (dto.getUnitPrice() != null) target.setUnitPrice(dto.getUnitPrice());
        if (dto.getCurrency() != null) target.setCurrency(dto.getCurrency());
        if (dto.getUnitLength() != null) target.setUnitLength(dto.getUnitLength());
        if (dto.getUnitWidth() != null) target.setUnitWidth(dto.getUnitWidth());
        if (dto.getUnitHeight() != null) target.setUnitHeight(dto.getUnitHeight());
        if (dto.getDimensionUom() != null) target.setDimensionUom(dto.getDimensionUom());
        if (dto.getUnitVolume() != null) target.setUnitVolume(dto.getUnitVolume());
        if (dto.getVolumeUom() != null) target.setVolumeUom(dto.getVolumeUom());
        if (dto.getNotes() != null) target.setNotes(dto.getNotes());
    }

    // Unitized capacities (pallets/containers) count load units, so the
    // per-unit dimensions are mandatory — otherwise suppliers cannot tell
    // whether their freight fits.
    private String validateCapacitySpecs(CapacityForecast capacity) {
        if (capacity.getCapacityUnit() == null) {
            capacity.setCapacityUnit(com.nexus.core.model.enums.CapacityUnit.KG);
        }
        if (!capacity.getCapacityUnit().isUnitized()) {
            return null;
        }
        if (capacity.getUnitLength() == null || capacity.getUnitWidth() == null
                || capacity.getUnitHeight() == null) {
            return "Unitized capacity (" + capacity.getCapacityUnit()
                    + ") requires unit length, width and height specifications";
        }
        if (capacity.getUnitLength() <= 0 || capacity.getUnitWidth() <= 0
                || capacity.getUnitHeight() <= 0) {
            return "Unit dimensions must be positive numbers";
        }
        return null;
    }

    // Auto-fill unit volume from LxWxH when the caller did not supply one,
    // normalizing to the declared volume UoM.
    private void deriveUnitVolume(CapacityForecast capacity) {
        if (capacity.getUnitVolume() != null) return;
        if (capacity.getUnitLength() == null || capacity.getUnitWidth() == null
                || capacity.getUnitHeight() == null) return;
        double toMeters = "FT".equalsIgnoreCase(capacity.getDimensionUom()) ? 0.3048 : 1.0;
        double cbm = capacity.getUnitLength() * toMeters
                * capacity.getUnitWidth() * toMeters
                * capacity.getUnitHeight() * toMeters;
        String vuom = capacity.getVolumeUom() != null ? capacity.getVolumeUom().toUpperCase() : "CBM";
        double volume = switch (vuom) {
            case "CFT" -> cbm * 35.3147;
            case "L" -> cbm * 1000.0;
            default -> cbm;
        };
        capacity.setUnitVolume(Math.round(volume * 100.0) / 100.0);
        if (capacity.getVolumeUom() == null) capacity.setVolumeUom("CBM");
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteCapacity(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var capacity = capacityRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId", id));
        capacity.setIsActive(false);
        capacityRepo.save(capacity);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<?> extendCapacityForPartnership(Long id, Map<String, Object> body) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var capacity = capacityRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId", id));
        if (body == null || body.get("partnershipId") == null || body.get("newPeriodEnd") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "partnershipId and newPeriodEnd are required"));
        }
        Long partnershipId = Long.valueOf(body.get("partnershipId").toString());
        var partnership = partnershipRepo.findById(partnershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Partnership", "partnershipId", partnershipId));
        boolean involvesCaller = (partnership.getPrimaryOrg() != null && partnership.getPrimaryOrg().getAccountId().equals(orgId))
                || (partnership.getSecondaryOrg() != null && partnership.getSecondaryOrg().getAccountId().equals(orgId));
        if (!involvesCaller) {
            return ResponseEntity.badRequest().body(Map.of("error", "Partnership does not involve your organization"));
        }
        if (!"LONG_TERM".equals(partnership.getPartnershipTermType())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only LONG_TERM partnerships allow routing-capacity extension"));
        }
        java.sql.Date newEnd;
        try {
            newEnd = java.sql.Date.valueOf(body.get("newPeriodEnd").toString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "newPeriodEnd must be yyyy-MM-dd"));
        }
        if (capacity.getPeriodEnd() != null && !newEnd.after(capacity.getPeriodEnd())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "newPeriodEnd must be after the current period end"));
        }
        capacity.setPeriodEnd(newEnd);
        if (body.get("availableCapacity") != null) {
            capacity.setAvailableCapacity(Double.valueOf(body.get("availableCapacity").toString()));
        }
        String note = "Extended for LONG_TERM partnership " + partnershipId;
        capacity.setNotes(capacity.getNotes() == null ? note : capacity.getNotes() + " | " + note);
        // Keep the partnership validity aligned with the extended routing window.
        if (partnership.getValidityEnd() == null
                || partnership.getValidityEnd().before(Timestamp.valueOf(newEnd.toLocalDate().atTime(23, 59, 59)))) {
            partnership.setValidityEnd(Timestamp.valueOf(newEnd.toLocalDate().atTime(23, 59, 59)));
            partnershipRepo.save(partnership);
        }
        return ResponseEntity.ok(toCapacityDto(capacityRepo.save(capacity)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createPayable(CarrierPayableDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        Shipment shipment = null;
        if (dto.getShipmentId() != null) {
            shipment = shipmentRepo.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
        }
        var payable = modelMapper.map(dto, CarrierPayable.class);
        payable.setPayableId(null);
        payable.setLogisticsOrg(org);
        payable.setShipment(shipment);
        payable.setPayableNumber("CPB-" + System.currentTimeMillis());
        payable.setStatus(PayableStatus.PENDING);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPayableDto(payableRepo.save(payable)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPayable(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var payable = payableRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CarrierPayable", "payableId", id));
        return ResponseEntity.ok(toPayableDto(payable));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllPayables(Long shipmentId, String status, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        PayableStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = PayableStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }
        Page<CarrierPayable> page = payableRepo.findByOrgWithFilters(orgId, shipmentId, st, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(this::toPayableDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updatePayable(Long id, CarrierPayableDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = payableRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CarrierPayable", "payableId", id));
        if (existing.getStatus() == PayableStatus.PAID || existing.getStatus() == PayableStatus.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Paid or cancelled payables cannot be edited"));
        }
        if (dto.getCarrierName() != null) existing.setCarrierName(dto.getCarrierName());
        if (dto.getPayableAmount() != null) existing.setPayableAmount(dto.getPayableAmount());
        if (dto.getPaidAmount() != null) existing.setPaidAmount(dto.getPaidAmount());
        if (dto.getCurrency() != null) existing.setCurrency(dto.getCurrency());
        if (dto.getDueDate() != null) existing.setDueDate(dto.getDueDate());
        if (dto.getPmsReferenceId() != null) existing.setPmsReferenceId(dto.getPmsReferenceId());
        if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
        if (dto.getShipmentId() != null && (existing.getShipment() == null
                || !dto.getShipmentId().equals(existing.getShipment().getShipmentId()))) {
            var shipment = shipmentRepo.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
            existing.setShipment(shipment);
        }
        return ResponseEntity.ok(toPayableDto(payableRepo.save(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionPayableStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var payable = payableRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CarrierPayable", "payableId", id));
        PayableStatus target;
        try {
            target = PayableStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!PAYABLE_ALLOWED.getOrDefault(payable.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + payable.getStatus() + " -> " + target));
        }
        payable.setStatus(target);
        if (params != null && params.get("paidAmount") != null) {
            payable.setPaidAmount(new BigDecimal(String.valueOf(params.get("paidAmount"))));
        }
        if (params != null && params.get("pmsReferenceId") != null) {
            payable.setPmsReferenceId(String.valueOf(params.get("pmsReferenceId")));
        }
        if (params != null && params.get("notes") != null) payable.setNotes(String.valueOf(params.get("notes")));
        return ResponseEntity.ok(toPayableDto(payableRepo.save(payable)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deletePayable(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var payable = payableRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CarrierPayable", "payableId", id));
        payable.setIsActive(false);
        payableRepo.save(payable);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getLogisticsDashboard() {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var dashboard = new LinkedHashMap<String, Object>();
        var shipments = shipmentRepo.findByLogisticsOrgAccountId(orgId, Pageable.unpaged()).getContent();
        var revenue = BigDecimal.ZERO;
        var deliveredCount = 0;
        Map<String, BigDecimal> revenuePerLane = new LinkedHashMap<>();
        Map<String, BigDecimal> revenuePerCustomer = new LinkedHashMap<>();
        for (var shipment : shipments) {
            if (shipment.getActualFreightCost() != null) {
                revenue = revenue.add(BigDecimal.valueOf(shipment.getActualFreightCost()));
            } else if (shipment.getFreightCost() != null) {
                revenue = revenue.add(BigDecimal.valueOf(shipment.getFreightCost()));
            }
            if (shipment.getStatus() == ShipmentStatus.DELIVERED) deliveredCount++;
            String lane = (shipment.getPickupLocation() != null ? shipment.getPickupLocation() : "?")
                    + " → " + (shipment.getDeliveryLocation() != null ? shipment.getDeliveryLocation() : "?");
            BigDecimal amount = shipment.getActualFreightCost() != null
                    ? BigDecimal.valueOf(shipment.getActualFreightCost())
                    : BigDecimal.valueOf(shipment.getFreightCost() != null ? shipment.getFreightCost() : 0.0);
            revenuePerLane.merge(lane, amount, BigDecimal::add);
            String customer = shipment.getRetailerOrg() != null ? String.valueOf(shipment.getRetailerOrg().getAccountId()) : "unknown";
            revenuePerCustomer.merge(customer, amount, BigDecimal::add);
        }
        dashboard.put("totalShipments", shipments.size());
        dashboard.put("deliveredShipments", deliveredCount);
        dashboard.put("totalRevenue", revenue);
        dashboard.put("revenuePerLane", revenuePerLane);
        dashboard.put("revenuePerCustomer", revenuePerCustomer);
        long totalAssets = assetRepo.findByOrgWithFilters(orgId, null, null, null, Pageable.unpaged()).getTotalElements();
        long availableAssets = assetRepo.findByOrgWithFilters(orgId, FleetAssetStatus.AVAILABLE, null, null, Pageable.unpaged()).getTotalElements();
        double utilization = totalAssets == 0 ? 0.0
                : BigDecimal.valueOf(totalAssets - availableAssets).divide(BigDecimal.valueOf(totalAssets), 4, RoundingMode.HALF_UP).doubleValue();
        dashboard.put("totalAssets", totalAssets);
        dashboard.put("availableAssets", availableAssets);
        dashboard.put("equipmentUtilization", utilization);
        dashboard.put("openPayables", payableRepo.findByOrgWithFilters(orgId, null, PayableStatus.PENDING, null, Pageable.unpaged()).getTotalElements());
        return ResponseEntity.ok(dashboard);
    }

    private void recalculateTotals(ConsolidationGroup group) {
        double weight = 0.0;
        double volume = 0.0;
        if (group.getShipmentIds() != null) {
            List<Shipment> shipments = shipmentRepo.findAllById(group.getShipmentIds());
            for (var shipment : shipments) {
                if (shipment.getTotalWeight() != null) weight += shipment.getTotalWeight();
                if (shipment.getTotalVolume() != null) volume += shipment.getTotalVolume();
            }
        }
        group.setTotalWeight(weight);
        group.setTotalVolume(volume);
    }

    private CarrierPayableDto toPayableDto(CarrierPayable payable) {
        var dto = modelMapper.map(payable, CarrierPayableDto.class);
        if (payable.getShipment() != null) dto.setShipmentId(payable.getShipment().getShipmentId());
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
