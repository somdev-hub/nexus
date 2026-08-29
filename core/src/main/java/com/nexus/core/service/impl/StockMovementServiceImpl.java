package com.nexus.core.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.entities.Material;
import com.nexus.core.entities.Stock;
import com.nexus.core.entities.StockMovement;
import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.StockMovementDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.StockMovementRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.StockMovementService;

import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StockMovementService.
 * Provides audit trail for inventory movements.
 */
@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

	private final StockMovementRepo stockMovementRepo;
	private final StockRepo stockRepo;
	private final MaterialRepo materialRepo;
	private final WarehouseRepo warehouseRepo;
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public ResponseEntity<?> addStockMovement(StockMovementDto movementDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(movementDto.getStockId())
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", movementDto.getStockId()));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		StockMovement movement = modelMapper.map(movementDto, StockMovement.class);
		movement.setStock(stock);

		StockMovement savedMovement = stockMovementRepo.save(movement);
		return new ResponseEntity<>(mapToMovementDto(savedMovement), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getStockMovementById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		StockMovement movement = stockMovementRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("StockMovement", "movementId", id));

		// Verify movement belongs to the organization
		if (!movement.getStock().getMaterial().getOrg().equals(orgId)
				|| !movement.getStock().getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Movement does not belong to this organization");
		}

		return new ResponseEntity<>(mapToMovementDto(movement), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getMovementsByOrg(Long stockId, Long warehouseId, String type, String referenceType,
			Long referenceId, String batchNumber, Timestamp beforeDate, Timestamp start, Timestamp end, Long materialId,
			Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		// If stockId is provided, validate it belongs to the organization
		if (stockId != null) {
			Stock stock = stockRepo.findById(stockId)
					.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));
			if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
			}
			Page<StockMovement> movements = stockMovementRepo.findByStockOrderByCreatedAtDesc(stock, pageable);
			Page<StockMovementDto> movementDtos = movements.map(this::mapToMovementDto);
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// If warehouseId is provided, validate it belongs to the organization
		if (warehouseId != null) {
			Warehouse warehouse = warehouseRepo.findById(warehouseId)
					.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", warehouseId));
			if (!warehouse.getOrg().equals(orgId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body("Warehouse does not belong to this organization");
			}
			List<StockMovement> movements = stockMovementRepo.findByWarehouseOrderByCreatedAtDesc(warehouse);
			List<StockMovementDto> movementDtos = movements.stream()
					.map(this::mapToMovementDto)
					.collect(Collectors.toList());
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// If type is provided, filter by movement type
		if (type != null && !type.isBlank()) {
			try {
				StockMovement.MovementType movementType = StockMovement.MovementType.valueOf(type.toUpperCase());
				List<StockMovement> movements = stockMovementRepo.findByMovementTypeAndOrg(movementType, orgId);
				List<StockMovementDto> movementDtos = movements.stream()
						.map(this::mapToMovementDto)
						.collect(Collectors.toList());
				return new ResponseEntity<>(movementDtos, HttpStatus.OK);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body("Invalid movement type: " + type);
			}
		}

		// If referenceType and referenceId are provided, filter by reference
		if (referenceType != null && !referenceType.isBlank() && referenceId != null) {
			List<StockMovement> movements = stockMovementRepo.findByReference(referenceType, referenceId);
			List<StockMovement> filteredMovements = movements.stream()
					.filter(m -> m.getStock().getMaterial().getOrg().equals(orgId)
							&& m.getStock().getWarehouse().getOrg().equals(orgId))
					.collect(Collectors.toList());
			List<StockMovementDto> movementDtos = filteredMovements.stream()
					.map(this::mapToMovementDto)
					.collect(Collectors.toList());
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// If batchNumber is provided, filter by batch
		if (batchNumber != null && !batchNumber.isBlank()) {
			List<StockMovement> movements = stockMovementRepo.findByBatchNumberAndOrg(batchNumber, orgId);
			List<StockMovementDto> movementDtos = movements.stream()
					.map(this::mapToMovementDto)
					.collect(Collectors.toList());
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// If beforeDate is provided, get expiring stock
		if (beforeDate != null) {
			List<StockMovement> movements = stockMovementRepo.findExpiringBeforeDate(beforeDate, orgId);
			List<StockMovementDto> movementDtos = movements.stream()
					.map(this::mapToMovementDto)
					.collect(Collectors.toList());
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// If start and end are provided, filter by date range
		if (start != null && end != null) {
			List<StockMovement> movements = stockMovementRepo.findByDateRangeAndOrg(start, end, orgId);
			List<StockMovementDto> movementDtos = movements.stream()
					.map(this::mapToMovementDto)
					.collect(Collectors.toList());
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// If materialId is provided, validate it belongs to the organization
		if (materialId != null) {
			Material material = materialRepo.findById(materialId)
					.orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", materialId));
			if (!material.getOrg().equals(orgId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body("Material does not belong to this organization");
			}
			List<StockMovement> movements = stockMovementRepo.findByMaterialAndOrg(material, orgId);
			List<StockMovementDto> movementDtos = movements.stream()
					.map(this::mapToMovementDto)
					.collect(Collectors.toList());
			return new ResponseEntity<>(movementDtos, HttpStatus.OK);
		}

		// No filters - return all movements for the organization with pagination
		Page<StockMovement> movements = stockMovementRepo.findByOrgIdOrderByCreatedAtDesc(orgId, pageable);
		Page<StockMovementDto> movementDtos = movements.map(this::mapToMovementDto);
		return new ResponseEntity<>(movementDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getMovementSummary(Timestamp start, Timestamp end) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<StockMovement> movements = stockMovementRepo.findByDateRangeAndOrg(start, end, orgId);

		// Group by movement type
		java.util.Map<StockMovement.MovementType, Long> typeCounts = movements.stream()
				.collect(Collectors.groupingBy(StockMovement::getMovementType, Collectors.counting()));

		// Group by reference type
		java.util.Map<String, Long> referenceTypeCounts = movements.stream()
				.collect(Collectors.groupingBy(StockMovement::getReferenceType, Collectors.counting()));

		// Total quantities
		Double totalInbound = movements.stream()
				.filter(m -> isInboundType(m.getMovementType()))
				.mapToDouble(StockMovement::getQuantity)
				.sum();

		Double totalOutbound = movements.stream()
				.filter(m -> isOutboundType(m.getMovementType()))
				.mapToDouble(StockMovement::getQuantity)
				.sum();

		return new ResponseEntity<>(new MovementSummaryResponse(
				(long) movements.size(),
				typeCounts,
				referenceTypeCounts,
				totalInbound,
				totalOutbound), HttpStatus.OK);
	}

	private boolean isInboundType(StockMovement.MovementType type) {
		return type == StockMovement.MovementType.RECEIPT ||
				type == StockMovement.MovementType.RETURN_FROM_CUSTOMER ||
				type == StockMovement.MovementType.TRANSFER_IN ||
				type == StockMovement.MovementType.ADJUSTMENT_IN ||
				type == StockMovement.MovementType.PRODUCTION_IN;
	}

	private boolean isOutboundType(StockMovement.MovementType type) {
		return type == StockMovement.MovementType.ISSUE ||
				type == StockMovement.MovementType.SHIPMENT ||
				type == StockMovement.MovementType.TRANSFER_OUT ||
				type == StockMovement.MovementType.ADJUSTMENT_OUT ||
				type == StockMovement.MovementType.RETURN_TO_SUPPLIER ||
				type == StockMovement.MovementType.SCRAP;
	}

	private StockMovementDto mapToMovementDto(StockMovement movement) {
		StockMovementDto dto = modelMapper.map(movement, StockMovementDto.class);
		dto.setMaterialCode(movement.getStock().getMaterial().getCode());
		dto.setMaterialName(movement.getStock().getMaterial().getName());
		dto.setWarehouseCode(movement.getStock().getWarehouse().getCode());
		if (movement.getFromWarehouseId() != null) {
			warehouseRepo.findById(movement.getFromWarehouseId()).ifPresent(w -> dto.setFromWarehouseCode(w.getCode()));
		}
		if (movement.getToWarehouseId() != null) {
			warehouseRepo.findById(movement.getToWarehouseId()).ifPresent(w -> dto.setToWarehouseCode(w.getCode()));
		}
		dto.setCreatedAt(movement.getCreatedAt());
		return dto;
	}

	// Using record for cleaner code
	public record MovementSummaryResponse(
			Long totalMovements,
			java.util.Map<StockMovement.MovementType, Long> movementsByType,
			java.util.Map<String, Long> movementsByReferenceType,
			Double totalInboundQuantity,
			Double totalOutboundQuantity) {
	}
}