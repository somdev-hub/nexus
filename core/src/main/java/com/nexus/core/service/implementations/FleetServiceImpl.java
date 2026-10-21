package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.model.entities.Account;
import com.nexus.core.model.entities.Driver;
import com.nexus.core.model.entities.DriverCommission;
import com.nexus.core.model.entities.FleetAsset;
import com.nexus.core.model.entities.MaintenanceRecord;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.enums.DriverCommissionStatus;
import com.nexus.core.model.enums.DriverStatus;
import com.nexus.core.model.enums.FleetAssetStatus;
import com.nexus.core.model.enums.FleetAssetType;
import com.nexus.core.model.enums.MaintenanceStatus;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.payload.AssetDriverHistoryDto;
import com.nexus.core.payload.AssetShipmentDto;
import com.nexus.core.payload.DriverDto;
import com.nexus.core.payload.FleetAssetDto;
import com.nexus.core.payload.MaintenanceRecordDto;
import com.nexus.core.repository.DriverCommissionRepo;
import com.nexus.core.repository.DriverRepo;
import com.nexus.core.repository.FleetAssetRepo;
import com.nexus.core.repository.MaintenanceRecordRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.AccountDirectory;
import com.nexus.core.service.interfaces.FleetService;
import com.nexus.core.service.interfaces.LogisticsBillingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FleetServiceImpl implements FleetService {

    private final FleetAssetRepo assetRepo;
    private final DriverRepo driverRepo;
    private final MaintenanceRecordRepo maintenanceRepo;
    private final ShipmentRepo shipmentRepo;
    private final DriverCommissionRepo commissionRepo;
    private final AccountDirectory accountDirectory;
    private final ModelMapper modelMapper;
    private final LogisticsBillingService billingService;

    private static final Map<FleetAssetStatus, Set<FleetAssetStatus>> ASSET_ALLOWED = Map.of(
            FleetAssetStatus.AVAILABLE, Set.of(FleetAssetStatus.ASSIGNED, FleetAssetStatus.IN_MAINTENANCE, FleetAssetStatus.OUT_OF_SERVICE, FleetAssetStatus.RETIRED),
            FleetAssetStatus.ASSIGNED, Set.of(FleetAssetStatus.AVAILABLE, FleetAssetStatus.IN_MAINTENANCE, FleetAssetStatus.OUT_OF_SERVICE),
            FleetAssetStatus.IN_MAINTENANCE, Set.of(FleetAssetStatus.AVAILABLE, FleetAssetStatus.OUT_OF_SERVICE),
            FleetAssetStatus.OUT_OF_SERVICE, Set.of(FleetAssetStatus.AVAILABLE, FleetAssetStatus.IN_MAINTENANCE, FleetAssetStatus.RETIRED),
            FleetAssetStatus.RETIRED, Set.of());

    private static final Map<DriverStatus, Set<DriverStatus>> DRIVER_ALLOWED = Map.of(
            DriverStatus.AVAILABLE, Set.of(DriverStatus.ASSIGNED, DriverStatus.ON_LEAVE, DriverStatus.SUSPENDED, DriverStatus.INACTIVE),
            DriverStatus.ASSIGNED, Set.of(DriverStatus.AVAILABLE, DriverStatus.ON_LEAVE, DriverStatus.SUSPENDED),
            DriverStatus.ON_LEAVE, Set.of(DriverStatus.AVAILABLE, DriverStatus.INACTIVE),
            DriverStatus.SUSPENDED, Set.of(DriverStatus.AVAILABLE, DriverStatus.INACTIVE),
            DriverStatus.INACTIVE, Set.of(DriverStatus.AVAILABLE));

    private static final Map<MaintenanceStatus, Set<MaintenanceStatus>> MAINT_ALLOWED = Map.of(
            MaintenanceStatus.SCHEDULED, Set.of(MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.CANCELLED),
            MaintenanceStatus.IN_PROGRESS, Set.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED),
            MaintenanceStatus.OVERDUE, Set.of(MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.CANCELLED),
            MaintenanceStatus.COMPLETED, Set.of(),
            MaintenanceStatus.CANCELLED, Set.of());

    // Assets
    @Override
    @Transactional
    public ResponseEntity<?> createAsset(FleetAssetDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        assetRepo.findByAssetNumberAndLogisticsOrgAccountId(dto.getAssetNumber(), orgId)
                .ifPresent(a -> { throw new IllegalArgumentException("Asset number already exists: " + dto.getAssetNumber()); });
        var asset = modelMapper.map(dto, FleetAsset.class);
        asset.setAssetId(null);
        asset.setLogisticsOrg(org);
        if (asset.getStatus() == null) asset.setStatus(FleetAssetStatus.AVAILABLE);
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(assetRepo.save(asset), FleetAssetDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAsset(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var asset = assetRepo.findByAssetIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", id));
        return ResponseEntity.ok(modelMapper.map(asset, FleetAssetDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllAssets(String status, String assetType, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        FleetAssetStatus st = parseEnum(status, FleetAssetStatus.class, "status");
        FleetAssetType type = parseEnum(assetType, FleetAssetType.class, "assetType");
        Page<FleetAsset> page = assetRepo.findByOrgWithFilters(orgId, st, type, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(a -> modelMapper.map(a, FleetAssetDto.class)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateAsset(Long id, FleetAssetDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = assetRepo.findByAssetIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", id));
        var status = existing.getStatus();
        var org = existing.getLogisticsOrg();
        modelMapper.map(dto, existing);
        existing.setAssetId(id);
        existing.setStatus(dto.getStatus() != null ? dto.getStatus() : status);
        existing.setLogisticsOrg(org);
        return ResponseEntity.ok(modelMapper.map(assetRepo.save(existing), FleetAssetDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionAssetStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var asset = assetRepo.findByAssetIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", id));
        FleetAssetStatus target;
        try {
            target = FleetAssetStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!ASSET_ALLOWED.getOrDefault(asset.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + asset.getStatus() + " -> " + target));
        }
        asset.setStatus(target);
        if (params != null && params.get("notes") != null) asset.setNotes(String.valueOf(params.get("notes")));
        return ResponseEntity.ok(modelMapper.map(assetRepo.save(asset), FleetAssetDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteAsset(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var asset = assetRepo.findByAssetIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", id));
        asset.setIsActive(false);
        assetRepo.save(asset);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAssetSummary() {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var summary = new LinkedHashMap<String, Object>();
        for (var s : FleetAssetStatus.values()) {
            summary.put(s.name(), assetRepo.findByOrgWithFilters(orgId, s, null, null, Pageable.unpaged()).getTotalElements());
        }
        return ResponseEntity.ok(summary);
    }

    // Drivers
    @Override
    @Transactional
    public ResponseEntity<?> createDriver(DriverDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Account org = accountDirectory.getOrCreateAccount(orgId);
        driverRepo.findByDriverCodeAndLogisticsOrgAccountId(dto.getDriverCode(), orgId)
                .ifPresent(d -> { throw new IllegalArgumentException("Driver code already exists: " + dto.getDriverCode()); });
        var driver = modelMapper.map(dto, Driver.class);
        driver.setDriverId(null);
        driver.setLogisticsOrg(org);
        if (driver.getStatus() == null) driver.setStatus(DriverStatus.AVAILABLE);
        return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(driverRepo.save(driver), DriverDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDriver(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var driver = driverRepo.findByDriverIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", id));
        return ResponseEntity.ok(modelMapper.map(driver, DriverDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllDrivers(String status, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        DriverStatus st = parseEnum(status, DriverStatus.class, "status");
        Page<Driver> page = driverRepo.findByOrgWithFilters(orgId, st, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(d -> modelMapper.map(d, DriverDto.class)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateDriver(Long id, DriverDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = driverRepo.findByDriverIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", id));
        var status = existing.getStatus();
        var org = existing.getLogisticsOrg();
        Long iamUserId = existing.getIamUserId();
        String hrEmployeeId = existing.getHrEmployeeId();
        modelMapper.map(dto, existing);
        existing.setDriverId(id);
        existing.setStatus(dto.getStatus() != null ? dto.getStatus() : status);
        existing.setLogisticsOrg(org);
        // employee links set by IAM onboarding must survive plain edits
        if (dto.getIamUserId() == null) existing.setIamUserId(iamUserId);
        if (dto.getHrEmployeeId() == null) existing.setHrEmployeeId(hrEmployeeId);
        return ResponseEntity.ok(modelMapper.map(driverRepo.save(existing), DriverDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionDriverStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var driver = driverRepo.findByDriverIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", id));
        DriverStatus target;
        try {
            target = DriverStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!DRIVER_ALLOWED.getOrDefault(driver.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + driver.getStatus() + " -> " + target));
        }
        driver.setStatus(target);
        if (params != null && params.get("notes") != null) driver.setNotes(String.valueOf(params.get("notes")));
        return ResponseEntity.ok(modelMapper.map(driverRepo.save(driver), DriverDto.class));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteDriver(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var driver = driverRepo.findByDriverIdAndLogisticsOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", id));
        driver.setIsActive(false);
        driverRepo.save(driver);
        return ResponseEntity.noContent().build();
    }

    // Maintenance
    @Override
    @Transactional
    public ResponseEntity<?> createMaintenance(MaintenanceRecordDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var asset = assetRepo.findByAssetIdAndLogisticsOrgAccountId(dto.getAssetId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", dto.getAssetId()));
        var record = modelMapper.map(dto, MaintenanceRecord.class);
        record.setMaintenanceId(null);
        record.setAsset(asset);
        if (record.getMaintenanceNumber() == null || record.getMaintenanceNumber().isBlank()) {
            record.setMaintenanceNumber("MNT-" + System.currentTimeMillis());
        }
        if (record.getStatus() == null) record.setStatus(MaintenanceStatus.SCHEDULED);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMaintenanceDto(maintenanceRepo.save(record)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMaintenance(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var record = maintenanceRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRecord", "maintenanceId", id));
        return ResponseEntity.ok(toMaintenanceDto(record));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllMaintenance(Long assetId, String status, Boolean isBreakdown, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        MaintenanceStatus st = parseEnum(status, MaintenanceStatus.class, "status");
        Page<MaintenanceRecord> page = maintenanceRepo.findByOrgWithFilters(orgId, assetId, st, isBreakdown, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(this::toMaintenanceDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateMaintenance(Long id, MaintenanceRecordDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = maintenanceRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRecord", "maintenanceId", id));
        if (existing.getStatus() == MaintenanceStatus.COMPLETED || existing.getStatus() == MaintenanceStatus.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Completed or cancelled records cannot be edited"));
        }
        if (dto.getMaintenanceType() != null) existing.setMaintenanceType(dto.getMaintenanceType());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getScheduledDate() != null) existing.setScheduledDate(dto.getScheduledDate());
        if (dto.getCompletedDate() != null) existing.setCompletedDate(dto.getCompletedDate());
        if (dto.getOdometerReading() != null) existing.setOdometerReading(dto.getOdometerReading());
        if (dto.getCost() != null) existing.setCost(dto.getCost());
        if (dto.getServiceProvider() != null) existing.setServiceProvider(dto.getServiceProvider());
        if (dto.getIsBreakdown() != null) existing.setIsBreakdown(dto.getIsBreakdown());
        if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
        if (dto.getAssetId() != null && !dto.getAssetId().equals(existing.getAsset().getAssetId())) {
            var target = assetRepo.findByAssetIdAndLogisticsOrgAccountId(dto.getAssetId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", dto.getAssetId()));
            existing.setAsset(target);
        }
        return ResponseEntity.ok(toMaintenanceDto(maintenanceRepo.save(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionMaintenanceStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var record = maintenanceRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRecord", "maintenanceId", id));
        MaintenanceStatus target;
        try {
            target = MaintenanceStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!MAINT_ALLOWED.getOrDefault(record.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + record.getStatus() + " -> " + target));
        }
        record.setStatus(target);
        if (target == MaintenanceStatus.IN_PROGRESS && record.getAsset() != null) {
            record.getAsset().setStatus(FleetAssetStatus.IN_MAINTENANCE);
        }
        if (target == MaintenanceStatus.COMPLETED) {
            record.setCompletedDate(new java.sql.Date(System.currentTimeMillis()));
            if (record.getAsset() != null) {
                record.getAsset().setStatus(FleetAssetStatus.AVAILABLE);
                record.getAsset().setLastMaintenanceDate(record.getCompletedDate());
                if (record.getOdometerReading() != null) record.getAsset().setCurrentMileage(record.getOdometerReading());
            }
            if (params != null && params.get("cost") != null) {
                record.setCost(new java.math.BigDecimal(String.valueOf(params.get("cost"))));
            }
        }
        if (params != null && params.get("notes") != null) record.setNotes(String.valueOf(params.get("notes")));
        return ResponseEntity.ok(toMaintenanceDto(maintenanceRepo.save(record)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteMaintenance(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var record = maintenanceRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRecord", "maintenanceId", id));
        record.setIsActive(false);
        maintenanceRepo.save(record);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAssetShipments(Long assetId, String status, String from, String to, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        assetRepo.findByAssetIdAndLogisticsOrgAccountId(assetId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", assetId));
        ShipmentStatus st = null;
        if (status != null && !status.isBlank()) {
            try {
                st = ShipmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }
        java.sql.Date fromDate = toSqlDate(from);
        java.sql.Date toDate = toSqlDate(to);
        if ((from != null && !from.isBlank() && fromDate == null) || (to != null && !to.isBlank() && toDate == null)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid from/to datetime, expected ISO format e.g. 2026-09-28T14:30"));
        }
        var page = shipmentRepo.findByAssetWithFilters(orgId, assetId, st, fromDate, toDate, pageable);
        return ResponseEntity.ok(page.map(this::toAssetShipmentDto));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAssetDrivers(Long assetId) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        assetRepo.findByAssetIdAndLogisticsOrgAccountId(assetId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", assetId));
        var trips = shipmentRepo.findByAssignedAssetIdAndLogisticsOrgAccountId(assetId, orgId);
        var byDriver = new LinkedHashMap<Long, java.util.List<Shipment>>();
        for (var shipment : trips) {
            if (shipment.getAssignedDriverId() == null) continue;
            byDriver.computeIfAbsent(shipment.getAssignedDriverId(), k -> new java.util.ArrayList<>()).add(shipment);
        }
        var history = new java.util.ArrayList<AssetDriverHistoryDto>();
        for (var entry : byDriver.entrySet()) {
            var driven = entry.getValue();
            var first = driven.stream().map(Shipment::getCreatedAt).filter(java.util.Objects::nonNull)
                    .min(java.sql.Timestamp::compareTo).orElse(null);
            var last = driven.stream().map(Shipment::getCreatedAt).filter(java.util.Objects::nonNull)
                    .max(java.sql.Timestamp::compareTo).orElse(null);
            var name = driven.stream().map(Shipment::getCarrierName).filter(java.util.Objects::nonNull)
                    .findFirst().orElse("Driver #" + entry.getKey());
            history.add(AssetDriverHistoryDto.builder()
                    .driverId(entry.getKey()).driverName(name).trips(driven.size())
                    .firstTripAt(first).lastTripAt(last).build());
        }
        history.sort((a, b) -> Integer.compare(b.getTrips(), a.getTrips()));
        return ResponseEntity.ok(history);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAssetCurrentShipment(Long assetId) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        assetRepo.findByAssetIdAndLogisticsOrgAccountId(assetId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", assetId));
        var active = shipmentRepo.findByAssignedAssetIdAndLogisticsOrgAccountId(assetId, orgId).stream()
                .filter(s -> s.getStatus() != ShipmentStatus.DELIVERED
                        && s.getStatus() != ShipmentStatus.CLOSED
                        && s.getStatus() != ShipmentStatus.CANCELLED)
                .max((a, b) -> {
                    var at = a.getCreatedAt();
                    var bt = b.getCreatedAt();
                    if (at == null) return -1;
                    if (bt == null) return 1;
                    return at.compareTo(bt);
                });
        var body = new LinkedHashMap<String, Object>();
        body.put("shipmentId", active.map(Shipment::getShipmentId).orElse(null));
        return ResponseEntity.ok(body);
    }

    private AssetShipmentDto toAssetShipmentDto(Shipment shipment) {
        return AssetShipmentDto.builder()
                .shipmentId(shipment.getShipmentId())
                .shipmentNumber(shipment.getShipmentNumber())
                .status(shipment.getStatus())
                .pickupDate(shipment.getPickupDate())
                .deliveryDate(shipment.getDeliveryDate())
                .actualDeparture(shipment.getActualDeparture())
                .actualArrival(shipment.getActualArrival())
                .driverId(shipment.getAssignedDriverId())
                .driverName(shipment.getCarrierName())
                .freightCost(shipment.getFreightCost())
                .build();
    }

    private static java.sql.Date toSqlDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) return null;
        try {
            return java.sql.Date.valueOf(java.time.LocalDateTime.parse(isoDateTime).toLocalDate());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private MaintenanceRecordDto toMaintenanceDto(MaintenanceRecord record) {
        var dto = modelMapper.map(record, MaintenanceRecordDto.class);
        if (record.getAsset() != null) dto.setAssetId(record.getAsset().getAssetId());
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> type, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }
    }

    // ── Driver commissions ──────────────────────────────────────────────

    /** Trip completion states that earn a driver commission. */
    private static final List<ShipmentStatus> COMMISSION_TRIP_STATUSES =
            List.of(ShipmentStatus.DELIVERED, ShipmentStatus.PARTIALLY_DELIVERED, ShipmentStatus.CLOSED);

    @Override
    @Transactional
    public ResponseEntity<?> listDriverCommissions(String status, String search, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        DriverStatus st = parseEnum(status, DriverStatus.class, "status");
        Page<Driver> page = driverRepo.findByOrgWithFilters(orgId, st, blankToNull(search), pageable);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Driver d : page.getContent()) {
            items.add(driverCommissionRow(d, orgId));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", items);
        body.put("totalElements", page.getTotalElements());
        body.put("totalPages", page.getTotalPages());
        body.put("number", page.getNumber());
        body.put("size", page.getSize());
        return ResponseEntity.ok(body);
    }

    @Override
    @Transactional
    public ResponseEntity<?> listDriverCommissionItems(Long driverId, String status, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Driver driver = driverRepo.findByDriverIdAndLogisticsOrgAccountId(driverId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", driverId));
        DriverCommissionStatus st = parseEnum(status, DriverCommissionStatus.class, "status");
        ensureDriverCommissions(driver, orgId);
        Page<DriverCommission> page = st != null
                ? commissionRepo.findByDriverIdAndLogisticsOrgIdAndStatus(driverId, orgId, st, pageable)
                : commissionRepo.findByDriverIdAndLogisticsOrgId(driverId, orgId, pageable);
        List<Map<String, Object>> items = new ArrayList<>();
        for (DriverCommission c : page.getContent()) {
            items.add(commissionDto(c));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", items);
        body.put("totalElements", page.getTotalElements());
        body.put("totalPages", page.getTotalPages());
        body.put("number", page.getNumber());
        body.put("size", page.getSize());
        return ResponseEntity.ok(body);
    }

    @Override
    @Transactional
    public ResponseEntity<?> payDriverCommission(Long driverId, Long commissionId, Map<String, Object> body) {
        DriverCommission c = requireCommission(driverId, commissionId);
        if (c.getStatus() == DriverCommissionStatus.PAID) {
            return ResponseEntity.badRequest().body(Map.of("error", "Commission already paid"));
        }
        applyPayment(c, body);
        return ResponseEntity.ok(commissionDto(commissionRepo.save(c)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> payDriverCommissions(Long driverId, Map<String, Object> body) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        driverRepo.findByDriverIdAndLogisticsOrgAccountId(driverId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", driverId));
        List<DriverCommission> pending = commissionRepo
                .findByStatusAndDriverIdInAndLogisticsOrgId(DriverCommissionStatus.PENDING, List.of(driverId), orgId);
        for (DriverCommission c : pending) {
            applyPayment(c, body);
            commissionRepo.save(c);
        }
        return ResponseEntity.ok(Map.of("paidCount", pending.size()));
    }

    private DriverCommission requireCommission(Long driverId, Long commissionId) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        driverRepo.findByDriverIdAndLogisticsOrgAccountId(driverId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", driverId));
        DriverCommission c = commissionRepo.findByCommissionIdAndDriverId(commissionId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission", "commissionId", commissionId));
        return c;
    }

    private void applyPayment(DriverCommission c, Map<String, Object> body) {
        c.setStatus(DriverCommissionStatus.PAID);
        c.setPaidAt(new Timestamp(System.currentTimeMillis()));
        String reference = body != null && body.get("reference") != null
                && !String.valueOf(body.get("reference")).isBlank()
                        ? String.valueOf(body.get("reference"))
                        : "MANUAL";
        c.setPaymentRef(reference);
        if (body != null && body.get("notes") != null && !String.valueOf(body.get("notes")).isBlank()) {
            c.setNotes(String.valueOf(body.get("notes")));
        }
    }

    private Map<String, Object> driverCommissionRow(Driver d, Long orgId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("driverId", d.getDriverId());
        row.put("driverCode", d.getDriverCode());
        row.put("fullName", d.getFullName());
        row.put("status", d.getStatus() != null ? d.getStatus().name() : null);
        row.put("iamUserId", d.getIamUserId());

        List<Shipment> recent = shipmentRepo
                .findTop10ByAssignedDriverIdAndLogisticsOrgAccountIdOrderByShipmentIdDesc(d.getDriverId(), orgId);
        Shipment last = recent != null && !recent.isEmpty() ? recent.get(0) : null;
        if (last != null) {
            row.put("lastActiveAt", last.getUpdatedAt() != null ? last.getUpdatedAt() : last.getCreatedAt());
            Map<String, Object> la = new LinkedHashMap<>();
            la.put("shipmentId", last.getShipmentId());
            la.put("shipmentNumber", last.getShipmentNumber());
            la.put("status", last.getStatus() != null ? last.getStatus().name() : null);
            la.put("lane", billingService.laneLabel(last));
            row.put("lastAssignment", la);
        } else {
            row.put("lastActiveAt", d.getUpdatedAt() != null ? d.getUpdatedAt() : d.getCreatedAt());
            row.put("lastAssignment", null);
        }

        ensureDriverCommissions(d, orgId);

        List<DriverCommission> pending = commissionRepo.findByStatusAndDriverIdInAndLogisticsOrgId(
                DriverCommissionStatus.PENDING, List.of(d.getDriverId()), orgId);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (DriverCommission c : pending) {
            if (c.getCurrency() == null) continue;
            BigDecimal current = totals.getOrDefault(c.getCurrency(), BigDecimal.ZERO);
            totals.put(c.getCurrency(), current.add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO));
        }
        List<Map<String, Object>> payable = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : totals.entrySet()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("currency", e.getKey());
            t.put("amount", e.getValue());
            payable.add(t);
        }
        row.put("payable", payable);
        row.put("pendingCount", pending.size());
        return row;
    }

    /** Self-heal: one commission line per delivered trip, equal to the matched route capacity's flat driver fees. */
    private void ensureDriverCommissions(Driver d, Long orgId) {
        List<Shipment> delivered = shipmentRepo.findByAssignedDriverIdAndLogisticsOrgAccountIdAndStatusIn(
                d.getDriverId(), orgId, COMMISSION_TRIP_STATUSES);
        if (delivered == null || delivered.isEmpty()) return;
        for (Shipment s : delivered) {
            DriverCommission c = commissionRepo
                    .findByDriverIdAndShipmentId(d.getDriverId(), s.getShipmentId())
                    .orElse(null);
            LogisticsBillingService.TripDriverFees fees = billingService.tripDriverFees(s);
            if (c == null) {
                c = new DriverCommission();
                c.setDriverId(d.getDriverId());
                c.setLogisticsOrgId(orgId);
                c.setShipmentId(s.getShipmentId());
                c.setShipmentNumber(s.getShipmentNumber());
                c.setLane(billingService.laneLabel(s));
                c.setShipmentStatus(s.getStatus() != null ? s.getStatus().name() : null);
                c.setDeliveredAt(s.getDeliveryDate() != null ? s.getDeliveryDate()
                        : s.getActualArrival() != null
                                ? new java.sql.Date(s.getActualArrival().getTime())
                                : null);
                c.setStatus(DriverCommissionStatus.PENDING);
            }
            if (c.getStatus() == DriverCommissionStatus.PENDING) {
                BigDecimal amount = fees.amount() != null ? fees.amount() : BigDecimal.ZERO;
                String currency = fees.currency();
                if (c.getAmount() == null || c.getAmount().compareTo(amount) != 0
                        || !Objects.equals(c.getCurrency(), currency)) {
                    c.setAmount(amount);
                    c.setCurrency(currency);
                }
            }
            commissionRepo.save(c);
        }
    }

    private Map<String, Object> commissionDto(DriverCommission c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("commissionId", c.getCommissionId());
        m.put("driverId", c.getDriverId());
        m.put("shipmentId", c.getShipmentId());
        m.put("shipmentNumber", c.getShipmentNumber());
        m.put("lane", c.getLane());
        m.put("deliveredAt", c.getDeliveredAt());
        m.put("amount", c.getAmount());
        m.put("currency", c.getCurrency());
        m.put("shipmentStatus", c.getShipmentStatus());
        m.put("status", c.getStatus() != null ? c.getStatus().name() : null);
        m.put("paidAt", c.getPaidAt());
        m.put("paymentRef", c.getPaymentRef());
        m.put("notes", c.getNotes());
        return m;
    }
}
