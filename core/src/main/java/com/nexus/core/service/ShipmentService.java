package com.nexus.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.dto.ShipmentStopDto;
import com.nexus.core.dto.TrackingEventDto;
import com.nexus.core.dto.ShipmentDocumentDto;
import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.ShipmentMode;
import com.nexus.core.entities.ShipmentStatus;

public interface ShipmentService {

	// Shipment CRUD operations
	ShipmentDto createShipment(ShipmentDto shipmentDto);

	Optional<ShipmentDto> getShipmentById(Long shipmentId);

	Page<ShipmentDto> getAllShipments(Pageable pageable);

	Page<ShipmentDto> getShipmentsByStatus(ShipmentStatus status, Pageable pageable);

	Page<ShipmentDto> getShipmentsByMode(ShipmentMode mode, Pageable pageable);

	Page<ShipmentDto> getShipmentsByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
			Pageable pageable);

	Page<ShipmentDto> getShipmentsBySupplier(Long supplierId, Pageable pageable);

	Page<ShipmentDto> getShipmentsByWarehouse(Long warehouseId, Pageable pageable);

	ShipmentDto updateShipment(Long shipmentId, ShipmentDto shipmentDto);

	void deleteShipment(Long shipmentId);

	// Shipment status transitions
	// Consolidated status transition
	ShipmentDto transitionShipmentStatus(Long shipmentId, ShipmentStatus newStatus, Map<String, Object> params);

	// Shipment stops management
	ShipmentStopDto addStopToShipment(Long shipmentId, ShipmentStopDto stopDto);

	ShipmentStopDto updateStop(Long stopId, ShipmentStopDto stopDto);

	void removeStop(Long stopId);

	List<ShipmentStopDto> getStopsByShipment(Long shipmentId);

	// Consolidated stop status transition
	ShipmentStopDto transitionStopStatus(Long stopId, com.nexus.core.entities.StopStatus newStatus,
			Map<String, Object> params);

	// Tracking events
	TrackingEventDto addTrackingEvent(Long shipmentId, TrackingEventDto eventDto);

	List<TrackingEventDto> getTrackingEventsByShipment(Long shipmentId);

	TrackingEventDto getLatestTrackingEvent(Long shipmentId);

	// Documents
	ShipmentDocumentDto addDocument(Long shipmentId, ShipmentDocumentDto documentDto);

	List<ShipmentDocumentDto> getDocumentsByShipment(Long shipmentId);

	List<ShipmentDocumentDto> getDocumentsByShipmentAndType(Long shipmentId,
			com.nexus.core.entities.ShipmentDocumentType documentType);

	void deleteDocument(Long documentId);

	// Freight cost management (Consolidated)
	ShipmentDto updateFreightCost(Long shipmentId, java.math.BigDecimal estimatedCost, java.math.BigDecimal actualCost,
			java.util.Currency currency);

	// Search and filtering
	Page<ShipmentDto> searchShipments(String query, Pageable pageable);

	List<ShipmentDto> getShipmentsRequiringAttention();

	List<ShipmentDto> getOverdueShipments();
}