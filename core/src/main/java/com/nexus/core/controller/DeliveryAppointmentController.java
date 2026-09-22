package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.DeliveryAppointmentDto;
import com.nexus.core.service.DeliveryAppointmentService;

import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.util.Map;

@RestController
@RequestMapping("/core/delivery-appointments")
@RequiredArgsConstructor
public class DeliveryAppointmentController {

    private final DeliveryAppointmentService deliveryAppointmentService;

    @PostMapping("/create")
    @LogActivity("Create Delivery Appointment")
    public ResponseEntity<?> createAppointment(@RequestBody DeliveryAppointmentDto dto) {
        return deliveryAppointmentService.createAppointment(dto);
    }

    @GetMapping("/{id}")
    @LogActivity("Get Delivery Appointment")
    public ResponseEntity<?> getAppointment(@PathVariable Long id) {
        return deliveryAppointmentService.getAppointment(id);
    }

    /**
     * Consolidated GET all – single endpoint with filters (FR-RET-034).
     */
    @GetMapping("/all")
    @LogActivity("Get All Delivery Appointments")
    public ResponseEntity<?> getAllAppointments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) Timestamp fromDate,
            @RequestParam(required = false) Timestamp toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return deliveryAppointmentService.getAllAppointments(status, warehouseId, shipmentId, fromDate, toDate, pageable);
    }

    @PutMapping("/{id}/update")
    @LogActivity("Update Delivery Appointment")
    public ResponseEntity<?> updateAppointment(@PathVariable Long id, @RequestBody DeliveryAppointmentDto dto) {
        return deliveryAppointmentService.updateAppointment(id, dto);
    }

    @PutMapping("/{id}/status")
    @LogActivity("Transition Delivery Appointment Status")
    public ResponseEntity<?> transitionStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return deliveryAppointmentService.transitionStatus(id, newStatus, params);
    }

    @DeleteMapping("/{id}")
    @LogActivity("Delete Delivery Appointment")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        return deliveryAppointmentService.deleteAppointment(id);
    }

    @GetMapping("/summary")
    @LogActivity("Get Delivery Appointment Summary")
    public ResponseEntity<?> getSummary() {
        return deliveryAppointmentService.getAppointmentSummary();
    }
}
