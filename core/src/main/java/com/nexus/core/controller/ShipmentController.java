package com.nexus.core.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.dto.ShipmentStopDto;
import com.nexus.core.dto.TrackingEventDto;
import com.nexus.core.dto.ShipmentDocumentDto;
import com.nexus.core.entities.ShipmentMode;
import com.nexus.core.entities.ShipmentStatus;
import com.nexus.core.service.ShipmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/shipments")
@RequiredArgsConstructor
public class ShipmentController {

	private final ShipmentService shipmentService;

	@PostMapping("/create")
	@LogActivity("Create Shipment")
	public ResponseEntity<?> createShipment(@Valid @RequestBody ShipmentDto shipmentDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.createShipment(shipmentDto));
	}

	@GetMapping("/{id}")
	@LogActivity("Get Shipment")
	public ResponseEntity<?> getShipment(@PathVariable Long id) {
		return shipmentService.getShipmentById(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/all")
	@LogActivity("Get All Shipments")
	public ResponseEntity<?> getAllShipments(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String mode,
			@RequestParam(required = false) Long supplierId,
			@RequestParam(required = false) Long warehouseId,
			@PageableDefault(size = 20) Pageable pageable) {

		if (status != null) {
			try {
				ShipmentStatus shipmentStatus = ShipmentStatus.valueOf(status.toUpperCase());
				return ResponseEntity.ok(shipmentService.getShipmentsByStatus(shipmentStatus, pageable));
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
			}
		}

		if (mode != null) {
			try {
				ShipmentMode shipmentMode = ShipmentMode.valueOf(mode.toUpperCase());
				return ResponseEntity.ok(shipmentService.getShipmentsByMode(shipmentMode, pageable));
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid mode: " + mode);
			}
		}

		if (supplierId != null) {
			return ResponseEntity.ok(shipmentService.getShipmentsBySupplier(supplierId, pageable));
		}

		if (warehouseId != null) {
			return ResponseEntity.ok(shipmentService.getShipmentsByWarehouse(warehouseId, pageable));
		}

		return ResponseEntity.ok(shipmentService.getAllShipments(pageable));
	}

	@PutMapping("/{id}/update")
	@LogActivity("Update Shipment")
	public ResponseEntity<?> updateShipment(@PathVariable Long id, @RequestBody ShipmentDto shipmentDto) {
		return ResponseEntity.ok(shipmentService.updateShipment(id, shipmentDto));
	}

	@DeleteMapping("/{id}")
	@LogActivity("Delete Shipment")
	public ResponseEntity<?> deleteShipment(@PathVariable Long id) {
		shipmentService.deleteShipment(id);
		return ResponseEntity.noContent().build();
	}

	// Consolidated Status Transition
	@PutMapping("/{id}/status")
	@LogActivity("Transition Shipment Status")
	public ResponseEntity<?> transitionShipmentStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params) {
		try {
			ShipmentStatus status = ShipmentStatus.valueOf(newStatus.toUpperCase());
			return ResponseEntity.ok(shipmentService.transitionShipmentStatus(id, status, params));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + newStatus);
		}
	}

	// Stops management
	@PostMapping("/{shipmentId}/stops")
	@LogActivity("Add Stop to Shipment")
	public ResponseEntity<?> addStop(@PathVariable Long shipmentId, @Valid @RequestBody ShipmentStopDto stopDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.addStopToShipment(shipmentId, stopDto));
	}

	@GetMapping("/{shipmentId}/stops")
	@LogActivity("Get Shipment Stops")
	public ResponseEntity<?> getStops(@PathVariable Long shipmentId) {
		return ResponseEntity.ok(shipmentService.getStopsByShipment(shipmentId));
	}

	@PutMapping("/stops/{stopId}")
	@LogActivity("Update Shipment Stop")
	public ResponseEntity<?> updateStop(@PathVariable Long stopId, @RequestBody ShipmentStopDto stopDto) {
		return ResponseEntity.ok(shipmentService.updateStop(stopId, stopDto));
	}

	@DeleteMapping("/stops/{stopId}")
	@LogActivity("Remove Shipment Stop")
	public ResponseEntity<?> removeStop(@PathVariable Long stopId) {
		shipmentService.removeStop(stopId);
		return ResponseEntity.noContent().build();
	}

	// Consolidated Stop Status Transition
	@PutMapping("/stops/{stopId}/status")
	@LogActivity("Transition Shipment Stop Status")
	public ResponseEntity<?> transitionStopStatus(@PathVariable Long stopId,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params) {
		try {
			StopStatus status = StopStatus.valueOf(newStatus.toUpperCase());
			return ResponseEntity.ok(shipmentService.transitionStopStatus(stopId, status, params));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid stop status: " + newStatus);
		}
	}

	// Tracking events
	@PostMapping("/{shipmentId}/tracking")
	@LogActivity("Add Tracking Event")
	public ResponseEntity<?> addTrackingEvent(@PathVariable Long shipmentId,
			@Valid @RequestBody TrackingEventDto eventDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.addTrackingEvent(shipmentId, eventDto));
	}

	@GetMapping("/{shipmentId}/tracking")
	@LogActivity("Get Tracking Events")
	public ResponseEntity<?> getTrackingEvents(@PathVariable Long shipmentId) {
		return ResponseEntity.ok(shipmentService.getTrackingEventsByShipment(shipmentId));
	}

	@GetMapping("/{shipmentId}/tracking/latest")
	@LogActivity("Get Latest Tracking Event")
	public ResponseEntity<?> getLatestTrackingEvent(@PathVariable Long shipmentId) {
		TrackingEventDto latest = shipmentService.getLatestTrackingEvent(shipmentId);
		if (latest != null) {
			return ResponseEntity.ok(latest);
		}
		return ResponseEntity.notFound().build();
	}

	// Documents
	@PostMapping("/{shipmentId}/documents")
	@LogActivity("Add Shipment Document")
	public ResponseEntity<?> addDocument(@PathVariable Long shipmentId,
			@Valid @RequestBody ShipmentDocumentDto documentDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.addDocument(shipmentId, documentDto));
	}

	@GetMapping("/{shipmentId}/documents")
	@LogActivity("Get Shipment Documents")
	public ResponseEntity<?> getDocuments(@PathVariable Long shipmentId,
			@RequestParam(required = false) String documentType) {
		if (documentType != null) {
			try {
				com.nexus.core.entities.ShipmentDocumentType type = com.nexus.core.entities.ShipmentDocumentType
						.valueOf(documentType.toUpperCase());
				return ResponseEntity.ok(shipmentService.getDocumentsByShipmentAndType(shipmentId, type));
			} catch (IllegalArgumentException e) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid document type: " + documentType);
			}
		}
		return ResponseEntity.ok(shipmentService.getDocumentsByShipment(shipmentId));
	}

	@DeleteMapping("/documents/{documentId}")
	@LogActivity("Delete Shipment Document")
	public ResponseEntity<?> deleteDocument(@PathVariable Long documentId) {
		shipmentService.deleteDocument(documentId);
		return ResponseEntity.noContent().build();
	}

	// Freight cost management (Consolidated)
	@PutMapping("/{id}/freight-cost")
	@LogActivity("Update Freight Cost")
	public ResponseEntity<?> updateFreightCost(@PathVariable Long id,
			@RequestParam(required = false) Double estimatedCost,
			@RequestParam(required = false) Double actualCost,
			@RequestParam(required = false) String currency) {
		java.math.BigDecimal estCost = estimatedCost != null ? java.math.BigDecimal.valueOf(estimatedCost) : null;
		java.math.BigDecimal actCost = actualCost != null ? java.math.BigDecimal.valueOf(actualCost) : null;
		java.util.Currency curr = currency != null ? java.util.Currency.getInstance(currency) : null;
		return ResponseEntity.ok(shipmentService.updateFreightCost(id, estCost, actCost, curr));
	}

	// Search and filtering
	@GetMapping("/search")
	@LogActivity("Search Shipments")
	public ResponseEntity<?> searchShipments(@RequestParam String q, @PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(shipmentService.searchShipments(q, pageable));
	}

	@GetMapping("/attention")
	@LogActivity("Get Shipments Requiring Attention")
	public ResponseEntity<?> getShipmentsRequiringAttention() {
		return ResponseEntity.ok(shipmentService.getShipmentsRequiringAttention());
	}

	@GetMapping("/overdue")
	@LogActivity("Get Overdue Shipments")
	public ResponseEntity<?> getOverdueShipments() {
		return ResponseEntity.ok(shipmentService.getOverdueShipments());
	}

	@GetMapping("/date-range")
	@LogActivity("Get Shipments by Date Range")
	public ResponseEntity<?> getShipmentsByDateRange(
			@RequestParam String startDate,
			@RequestParam String endDate,
			@PageableDefault(size = 20) Pageable pageable) {
		java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
		java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
		return ResponseEntity.ok(shipmentService.getShipmentsByDateRange(start, end, pageable));
	}
}