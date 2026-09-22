package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.DeliveryAppointmentDto;

import java.util.Map;

public interface DeliveryAppointmentService {

    ResponseEntity<?> createAppointment(DeliveryAppointmentDto dto);

    ResponseEntity<?> getAppointment(Long id);

    ResponseEntity<?> getAllAppointments(String status, Long warehouseId, Long shipmentId,
            java.sql.Timestamp fromDate, java.sql.Timestamp toDate, Pageable pageable);

    ResponseEntity<?> updateAppointment(Long id, DeliveryAppointmentDto dto);

    ResponseEntity<?> transitionStatus(Long id, String newStatus, Map<String, Object> params);

    ResponseEntity<?> deleteAppointment(Long id);

    ResponseEntity<?> getAppointmentSummary();
}
