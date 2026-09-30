package com.nexus.core.service.implementations;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.dto.ShipmentStopDto;
import com.nexus.core.dto.TrackingEventDto;
import com.nexus.core.dto.ShipmentDocumentDto;
import com.nexus.core.model.entities.ShipmentDocument;
import com.nexus.core.model.entities.Shipment;import com.nexus.core.model.enums.ShipmentDocumentType;
import com.nexus.core.model.enums.ShipmentMode;
import com.nexus.core.model.enums.ShipmentStatus;
import com.nexus.core.model.entities.ShipmentStop;
import com.nexus.core.model.enums.StopStatus;
import com.nexus.core.model.entities.TrackingEvent;
import com.nexus.core.repository.ShipmentDocumentRepo;
import com.nexus.core.repository.AccountRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.ShipmentStopRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.repository.TrackingEventRepo;
import com.nexus.core.service.interfaces.ShipmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepo shipmentRepo;
    private final ShipmentStopRepo shipmentStopRepo;
    private final TrackingEventRepo trackingEventRepo;
    private final ShipmentDocumentRepo shipmentDocumentRepo;
    private final AccountRepo accountRepo;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ShipmentDto createShipment(ShipmentDto shipmentDto) {
        log.info("Creating new shipment: {}", shipmentDto.getShipmentNumber());
        Shipment shipment = modelMapper.map(shipmentDto, Shipment.class);
        shipment.setStatus(ShipmentStatus.DRAFT);
        shipment.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        shipment.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        if (shipment.getShipmentNumber() == null || shipment.getShipmentNumber().isBlank()) {
            shipment.setShipmentNumber(generateShipmentNumber());
        }
        // Bookable by a logistics partner: bind the logistics org when supplied
        // (missing Account tolerated by leaving logisticsOrg null).
        if (shipmentDto.getLogisticsOrgId() != null) {
            shipment.setLogisticsOrg(
                    accountRepo.findByAccountId(shipmentDto.getLogisticsOrgId()).orElse(null));
        }
        // Booking: a shipment carrying a logistics org leaves DRAFT as BOOKED.
        // DRAFT -> BOOKED is a permitted edge in ALLOWED_TRANSITIONS below.
        // Partner-less shipments keep the default DRAFT behavior.
        if (shipment.getLogisticsOrg() != null && shipment.getStatus() == ShipmentStatus.DRAFT) {
            shipment.setStatus(ShipmentStatus.BOOKED);
        }
        Shipment saved = shipmentRepo.save(shipment);
        return modelMapper.map(saved, ShipmentDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShipmentDto> getShipmentById(Long shipmentId) {
        return shipmentRepo.findById(shipmentId).map(s -> modelMapper.map(s, ShipmentDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getAllShipments(Pageable pageable) {
        return shipmentRepo.findAll(pageable).map(s -> modelMapper.map(s, ShipmentDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsByStatus(ShipmentStatus status, Pageable pageable) {
        var all = shipmentRepo.findAll().stream().filter(s -> s.getStatus() == status).toList();
        var mapped = all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        return new PageImpl<>(mapped, pageable, mapped.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsByMode(ShipmentMode mode, Pageable pageable) {
        var all = shipmentRepo.findAll().stream().filter(s -> s.getShipmentMode() == mode).toList();
        var mapped = all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        return new PageImpl<>(mapped, pageable, mapped.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable) {
        Timestamp start = Timestamp.valueOf(startDate);
        Timestamp end = Timestamp.valueOf(endDate);
        var all = shipmentRepo.findAll().stream()
                .filter(s -> s.getPickupTimeWindowStart() != null && !s.getPickupTimeWindowStart().before(start) && !s.getPickupTimeWindowStart().after(end))
                .toList();
        var mapped = all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        return new PageImpl<>(mapped, pageable, mapped.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsBySupplier(Long supplierId, Pageable pageable) {
        var all = shipmentRepo.findByPurchaseOrderId(supplierId);
        var mapped = all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        return new PageImpl<>(mapped, pageable, mapped.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsByWarehouse(Long warehouseId, Pageable pageable) {
        var all = shipmentRepo.findAll().stream()
                .filter(s -> s.getDeliveryLocation() != null)
                .toList();
        var mapped = all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        return new PageImpl<>(mapped, pageable, mapped.size());
    }

    @Override
    @Transactional
    public ShipmentDto updateShipment(Long shipmentId, ShipmentDto shipmentDto) {
        log.info("Updating shipment: {}", shipmentId);
        Shipment existing = shipmentRepo.findById(shipmentId).orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
        modelMapper.map(shipmentDto, existing);
        existing.setShipmentId(shipmentId);
        existing.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        Shipment saved = shipmentRepo.save(existing);
        return modelMapper.map(saved, ShipmentDto.class);
    }

    @Override
    @Transactional
    public void deleteShipment(Long shipmentId) {
        log.info("Deleting shipment: {}", shipmentId);
        shipmentRepo.deleteById(shipmentId);
    }

    @Override
    @Transactional
    public ShipmentDto transitionShipmentStatus(Long shipmentId, ShipmentStatus newStatus, Map<String, Object> params) {
        String reason = params != null && params.get("reason") != null ? params.get("reason").toString() : "Status transition";
        return transitionStatus(shipmentId, newStatus, reason);
    }

    @Override
    @Transactional
    public ShipmentStopDto addStopToShipment(Long shipmentId, ShipmentStopDto stopDto) {
        log.info("Adding stop to shipment: {}", shipmentId);
        Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
        ShipmentStop stop = modelMapper.map(stopDto, ShipmentStop.class);
        stop.setShipment(shipment);
        stop.setStopStatus(StopStatus.PENDING);
        stop.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        stop.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        ShipmentStop saved = shipmentStopRepo.save(stop);
        return modelMapper.map(saved, ShipmentStopDto.class);
    }

    @Override
    @Transactional
    public ShipmentStopDto updateStop(Long stopId, ShipmentStopDto stopDto) {
        ShipmentStop existing = shipmentStopRepo.findById(stopId).orElseThrow();
        modelMapper.map(stopDto, existing);
        existing.setStopId(stopId);
        existing.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return modelMapper.map(shipmentStopRepo.save(existing), ShipmentStopDto.class);
    }

    @Override
    @Transactional
    public void removeStop(Long stopId) {
        shipmentStopRepo.deleteById(stopId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentStopDto> getStopsByShipment(Long shipmentId) {
        var page = shipmentStopRepo.findByShipmentShipmentId(shipmentId, Pageable.unpaged());
        return page.getContent().stream().map(stop -> modelMapper.map(stop, ShipmentStopDto.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShipmentStopDto transitionStopStatus(Long stopId, StopStatus newStatus, Map<String, Object> params) {
        ShipmentStop stop = shipmentStopRepo.findById(stopId).orElseThrow();
        StopStatus oldStatus = stop.getStopStatus();
        stop.setStopStatus(newStatus);
        String reason = params != null && params.get("reason") != null ? params.get("reason").toString() : "Stop status transition";
        String notes = stop.getNotes() != null ? stop.getNotes() + "\n" : "";
        stop.setNotes(notes + "Status changed: " + oldStatus + " -> " + newStatus + " (" + reason + ")");
        stop.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return modelMapper.map(shipmentStopRepo.save(stop), ShipmentStopDto.class);
    }

    @Override
    @Transactional
    public TrackingEventDto addTrackingEvent(Long shipmentId, TrackingEventDto eventDto) {
        log.info("Adding tracking event to shipment: {}", shipmentId);
        Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
        TrackingEvent event = modelMapper.map(eventDto, TrackingEvent.class);
        event.setShipment(shipment);
        event.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        TrackingEvent saved = trackingEventRepo.save(event);
        return modelMapper.map(saved, TrackingEventDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingEventDto> getTrackingEventsByShipment(Long shipmentId) {
        var all = trackingEventRepo.findAll().stream()
                .filter(e -> e.getShipment() != null && e.getShipment().getShipmentId().equals(shipmentId))
                .sorted((a, b) -> b.getEventTimestamp().compareTo(a.getEventTimestamp()))
                .toList();
        return all.stream().map(e -> modelMapper.map(e, TrackingEventDto.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingEventDto getLatestTrackingEvent(Long shipmentId) {
        var all = trackingEventRepo.findAll().stream()
                .filter(e -> e.getShipment() != null && e.getShipment().getShipmentId().equals(shipmentId))
                .sorted((a, b) -> b.getEventTimestamp().compareTo(a.getEventTimestamp()))
                .findFirst().orElse(null);
        return all != null ? modelMapper.map(all, TrackingEventDto.class) : null;
    }

    @Override
    @Transactional
    public ShipmentDocumentDto addDocument(Long shipmentId, ShipmentDocumentDto documentDto) {
        log.info("Adding document to shipment: {}", shipmentId);
        Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
        ShipmentDocument document = modelMapper.map(documentDto, ShipmentDocument.class);
        document.setShipment(shipment);
        document.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        document.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        ShipmentDocument saved = shipmentDocumentRepo.save(document);
        return modelMapper.map(saved, ShipmentDocumentDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentDocumentDto> getDocumentsByShipment(Long shipmentId) {
        var page = shipmentDocumentRepo.findByShipmentShipmentId(shipmentId, Pageable.unpaged());
        return page.getContent().stream().map(doc -> modelMapper.map(doc, ShipmentDocumentDto.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentDocumentDto> getDocumentsByShipmentAndType(Long shipmentId, ShipmentDocumentType documentType) {
        var page = shipmentDocumentRepo.findByShipmentShipmentIdAndDocumentType(shipmentId, documentType, Pageable.unpaged());
        return page.getContent().stream().map(doc -> modelMapper.map(doc, ShipmentDocumentDto.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        shipmentDocumentRepo.deleteById(documentId);
    }

    @Override
    @Transactional
    public ShipmentDto updateFreightCost(Long shipmentId, BigDecimal estimatedCost, BigDecimal actualCost, Currency currency) {
        Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
        if (estimatedCost != null) shipment.setFreightCost(estimatedCost.doubleValue());
        if (actualCost != null) shipment.setActualFreightCost(actualCost.doubleValue());
        if (currency != null) shipment.setCurrency(currency.getCurrencyCode());
        shipment.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return modelMapper.map(shipmentRepo.save(shipment), ShipmentDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> getShipmentsByLogisticsOrg(Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        return shipmentRepo.findByLogisticsOrgAccountId(orgId, pageable)
                .map(s -> modelMapper.map(s, ShipmentDto.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> searchShipments(String query, Pageable pageable) {
        var all = shipmentRepo.findAll().stream()
                .filter(s -> s.getShipmentNumber() != null && s.getShipmentNumber().contains(query))
                .toList();
        var mapped = all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).toList();
        return new PageImpl<>(mapped, pageable, mapped.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentDto> getShipmentsRequiringAttention() {
        var all = shipmentRepo.findAll().stream().filter(s -> s.getStatus() == ShipmentStatus.EXCEPTION).toList();
        return all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentDto> getOverdueShipments() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        var all = shipmentRepo.findAll().stream()
                .filter(s -> s.getEstimatedArrival() != null && s.getEstimatedArrival().before(now) && s.getStatus() != ShipmentStatus.DELIVERED)
                .toList();
        return all.stream().map(s -> modelMapper.map(s, ShipmentDto.class)).collect(Collectors.toList());
    }

    private static final Map<ShipmentStatus, java.util.Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry(ShipmentStatus.DRAFT, java.util.Set.of(ShipmentStatus.PENDING_APPROVAL, ShipmentStatus.BOOKED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.PENDING_APPROVAL, java.util.Set.of(ShipmentStatus.APPROVED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.APPROVED, java.util.Set.of(ShipmentStatus.BOOKED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.BOOKED, java.util.Set.of(ShipmentStatus.ASSIGNED, ShipmentStatus.EXCEPTION, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.ASSIGNED, java.util.Set.of(ShipmentStatus.PICKED_UP, ShipmentStatus.EXCEPTION, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.PICKED_UP, java.util.Set.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.EXCEPTION)),
            Map.entry(ShipmentStatus.IN_TRANSIT, java.util.Set.of(ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.PARTIALLY_DELIVERED, ShipmentStatus.EXCEPTION)),
            Map.entry(ShipmentStatus.OUT_FOR_DELIVERY, java.util.Set.of(ShipmentStatus.DELIVERED, ShipmentStatus.PARTIALLY_DELIVERED, ShipmentStatus.EXCEPTION)),
            Map.entry(ShipmentStatus.EXCEPTION, java.util.Set.of(ShipmentStatus.RESOLVED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.RESOLVED, java.util.Set.of(ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT)),
            Map.entry(ShipmentStatus.DELIVERED, java.util.Set.of(ShipmentStatus.CLOSED)),
            Map.entry(ShipmentStatus.PARTIALLY_DELIVERED, java.util.Set.of(ShipmentStatus.CLOSED)),
            Map.entry(ShipmentStatus.CANCELLED, java.util.Set.of()),
            Map.entry(ShipmentStatus.CLOSED, java.util.Set.of()));

    private ShipmentDto transitionStatus(Long shipmentId, ShipmentStatus newStatus, String reason) {
        Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
        ShipmentStatus oldStatus = shipment.getStatus();
        if (oldStatus != newStatus && !ALLOWED_TRANSITIONS.getOrDefault(oldStatus, java.util.Set.of()).contains(newStatus)) {
            throw new IllegalArgumentException("Invalid shipment transition: " + oldStatus + " -> " + newStatus);
        }
        shipment.setStatus(newStatus);
        String notes = shipment.getSpecialInstructions() != null ? shipment.getSpecialInstructions() + "\n" : "";
        shipment.setSpecialInstructions(notes + "Status changed: " + oldStatus + " -> " + newStatus + " (" + reason + ")");
        shipment.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return modelMapper.map(shipmentRepo.save(shipment), ShipmentDto.class);
    }

    private String generateShipmentNumber() {
        return "SHP-" + System.currentTimeMillis();
    }
}
