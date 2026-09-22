package com.nexus.core.service.impl;

import com.nexus.core.entities.Account;
import com.nexus.core.entities.DeliveryAppointment;
import com.nexus.core.entities.DeliveryAppointmentStatus;
import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.DeliveryAppointmentDto;
import com.nexus.core.repository.AccountRepository;
import com.nexus.core.repository.DeliveryAppointmentRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.DeliveryAppointmentService;
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
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAppointmentServiceImpl implements DeliveryAppointmentService {

    private final DeliveryAppointmentRepo appointmentRepo;
    private final WarehouseRepo warehouseRepo;
    private final ShipmentRepo shipmentRepo;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    private static final Map<DeliveryAppointmentStatus, Set<DeliveryAppointmentStatus>> ALLOWED = Map.of(
            DeliveryAppointmentStatus.SCHEDULED, Set.of(DeliveryAppointmentStatus.CONFIRMED, DeliveryAppointmentStatus.CANCELLED, DeliveryAppointmentStatus.RESCHEDULED),
            DeliveryAppointmentStatus.CONFIRMED, Set.of(DeliveryAppointmentStatus.IN_PROGRESS, DeliveryAppointmentStatus.CANCELLED, DeliveryAppointmentStatus.RESCHEDULED),
            DeliveryAppointmentStatus.IN_PROGRESS, Set.of(DeliveryAppointmentStatus.COMPLETED, DeliveryAppointmentStatus.MISSED),
            DeliveryAppointmentStatus.RESCHEDULED, Set.of(DeliveryAppointmentStatus.SCHEDULED, DeliveryAppointmentStatus.CANCELLED),
            DeliveryAppointmentStatus.COMPLETED, Set.of(),
            DeliveryAppointmentStatus.CANCELLED, Set.of(),
            DeliveryAppointmentStatus.MISSED, Set.of(DeliveryAppointmentStatus.RESCHEDULED)
    );

    @Override
    @Transactional
    public ResponseEntity<?> createAppointment(DeliveryAppointmentDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        Warehouse warehouse = warehouseRepo.findById(dto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", dto.getWarehouseId()));
        if (!warehouse.getOrg().equals(orgId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Warehouse does not belong to organization"));
        }
        Shipment shipment = null;
        if (dto.getShipmentId() != null) {
            shipment = shipmentRepo.findById(dto.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", "shipmentId", dto.getShipmentId()));
        }
        Account retailerOrg = accountRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountId", orgId));

        // Validate time window
        if (dto.getScheduledStart() != null && dto.getScheduledEnd() != null && dto.getScheduledStart().after(dto.getScheduledEnd())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Scheduled start must be before end"));
        }

        DeliveryAppointment ap = modelMapper.map(dto, DeliveryAppointment.class);
        ap.setAppointmentId(null);
        ap.setAppointmentNumber("DA-" + System.currentTimeMillis());
        ap.setWarehouse(warehouse);
        ap.setShipment(shipment);
        ap.setRetailerOrg(retailerOrg);
        ap.setStatus(DeliveryAppointmentStatus.SCHEDULED);
        DeliveryAppointment saved = appointmentRepo.save(ap);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAppointment(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        DeliveryAppointment ap = appointmentRepo.findByAppointmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAppointment", "appointmentId", id));
        return ResponseEntity.ok(mapToDto(ap));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllAppointments(String status, Long warehouseId, Long shipmentId, Timestamp fromDate, Timestamp toDate, Pageable pageable) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        DeliveryAppointmentStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = DeliveryAppointmentStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status)); }
        }
        Page<DeliveryAppointment> page = appointmentRepo.findByOrgWithFilters(orgId, st, warehouseId, shipmentId, fromDate, toDate, pageable);
        return ResponseEntity.ok(page.map(this::mapToDto));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateAppointment(Long id, DeliveryAppointmentDto dto) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        DeliveryAppointment ap = appointmentRepo.findByAppointmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAppointment", "appointmentId", id));
        if (ap.getStatus() == DeliveryAppointmentStatus.COMPLETED || ap.getStatus() == DeliveryAppointmentStatus.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot update terminal appointment"));
        }
        if (dto.getScheduledStart() != null) ap.setScheduledStart(dto.getScheduledStart());
        if (dto.getScheduledEnd() != null) ap.setScheduledEnd(dto.getScheduledEnd());
        if (dto.getDockNumber() != null) ap.setDockNumber(dto.getDockNumber());
        if (dto.getContactName() != null) ap.setContactName(dto.getContactName());
        if (dto.getContactPhone() != null) ap.setContactPhone(dto.getContactPhone());
        if (dto.getSpecialInstructions() != null) ap.setSpecialInstructions(dto.getSpecialInstructions());
        if (dto.getNotes() != null) ap.setNotes(dto.getNotes());
        if (dto.getWarehouseId() != null && !dto.getWarehouseId().equals(ap.getWarehouse().getWarehouseId())) {
            Warehouse wh = warehouseRepo.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", dto.getWarehouseId()));
            if (!wh.getOrg().equals(orgId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Warehouse not in org"));
            ap.setWarehouse(wh);
        }
        DeliveryAppointment saved = appointmentRepo.save(ap);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        DeliveryAppointment ap = appointmentRepo.findByAppointmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAppointment", "appointmentId", id));
        DeliveryAppointmentStatus target;
        try { target = DeliveryAppointmentStatus.valueOf(newStatus.toUpperCase()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + newStatus)); }
        if (!ALLOWED.getOrDefault(ap.getStatus(), Set.of()).contains(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transition not allowed: " + ap.getStatus() + " -> " + target));
        }
        ap.setStatus(target);
        if (params != null) {
            if (params.get("actualArrival") != null) ap.setActualArrival(Timestamp.valueOf(params.get("actualArrival").toString()));
            if (params.get("actualDeparture") != null) ap.setActualDeparture(Timestamp.valueOf(params.get("actualDeparture").toString()));
        }
        DeliveryAppointment saved = appointmentRepo.save(ap);
        return ResponseEntity.ok(mapToDto(saved));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteAppointment(Long id) {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        DeliveryAppointment ap = appointmentRepo.findByAppointmentIdAndRetailerOrgAccountId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAppointment", "appointmentId", id));
        if (ap.getStatus() == DeliveryAppointmentStatus.COMPLETED || ap.getStatus() == DeliveryAppointmentStatus.IN_PROGRESS) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete in-progress or completed appointment"));
        }
        appointmentRepo.delete(ap);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAppointmentSummary() {
        Long orgId = OrganizationContextHolder.requireOrganizationId();
        var summary = Map.of(
                "scheduled", appointmentRepo.countByOrgAndStatus(orgId, DeliveryAppointmentStatus.SCHEDULED),
                "confirmed", appointmentRepo.countByOrgAndStatus(orgId, DeliveryAppointmentStatus.CONFIRMED),
                "completed", appointmentRepo.countByOrgAndStatus(orgId, DeliveryAppointmentStatus.COMPLETED),
                "cancelled", appointmentRepo.countByOrgAndStatus(orgId, DeliveryAppointmentStatus.CANCELLED),
                "missed", appointmentRepo.countByOrgAndStatus(orgId, DeliveryAppointmentStatus.MISSED)
        );
        return ResponseEntity.ok(summary);
    }

    private DeliveryAppointmentDto mapToDto(DeliveryAppointment ap) {
        DeliveryAppointmentDto dto = modelMapper.map(ap, DeliveryAppointmentDto.class);
        if (ap.getShipment() != null) {
            dto.setShipmentId(ap.getShipment().getShipmentId());
            dto.setShipmentNumber(ap.getShipment().getShipmentNumber());
        }
        if (ap.getWarehouse() != null) dto.setWarehouseCode(ap.getWarehouse().getCode());
        return dto;
    }
}
