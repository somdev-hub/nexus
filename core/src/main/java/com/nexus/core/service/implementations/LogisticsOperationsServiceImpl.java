package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        var capacity = modelMapper.map(dto, CapacityForecast.class);
        capacity.setForecastId(null);
        capacity.setLogisticsOrg(org);
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(capacityRepo.save(capacity), CapacityForecastDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCapacity(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var capacity = capacityRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId", id));
        return ResponseEntity.ok(modelMapper.map(capacity, CapacityForecastDto.class));
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
        return ResponseEntity.ok(page.map(c -> modelMapper.map(c, CapacityForecastDto.class)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateCapacity(Long id, CapacityForecastDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = capacityRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("CapacityForecast", "forecastId", id));
        var org = existing.getLogisticsOrg();
        modelMapper.map(dto, existing);
        existing.setForecastId(id);
        existing.setLogisticsOrg(org);
        return ResponseEntity.ok(modelMapper.map(capacityRepo.save(existing), CapacityForecastDto.class));
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
