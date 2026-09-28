package com.nexus.core.service.interfaces;

import com.nexus.core.payload.ProofOfDeliveryDto;
import com.nexus.core.payload.ShipmentIncidentDto;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface LogisticsExecutionService {

    // Booking confirmation: assign driver/asset + BOL document reference (FR-LOG-003)
    ResponseEntity<?> assignBooking(Long shipmentId, Map<String, Object> assignment);

    // Shipment lifecycle transition (SRS-04 state machine, consolidated)
    ResponseEntity<?> transitionShipmentStatus(Long shipmentId, String newStatus, Map<String, Object> params);

    // Tracking + ETA (FR-LOG-020)
    ResponseEntity<?> getShipmentEta(Long shipmentId);

    // Live map position with simulated GPS tick (telematics placeholder)
    ResponseEntity<?> getShipmentPosition(Long shipmentId);
    ResponseEntity<?> updateShipmentRoute(Long shipmentId, Map<String, Object> route);

    // POD (FR-LOG-021 consolidated)
    ResponseEntity<?> capturePod(ProofOfDeliveryDto dto);
    ResponseEntity<?> updatePod(Long id, ProofOfDeliveryDto dto);
    ResponseEntity<?> getPodByShipment(Long shipmentId);
    ResponseEntity<?> getAllPods(String search, Pageable pageable);

    // Exceptions/claims (FR-LOG-022 consolidated: delay/reroute/damage/loss/claim in one status API)
    ResponseEntity<?> reportIncident(ShipmentIncidentDto dto);
    ResponseEntity<?> updateIncident(Long id, ShipmentIncidentDto dto);
    ResponseEntity<?> getIncident(Long id);
    ResponseEntity<?> getAllIncidents(Long shipmentId, String incidentType, String status, String search, Pageable pageable);
    ResponseEntity<?> transitionIncidentStatus(Long id, String newStatus, Map<String, Object> params);
}
