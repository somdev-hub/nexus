package com.nexus.core.controller;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.ProofOfDeliveryDto;
import com.nexus.core.payload.ShipmentIncidentDto;
import com.nexus.core.service.LogisticsExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/core/logistics/execution")
@RequiredArgsConstructor
public class LogisticsExecutionController {

    private final LogisticsExecutionService executionService;

    @PutMapping("/shipments/{shipmentId}/assign")
    @LogActivity("Assign Logistics Booking")
    public ResponseEntity<?> assignBooking(@PathVariable Long shipmentId,
            @RequestBody(required = false) Map<String, Object> assignment) {
        return executionService.assignBooking(shipmentId, assignment);
    }

    @PutMapping("/shipments/{shipmentId}/status")
    @LogActivity("Transition Logistics Shipment Status")
    public ResponseEntity<?> transitionShipmentStatus(@PathVariable Long shipmentId,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return executionService.transitionShipmentStatus(shipmentId, newStatus, params);
    }

    @GetMapping("/shipments/{shipmentId}/position")
    @LogActivity("Get Shipment Live Position")
    public ResponseEntity<?> getShipmentPosition(@PathVariable Long shipmentId) {
        return executionService.getShipmentPosition(shipmentId);
    }

    @PutMapping("/shipments/{shipmentId}/route")
    @LogActivity("Update Shipment Route Coordinates")
    public ResponseEntity<?> updateShipmentRoute(@PathVariable Long shipmentId,
            @RequestBody(required = false) Map<String, Object> route) {
        return executionService.updateShipmentRoute(shipmentId, route);
    }

    @GetMapping("/shipments/{shipmentId}/eta")
    @LogActivity("Get Shipment ETA")
    public ResponseEntity<?> getShipmentEta(@PathVariable Long shipmentId) {
        return executionService.getShipmentEta(shipmentId);
    }

    @PostMapping("/pod/capture")
    @LogActivity("Capture Proof of Delivery")
    public ResponseEntity<?> capturePod(@Valid @RequestBody ProofOfDeliveryDto dto) {
        return executionService.capturePod(dto);
    }

    @GetMapping("/pod/shipment/{shipmentId}")
    @LogActivity("Get POD by Shipment")
    public ResponseEntity<?> getPodByShipment(@PathVariable Long shipmentId) {
        return executionService.getPodByShipment(shipmentId);
    }

    @GetMapping("/pod/all")
    @LogActivity("Get All PODs")
    public ResponseEntity<?> getAllPods(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return executionService.getAllPods(search, pageable);
    }

    @PostMapping("/incidents/report")
    @LogActivity("Report Shipment Incident")
    public ResponseEntity<?> reportIncident(@Valid @RequestBody ShipmentIncidentDto dto) {
        return executionService.reportIncident(dto);
    }

    @GetMapping("/incidents/{id}")
    @LogActivity("Get Shipment Incident")
    public ResponseEntity<?> getIncident(@PathVariable Long id) {
        return executionService.getIncident(id);
    }

    @GetMapping("/incidents/all")
    @LogActivity("Get All Shipment Incidents")
    public ResponseEntity<?> getAllIncidents(
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) String incidentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return executionService.getAllIncidents(shipmentId, incidentType, status, search, pageable);
    }

    @PutMapping("/incidents/{id}/status")
    @LogActivity("Transition Shipment Incident Status")
    public ResponseEntity<?> transitionIncidentStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return executionService.transitionIncidentStatus(id, newStatus, params);
    }
}
