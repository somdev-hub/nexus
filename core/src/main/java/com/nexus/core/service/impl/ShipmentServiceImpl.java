package com.nexus.core.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.dto.ShipmentDto;
import com.nexus.core.dto.ShipmentStopDto;
import com.nexus.core.dto.TrackingEventDto;
import com.nexus.core.dto.ShipmentDocumentDto;
import com.nexus.core.entities.Shipment;
import com.nexus.core.entities.ShipmentDocument;
import com.nexus.core.entities.ShipmentDocumentType;
import com.nexus.core.entities.ShipmentMode;
import com.nexus.core.entities.ShipmentStatus;
import com.nexus.core.entities.ShipmentStop;
import com.nexus.core.entities.StopStatus;
import com.nexus.core.entities.StopType;
import com.nexus.core.entities.TrackingEvent;
import com.nexus.core.entities.TrackingEventType;
import com.nexus.core.repository.ShipmentDocumentRepo;
import com.nexus.core.repository.ShipmentRepo;
import com.nexus.core.repository.ShipmentStopRepo;
import com.nexus.core.repository.TrackingEventRepo;
import com.nexus.core.service.ShipmentService;

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
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public ShipmentDto createShipment(ShipmentDto shipmentDto) {
		log.info("Creating new shipment: {}", shipmentDto.getShipmentNumber());

		Shipment shipment = modelMapper.map(shipmentDto, Shipment.class);
		shipment.setStatus(ShipmentStatus.DRAFT);
		shipment.setCreatedAt(LocalDateTime.now());
		shipment.setUpdatedAt(LocalDateTime.now());

		// Generate shipment number if not provided
		if (shipment.getShipmentNumber() == null || shipment.getShipmentNumber().isBlank()) {
			shipment.setShipmentNumber(generateShipmentNumber());
		}

		Shipment saved = shipmentRepo.save(shipment);
		return modelMapper.map(saved, ShipmentDto.class);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ShipmentDto> getShipmentById(Long shipmentId) {
		return shipmentRepo.findById(shipmentId)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> getAllShipments(Pageable pageable) {
		return shipmentRepo.findAll(pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> getShipmentsByStatus(ShipmentStatus status, Pageable pageable) {
		return shipmentRepo.findByStatus(status, pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> getShipmentsByMode(ShipmentMode mode, Pageable pageable) {
		return shipmentRepo.findByMode(mode, pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> getShipmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
			Pageable pageable) {
		return shipmentRepo.findByScheduledPickupDateBetween(startDate, endDate, pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> getShipmentsBySupplier(Long supplierId, Pageable pageable) {
		return shipmentRepo.findBySupplierId(supplierId, pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> getShipmentsByWarehouse(Long warehouseId, Pageable pageable) {
		return shipmentRepo.findByWarehouseId(warehouseId, pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional
	public ShipmentDto updateShipment(Long shipmentId, ShipmentDto shipmentDto) {
		log.info("Updating shipment: {}", shipmentId);

		Shipment existing = shipmentRepo.findById(shipmentId)
				.orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

		// Update fields
		modelMapper.map(shipmentDto, existing);
		existing.setShipmentId(shipmentId);
		existing.setUpdatedAt(LocalDateTime.now());

		Shipment saved = shipmentRepo.save(existing);
		return modelMapper.map(saved, ShipmentDto.class);
	}

	@Override
	@Transactional
	public void deleteShipment(Long shipmentId) {
		log.info("Deleting shipment: {}", shipmentId);
		shipmentRepo.deleteById(shipmentId);
	}

	// Consolidated Status Transition
	@Override
	@Transactional
	public ShipmentDto transitionShipmentStatus(Long shipmentId, ShipmentStatus newStatus, Map<String, Object> params) {
		String reason = params != null && params.get("reason") != null ? params.get("reason").toString()
				: "Status transition";
		return transitionStatus(shipmentId, newStatus, reason);
	}

	// Stops management
	@Override
	@Transactional
	public ShipmentStopDto addStopToShipment(Long shipmentId, ShipmentStopDto stopDto) {
		log.info("Adding stop to shipment: {}", shipmentId);

		Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();

		ShipmentStop stop = modelMapper.map(stopDto, ShipmentStop.class);
		stop.setShipment(shipment);
		stop.setStatus(StopStatus.PENDING);
		stop.setCreatedAt(LocalDateTime.now());
		stop.setUpdatedAt(LocalDateTime.now());

		ShipmentStop saved = shipmentStopRepo.save(stop);
		return modelMapper.map(saved, ShipmentStopDto.class);
	}

	@Override
	@Transactional
	public ShipmentStopDto updateStop(Long stopId, ShipmentStopDto stopDto) {
		ShipmentStop existing = shipmentStopRepo.findById(stopId).orElseThrow();
		modelMapper.map(stopDto, existing);
		existing.setStopId(stopId);
		existing.setUpdatedAt(LocalDateTime.now());
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
		return shipmentStopRepo.findByShipmentShipmentIdOrderBySequenceNumber(shipmentId)
				.stream()
				.map(stop -> modelMapper.map(stop, ShipmentStopDto.class))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ShipmentStopDto transitionStopStatus(Long stopId, StopStatus newStatus, Map<String, Object> params) {
		ShipmentStop stop = shipmentStopRepo.findById(stopId).orElseThrow();
		StopStatus oldStatus = stop.getStatus();
		stop.setStatus(newStatus);
		String reason = params != null && params.get("reason") != null ? params.get("reason").toString()
				: "Stop status transition";
		stop.setSpecialInstructions((stop.getSpecialInstructions() != null ? stop.getSpecialInstructions() + "\n" : "")
				+ "Status changed: " + oldStatus + " -> " + newStatus + " (" + reason + ")");
		stop.setUpdatedAt(LocalDateTime.now());
		return modelMapper.map(shipmentStopRepo.save(stop), ShipmentStopDto.class);
	}

	// Tracking events
	@Override
	@Transactional
	public TrackingEventDto addTrackingEvent(Long shipmentId, TrackingEventDto eventDto) {
		log.info("Adding tracking event to shipment: {}", shipmentId);

		Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();

		TrackingEvent event = modelMapper.map(eventDto, TrackingEvent.class);
		event.setShipment(shipment);
		event.setCreatedAt(LocalDateTime.now());

		TrackingEvent saved = trackingEventRepo.save(event);
		return modelMapper.map(saved, TrackingEventDto.class);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TrackingEventDto> getTrackingEventsByShipment(Long shipmentId) {
		return trackingEventRepo.findByShipmentShipmentIdOrderByEventTimestampDesc(shipmentId)
				.stream()
				.map(event -> modelMapper.map(event, TrackingEventDto.class))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public TrackingEventDto getLatestTrackingEvent(Long shipmentId) {
		return trackingEventRepo.findFirstByShipmentShipmentIdOrderByEventTimestampDesc(shipmentId)
				.map(event -> modelMapper.map(event, TrackingEventDto.class))
				.orElse(null);
	}

	// Documents
	@Override
	@Transactional
	public ShipmentDocumentDto addDocument(Long shipmentId, ShipmentDocumentDto documentDto) {
		log.info("Adding document to shipment: {}", shipmentId);

		Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();

		ShipmentDocument document = modelMapper.map(documentDto, ShipmentDocument.class);
		document.setShipment(shipment);
		document.setCreatedAt(LocalDateTime.now());
		document.setUpdatedAt(LocalDateTime.now());

		ShipmentDocument saved = shipmentDocumentRepo.save(document);
		return modelMapper.map(saved, ShipmentDocumentDto.class);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentDocumentDto> getDocumentsByShipment(Long shipmentId) {
		return shipmentDocumentRepo.findByShipmentShipmentId(shipmentId)
				.stream()
				.map(doc -> modelMapper.map(doc, ShipmentDocumentDto.class))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentDocumentDto> getDocumentsByShipmentAndType(Long shipmentId, ShipmentDocumentType documentType) {
		return shipmentDocumentRepo.findByShipmentShipmentIdAndDocumentType(shipmentId, documentType)
				.stream()
				.map(doc -> modelMapper.map(doc, ShipmentDocumentDto.class))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void deleteDocument(Long documentId) {
		shipmentDocumentRepo.deleteById(documentId);
	}

	// Freight cost management (Consolidated)
	@Override
	@Transactional
	public ShipmentDto updateFreightCost(Long shipmentId, BigDecimal estimatedCost, BigDecimal actualCost,
			Currency currency) {
		Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
		if (estimatedCost != null) {
			shipment.setFreightCost(estimatedCost.doubleValue());
		}
		if (actualCost != null) {
			shipment.setActualFreightCost(actualCost.doubleValue());
		}
		if (currency != null) {
			shipment.setCurrency(currency.getCurrencyCode());
		}
		shipment.setUpdatedAt(LocalDateTime.now());
		return modelMapper.map(shipmentRepo.save(shipment), ShipmentDto.class);
	}

	// Search and filtering
	@Override
	@Transactional(readOnly = true)
	public Page<ShipmentDto> searchShipments(String query, Pageable pageable) {
		return shipmentRepo.searchShipments(query, pageable)
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentDto> getShipmentsRequiringAttention() {
		return shipmentRepo.findShipmentsRequiringAttention()
				.stream()
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentDto> getOverdueShipments() {
		return shipmentRepo.findOverdueShipments(LocalDateTime.now())
				.stream()
				.map(shipment -> modelMapper.map(shipment, ShipmentDto.class))
				.collect(Collectors.toList());
	}

	private ShipmentDto transitionStatus(Long shipmentId, ShipmentStatus newStatus, String reason) {
		Shipment shipment = shipmentRepo.findById(shipmentId).orElseThrow();
		ShipmentStatus oldStatus = shipment.getStatus();
		shipment.setStatus(newStatus);
		shipment.setSpecialInstructions(
				(shipment.getSpecialInstructions() != null ? shipment.getSpecialInstructions() + "\n" : "")
						+ "Status changed: " + oldStatus + " -> " + newStatus + " (" + reason + ")");
		shipment.setUpdatedAt(LocalDateTime.now());
		return modelMapper.map(shipmentRepo.save(shipment), ShipmentDto.class);
	}

	private String generateShipmentNumber() {
		return "SHP-" + System.currentTimeMillis();
	}
}