package com.nexus.core.service.implementations;

import com.nexus.core.dto.TrackingEventDto;
import com.nexus.core.model.entities.Driver;
import com.nexus.core.model.enums.DriverStatus;
import com.nexus.core.model.entities.CapacityForecast;
import com.nexus.core.model.entities.FleetAsset;
import com.nexus.core.model.enums.FleetAssetStatus;
import com.nexus.core.model.enums.IncidentStatus;
import com.nexus.core.model.enums.IncidentType;
import com.nexus.core.model.entities.ProofOfDelivery;
import com.nexus.core.model.entities.Shipment;
import com.nexus.core.model.entities.ShipmentDocument;
import com.nexus.core.model.enums.ShipmentDocumentType;
import com.nexus.core.model.entities.ShipmentIncident;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.model.entities.TrackingEvent;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ProofOfDeliveryDto;
import com.nexus.core.payload.ShipmentIncidentDto;
import com.nexus.core.repository.DriverRepo;
import com.nexus.core.repository.CapacityForecastRepo;
import com.nexus.core.repository.FleetAssetRepo;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.ProofOfDeliveryRepo;
import com.nexus.core.repository.ShipmentDocumentRepo;
import com.nexus.core.repository.ShipmentIncidentRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.TrackingEventRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.interfaces.LogisticsExecutionService;
import com.nexus.core.service.interfaces.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogisticsExecutionServiceImpl implements LogisticsExecutionService {

    private final ShipmentRepo shipmentRepo;
    private final PurchaseOrderRepo purchaseOrderRepo;
    private final CapacityForecastRepo capacityForecastRepo;
    private final DriverRepo driverRepo;
    private final FleetAssetRepo assetRepo;
    private final ProofOfDeliveryRepo podRepo;
    private final ShipmentIncidentRepo incidentRepo;
    private final TrackingEventRepo trackingEventRepo;
    private final ShipmentDocumentRepo documentRepo;
    private final ShipmentService shipmentService;
    private final ModelMapper modelMapper;

    private static final Map<IncidentStatus, Set<IncidentStatus>> INCIDENT_ALLOWED = Map.of(
            IncidentStatus.OPEN, Set.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
            IncidentStatus.IN_PROGRESS, Set.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
            IncidentStatus.RESOLVED, Set.of(IncidentStatus.CLOSED),
            IncidentStatus.CLOSED, Set.of());

    // Simulation defaults (Mumbai hub) until live telematics lands
    private static final double DEFAULT_LAT = 19.0760;
    private static final double DEFAULT_LNG = 72.8777;
    private static final double ARRIVAL_THRESHOLD_KM = 2.0;
    private final java.util.Random random = new java.util.Random();

    @Override
    @Transactional
    public ResponseEntity<?> assignBooking(Long shipmentId, Map<String, Object> assignment) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
        if (shipment.getLogisticsOrg() == null || !shipment.getLogisticsOrg().getAccountId().equals(orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Shipment does not belong to organization"));
        }
        if (shipment.getStatus() != ShipmentStatus.BOOKED && shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only BOOKED shipments can be assigned"));
        }
        Long driverId = toLong(assignment != null ? assignment.get("driverId") : null);
        Long assetId = toLong(assignment != null ? assignment.get("assetId") : null);
        if (driverId != null) {
            Driver driver = driverRepo.findByDriverIdAndLogisticsOrgAccountId(driverId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Driver", "driverId", driverId));
            if (driver.getStatus() != DriverStatus.AVAILABLE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Driver is not available"));
            }
            driver.setStatus(DriverStatus.ASSIGNED);
            driverRepo.save(driver);
            shipment.setCarrierName(driver.getFullName());
            shipment.setAssignedDriverId(driverId);
        }
        if (assetId != null) {
            FleetAsset asset = assetRepo.findByAssetIdAndLogisticsOrgAccountId(assetId, orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("FleetAsset", "assetId", assetId));
            if (asset.getStatus() != FleetAssetStatus.AVAILABLE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Asset is not available"));
            }
            asset.setStatus(FleetAssetStatus.ASSIGNED);
            assetRepo.save(asset);
            shipment.setCarrierReference(asset.getAssetNumber());
            shipment.setAssignedAssetId(assetId);
        }
        if (assignment != null && assignment.get("bolDocumentId") != null) {
            var doc = new ShipmentDocument();
            doc.setShipment(shipment);
            doc.setDocumentType(ShipmentDocumentType.BILL_OF_LADING);
            doc.setDocumentName("BOL-" + shipment.getShipmentNumber());
            doc.setDmsDocumentId(String.valueOf(assignment.get("bolDocumentId")));
            documentRepo.save(doc);
        }
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        Shipment saved = shipmentRepo.save(shipment);

        // Optional immediate pickup: the carrier collects the goods right
        // away. Actual pickup is stamped, the shipment moves to PICKED_UP
        // (which also flips the linked PO), and the expected delivery is
        // computed from the route capacity's average delivery time.
        boolean startPickup = assignment != null && Boolean.TRUE.equals(toBoolean(assignment.get("startPickup")));
        Timestamp pickupAt = null;
        Timestamp expectedAt = null;
        if (startPickup) {
            pickupAt = Timestamp.valueOf(LocalDateTime.now());
            saved.setActualDeparture(pickupAt);
            shipmentRepo.save(saved);
            shipmentService.transitionShipmentStatus(saved.getShipmentId(), ShipmentStatus.PICKED_UP, Map.of());
            expectedAt = computeExpectedDelivery(saved);
            if (expectedAt != null) {
                saved.setEstimatedArrival(expectedAt);
                shipmentRepo.save(saved);
                final Timestamp finalExpectedAt = expectedAt;
                if (saved.getPurchaseOrder() != null && saved.getPurchaseOrder().getPurchaseOrderId() != null) {
                    purchaseOrderRepo.findById(saved.getPurchaseOrder().getPurchaseOrderId()).ifPresent(po -> {
                        po.setExpectedDeliveryDate(new java.sql.Date(finalExpectedAt.getTime()));
                        purchaseOrderRepo.save(po);
                    });
                }
            }
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("shipmentId", saved.getShipmentId());
        resp.put("shipmentNumber", saved.getShipmentNumber());
        resp.put("status", startPickup ? ShipmentStatus.PICKED_UP.name() : ShipmentStatus.ASSIGNED.name());
        resp.put("actualDeparture", pickupAt != null ? pickupAt.toString() : null);
        resp.put("estimatedArrival", expectedAt != null ? expectedAt.toString() : null);
        return ResponseEntity.ok(resp);
    }

    private static Boolean toBoolean(Object v) {
        if (v instanceof Boolean b) return b;
        if (v == null) return null;
        return Boolean.parseBoolean(v.toString());
    }

    private static String normLane(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static boolean laneCovers(String lane, String place) {
        String a = normLane(lane);
        String b = normLane(place);
        return !a.isEmpty() && !b.isEmpty() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    // Expected delivery = pickup moment + the matched route capacity's
    // average delivery time. Prefers this org's own lanes (private
    // included), else any public lane covering the shipment's route.
    private Timestamp computeExpectedDelivery(Shipment shipment) {
        String pickup = shipment.getPickupLocation() != null
                ? shipment.getPickupLocation() : shipment.getPickupAddress();
        String delivery = shipment.getDeliveryLocation() != null
                ? shipment.getDeliveryLocation() : shipment.getDeliveryAddress();
        List<CapacityForecast> candidates = null;
        if (shipment.getLogisticsOrg() != null && shipment.getLogisticsOrg().getAccountId() != null) {
            candidates = capacityForecastRepo.findByOrgWithFilters(
                    shipment.getLogisticsOrg().getAccountId(), null, null,
                    org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        if (candidates == null || candidates.isEmpty()) {
            candidates = capacityForecastRepo.findMarketplaceAvailabilities(
                    null, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }
        LocalDateTime base = shipment.getActualDeparture() != null
                ? shipment.getActualDeparture().toLocalDateTime() : LocalDateTime.now();
        for (CapacityForecast c : candidates) {
            if (c.getAverageDeliveryTime() == null || c.getAverageDeliveryTime() <= 0) continue;
            if (!laneCovers(c.getOriginLane(), pickup) || !laneCovers(c.getDestinationLane(), delivery)) continue;
            long amount = Math.round(c.getAverageDeliveryTime());
            LocalDateTime eta = "DAYS".equalsIgnoreCase(c.getDeliveryTimeUom())
                    ? base.plusDays(amount) : base.plusHours(amount);
            return Timestamp.valueOf(eta);
        }
        return null;
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionShipmentStatus(Long shipmentId, String newStatus, Map<String, Object> params) {
        ShipmentStatus target;
        try {
            target = ShipmentStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        try {
            return ResponseEntity.ok(shipmentService.transitionShipmentStatus(shipmentId, target, params));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateShipmentRoute(Long shipmentId, Map<String, Object> route) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
        if (route != null) {
            if (route.get("originLatitude") != null) shipment.setOriginLatitude(toDouble(route.get("originLatitude")));
            if (route.get("originLongitude") != null) shipment.setOriginLongitude(toDouble(route.get("originLongitude")));
            if (route.get("destinationLatitude") != null) shipment.setDestinationLatitude(toDouble(route.get("destinationLatitude")));
            if (route.get("destinationLongitude") != null) shipment.setDestinationLongitude(toDouble(route.get("destinationLongitude")));
        }
        shipmentRepo.save(shipment);
        return getShipmentPosition(shipmentId);
    }

    @Override
    @Transactional
    public ResponseEntity<?> getShipmentPosition(Long shipmentId) {
        Shipment shipment = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));

        FleetAsset asset = null;
        if (shipment.getAssignedAssetId() != null) {
            asset = assetRepo.findById(shipment.getAssignedAssetId()).orElse(null);
        }

        // Ensure a stable route; generate a simulated one when coordinates are missing
        if (shipment.getOriginLatitude() == null || shipment.getOriginLongitude() == null) {
            double[] start = asset != null && asset.getCurrentLatitude() != null
                    ? new double[]{asset.getCurrentLatitude(), asset.getCurrentLongitude()}
                    : new double[]{DEFAULT_LAT, DEFAULT_LNG};
            shipment.setOriginLatitude(start[0]);
            shipment.setOriginLongitude(start[1]);
        }
        if (shipment.getDestinationLatitude() == null || shipment.getDestinationLongitude() == null) {
            double[] dest = randomNearby(shipment.getOriginLatitude(), shipment.getOriginLongitude(), 80, 250);
            shipment.setDestinationLatitude(dest[0]);
            shipment.setDestinationLongitude(dest[1]);
        }
        shipmentRepo.save(shipment);

        double totalKm = haversineKm(shipment.getOriginLatitude(), shipment.getOriginLongitude(),
                shipment.getDestinationLatitude(), shipment.getDestinationLongitude());

        Map<String, Object> position = null;
        double progressPct = 0.0;
        boolean arrived = false;
        if (asset != null) {
            if (asset.getCurrentLatitude() == null || asset.getCurrentLongitude() == null) {
                asset.setCurrentLatitude(shipment.getOriginLatitude());
                asset.setCurrentLongitude(shipment.getOriginLongitude());
            }
            // Simulated GPS tick: advance a random 5-15% of the remaining leg plus jitter
            double remainingKm = haversineKm(asset.getCurrentLatitude(), asset.getCurrentLongitude(),
                    shipment.getDestinationLatitude(), shipment.getDestinationLongitude());
            if (remainingKm <= ARRIVAL_THRESHOLD_KM) {
                asset.setCurrentLatitude(shipment.getDestinationLatitude());
                asset.setCurrentLongitude(shipment.getDestinationLongitude());
                arrived = true;
            } else {
                double step = 0.05 + random.nextDouble() * 0.10;
                double newLat = asset.getCurrentLatitude()
                        + (shipment.getDestinationLatitude() - asset.getCurrentLatitude()) * step
                        + (random.nextDouble() - 0.5) * 0.02;
                double newLng = asset.getCurrentLongitude()
                        + (shipment.getDestinationLongitude() - asset.getCurrentLongitude()) * step
                        + (random.nextDouble() - 0.5) * 0.02;
                asset.setCurrentLatitude(newLat);
                asset.setCurrentLongitude(newLng);
            }
            asset.setLastPositionAt(new Timestamp(System.currentTimeMillis()));
            assetRepo.save(asset);
            double leftKm = haversineKm(asset.getCurrentLatitude(), asset.getCurrentLongitude(),
                    shipment.getDestinationLatitude(), shipment.getDestinationLongitude());
            progressPct = totalKm <= 0 ? 100.0 : Math.min(100.0, Math.max(0.0, (1 - leftKm / totalKm) * 100.0));
            if (leftKm <= ARRIVAL_THRESHOLD_KM) arrived = true;
            position = Map.of(
                    "latitude", asset.getCurrentLatitude(),
                    "longitude", asset.getCurrentLongitude(),
                    "at", asset.getLastPositionAt(),
                    "simulated", true);
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("shipmentId", shipmentId);
        body.put("status", shipment.getStatus());
        body.put("origin", Map.of("latitude", shipment.getOriginLatitude(), "longitude", shipment.getOriginLongitude()));
        body.put("destination", Map.of("latitude", shipment.getDestinationLatitude(), "longitude", shipment.getDestinationLongitude()));
        body.put("assetId", asset != null ? asset.getAssetId() : null);
        body.put("assetNumber", asset != null ? asset.getAssetNumber() : shipment.getCarrierReference());
        body.put("position", position);
        body.put("progressPct", Math.round(progressPct * 10.0) / 10.0);
        body.put("arrived", arrived);
        body.put("simulated", true);
        return ResponseEntity.ok(body);
    }

    private double[] randomNearby(double lat, double lng, double minKm, double maxKm) {
        double bearing = random.nextDouble() * 2 * Math.PI;
        double distKm = minKm + random.nextDouble() * (maxKm - minKm);
        double angular = distKm / 6371.0;
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);
        double newLatRad = Math.asin(Math.sin(latRad) * Math.cos(angular)
                + Math.cos(latRad) * Math.sin(angular) * Math.cos(bearing));
        double newLngRad = lngRad + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(latRad),
                Math.cos(angular) - Math.sin(latRad) * Math.sin(newLatRad));
        return new double[]{Math.toDegrees(newLatRad), Math.toDegrees(newLngRad)};
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static Double toDouble(Object value) {
        if (value == null) return null;
        return Double.valueOf(String.valueOf(value));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getShipmentEta(Long shipmentId) {
        shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", shipmentId));
        List<TrackingEvent> events = trackingEventRepo.findAll().stream()
                .filter(e -> e.getShipment() != null && e.getShipment().getShipmentId().equals(shipmentId))
                .sorted((a, b) -> b.getEventTimestamp().compareTo(a.getEventTimestamp()))
                .toList();
        TrackingEventDto latest = events.isEmpty() ? null : modelMapper.map(events.get(0), TrackingEventDto.class);
        var shipment = shipmentRepo.findById(shipmentId).orElseThrow();
        var eta = new LinkedHashMap<String, Object>();
        eta.put("shipmentId", shipmentId);
        eta.put("status", shipment.getStatus());
        eta.put("estimatedArrival", shipment.getEstimatedArrival());
        eta.put("latestEvent", latest);
        eta.put("totalMilestones", events.size());
        boolean delayed = shipment.getEstimatedArrival() != null
                && shipment.getEstimatedArrival().before(new Timestamp(System.currentTimeMillis()))
                && shipment.getStatus() != ShipmentStatus.DELIVERED;
        eta.put("delayed", delayed);
        return ResponseEntity.ok(eta);
    }

    @Override
    @Transactional
    public ResponseEntity<?> capturePod(ProofOfDeliveryDto dto) {
        Shipment shipment = shipmentRepo.findById(dto.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
        if (podRepo.findByShipmentId(dto.getShipmentId()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "POD already captured for shipment"));
        }
        var pod = modelMapper.map(dto, ProofOfDelivery.class);
        pod.setPodId(null);
        pod.setShipment(shipment);
        if (pod.getDeliveredAt() == null) pod.setDeliveredAt(new Timestamp(System.currentTimeMillis()));
        var saved = podRepo.save(pod);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setActualArrival(saved.getDeliveredAt());
        shipmentRepo.save(shipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPodDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updatePod(Long id, ProofOfDeliveryDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = podRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", "podId", id));
        if (dto.getReceivedBy() != null) existing.setReceivedBy(dto.getReceivedBy());
        if (dto.getSignature() != null) existing.setSignature(dto.getSignature());
        if (dto.getPhotoUrls() != null) existing.setPhotoUrls(dto.getPhotoUrls());
        if (dto.getDeliveredAt() != null) existing.setDeliveredAt(dto.getDeliveredAt());
        if (dto.getLatitude() != null) existing.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) existing.setLongitude(dto.getLongitude());
        if (dto.getDmsDocumentId() != null) existing.setDmsDocumentId(dto.getDmsDocumentId());
        if (dto.getConditionNotes() != null) existing.setConditionNotes(dto.getConditionNotes());
        if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
        return ResponseEntity.ok(toPodDto(podRepo.save(existing)));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPodByShipment(Long shipmentId) {
        var pod = podRepo.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", "shipmentId", shipmentId));
        return ResponseEntity.ok(toPodDto(pod));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllPods(String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        Page<ProofOfDelivery> page = podRepo.findByOrgWithFilters(orgId, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(this::toPodDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> reportIncident(ShipmentIncidentDto dto) {
        Shipment shipment = shipmentRepo.findById(dto.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
        var incident = modelMapper.map(dto, ShipmentIncident.class);
        incident.setIncidentId(null);
        incident.setShipment(shipment);
        incident.setIncidentNumber("INC-" + System.currentTimeMillis());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setReportedAt(new Timestamp(System.currentTimeMillis()));
        var saved = incidentRepo.save(incident);
        if (shipment.getStatus() == ShipmentStatus.PICKED_UP || shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
            shipment.setStatus(ShipmentStatus.EXCEPTION);
            shipmentRepo.save(shipment);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toIncidentDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getIncident(Long id) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var incident = incidentRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentIncident", "incidentId", id));
        return ResponseEntity.ok(toIncidentDto(incident));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllIncidents(Long shipmentId, String incidentType, String status, String search, Pageable pageable) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        IncidentType type = null;
        IncidentStatus st = null;
        try {
            if (incidentType != null && !incidentType.isBlank()) type = IncidentType.valueOf(incidentType.toUpperCase());
            if (status != null && !status.isBlank()) st = IncidentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid filter value"));
        }
        Page<ShipmentIncident> page = incidentRepo.findByOrgWithFilters(orgId, shipmentId, type, st, blankToNull(search), pageable);
        return ResponseEntity.ok(page.map(this::toIncidentDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateIncident(Long id, ShipmentIncidentDto dto) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var existing = incidentRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentIncident", "incidentId", id));
        if (existing.getStatus() == IncidentStatus.RESOLVED || existing.getStatus() == IncidentStatus.CLOSED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Resolved or closed incidents cannot be edited"));
        }
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getClaimAmount() != null) existing.setClaimAmount(dto.getClaimAmount());
        if (dto.getDmsDocumentId() != null) existing.setDmsDocumentId(dto.getDmsDocumentId());
        if (dto.getNotes() != null) existing.setNotes(dto.getNotes());
        return ResponseEntity.ok(toIncidentDto(incidentRepo.save(existing)));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionIncidentStatus(Long id, String newStatus, Map<String, Object> params) {
        var orgId = OrganizationContextHolder.requireOrganizationId();
        var incident = incidentRepo.findByIdAndOrg(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ShipmentIncident", "incidentId", id));
        IncidentStatus target;
        try {
            target = IncidentStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus));
        }
        if (!INCIDENT_ALLOWED.getOrDefault(incident.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid transition: " + incident.getStatus() + " -> " + target));
        }
        incident.setStatus(target);
        if (target == IncidentStatus.RESOLVED || target == IncidentStatus.CLOSED) {
            incident.setResolvedAt(new Timestamp(System.currentTimeMillis()));
            var shipment = incident.getShipment();
            if (shipment != null && shipment.getStatus() == ShipmentStatus.EXCEPTION) {
                shipment.setStatus(ShipmentStatus.RESOLVED);
                shipmentRepo.save(shipment);
            }
        }
        if (params != null && params.get("notes") != null) incident.setNotes(String.valueOf(params.get("notes")));
        return ResponseEntity.ok(toIncidentDto(incidentRepo.save(incident)));
    }

    private ProofOfDeliveryDto toPodDto(ProofOfDelivery pod) {
        var dto = modelMapper.map(pod, ProofOfDeliveryDto.class);
        if (pod.getShipment() != null) dto.setShipmentId(pod.getShipment().getShipmentId());
        return dto;
    }

    private ShipmentIncidentDto toIncidentDto(ShipmentIncident incident) {
        var dto = modelMapper.map(incident, ShipmentIncidentDto.class);
        if (incident.getShipment() != null) dto.setShipmentId(incident.getShipment().getShipmentId());
        return dto;
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
