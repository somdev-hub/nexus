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
import com.nexus.core.entities.Stock.ValuationMethod;
import com.nexus.core.entities.StockMovement;
import com.nexus.core.entities.StockMovement.MovementType;
import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.StockDto;
import com.nexus.core.payload.StockMovementDto;
import com.nexus.core.repository.MaterialRepo;
import com.nexus.core.repository.StockMovementRepo;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.service.StockMovementService;
import com.nexus.core.service.StockService;

import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StockService.
 * Supports FR-RET-010: Multi-Warehouse Inventory
 * Supports FR-RET-011: Reorder Point Automation
 * Supports FR-RET-014: Inventory Valuation
 */
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

	private final StockRepo stockRepo;
	private final StockMovementRepo stockMovementRepo;
	private final MaterialRepo materialRepo;
	private final WarehouseRepo warehouseRepo;
	private final ModelMapper modelMapper;
	private final StockMovementService stockMovementService;

	@Override
	@Transactional
	public ResponseEntity<?> addStock(StockDto stockDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Material material = materialRepo.findById(stockDto.getMaterialId())
				.orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", stockDto.getMaterialId()));

		Warehouse warehouse = warehouseRepo.findById(stockDto.getWarehouseId())
				.orElseThrow(
						() -> new ResourceNotFoundException("Warehouse", "warehouseId", stockDto.getWarehouseId()));

		// Verify material and warehouse belong to the organization
		if (!material.getOrg().equals(orgId) || !warehouse.getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Material or Warehouse does not belong to this organization");
		}

		// Check if stock already exists for this material/warehouse combination
		if (stockRepo.findByMaterialAndWarehouse(material, warehouse).isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("Stock already exists for this material and warehouse combination");
		}

		Stock stock = modelMapper.map(stockDto, Stock.class);
		stock.setMaterial(material);
		stock.setWarehouse(warehouse);
		stock.recalculateAvailable();

		Stock savedStock = stockRepo.save(stock);
		return new ResponseEntity<>(mapToStockDto(savedStock), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getStockById(Long id) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", id));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		return new ResponseEntity<>(mapToStockDto(stock), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getStockByMaterialAndWarehouse(Long materialId, Long warehouseId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Material material = materialRepo.findById(materialId)
				.orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", materialId));

		Warehouse warehouse = warehouseRepo.findById(warehouseId)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", warehouseId));

		// Verify material and warehouse belong to the organization
		if (!material.getOrg().equals(orgId) || !warehouse.getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Material or Warehouse does not belong to this organization");
		}

		Stock stock = stockRepo.findByMaterialAndWarehouse(material, warehouse)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "materialId/warehouseId",
						materialId + "/" + warehouseId));

		return new ResponseEntity<>(mapToStockDto(stock), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllStockByOrgId(Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Page<Stock> stocks = stockRepo.findActiveByOrgId(orgId, pageable);
		Page<StockDto> stockDtos = stocks.map(this::mapToStockDto);
		return new ResponseEntity<>(stockDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllStockByWarehouse(Long warehouseId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Warehouse warehouse = warehouseRepo.findById(warehouseId)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", warehouseId));

		// Verify warehouse belongs to the organization
		if (!warehouse.getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Warehouse does not belong to this organization");
		}

		List<Stock> stocks = stockRepo.findActiveByWarehouse(warehouse);
		List<StockDto> stockDtos = stocks.stream()
				.map(this::mapToStockDto)
				.collect(Collectors.toList());
		return new ResponseEntity<>(stockDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllStockByMaterial(Long materialId, Pageable pageable) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Material material = materialRepo.findById(materialId)
				.orElseThrow(() -> new ResourceNotFoundException("Material", "materialId", materialId));

		// Verify material belongs to the organization
		if (!material.getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Material does not belong to this organization");
		}

		List<Stock> stocks = stockRepo.findActiveByMaterial(material);
		List<StockDto> stockDtos = stocks.stream()
				.map(this::mapToStockDto)
				.collect(Collectors.toList());
		return new ResponseEntity<>(stockDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getStockBelowReorderPoint() {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<Stock> stocks = stockRepo.findBelowReorderPoint(orgId);
		List<StockDto> stockDtos = stocks.stream()
				.map(this::mapToStockDto)
				.collect(Collectors.toList());
		return new ResponseEntity<>(stockDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getStockAtOrBelowMinLevel() {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<Stock> stocks = stockRepo.findAtOrBelowMinLevel(orgId);
		List<StockDto> stockDtos = stocks.stream()
				.map(this::mapToStockDto)
				.collect(Collectors.toList());
		return new ResponseEntity<>(stockDtos, HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getInventoryValuation() {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Double totalValue = stockRepo.getTotalInventoryValue(orgId);
		List<Stock> stocks = stockRepo.findActiveByOrgId(orgId);

		List<StockDto> stockDtos = stocks.stream()
				.map(this::mapToStockDto)
				.collect(Collectors.toList());

		return new ResponseEntity<>(new InventoryValuationResponse(totalValue, stockDtos), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getWarehouseInventoryValuation(Long warehouseId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Warehouse warehouse = warehouseRepo.findById(warehouseId)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", warehouseId));

		// Verify warehouse belongs to the organization
		if (!warehouse.getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Warehouse does not belong to this organization");
		}

		Double totalValue = stockRepo.getWarehouseInventoryValue(warehouse);
		List<Stock> stocks = stockRepo.findActiveByWarehouse(warehouse);

		List<StockDto> stockDtos = stocks.stream()
				.map(this::mapToStockDto)
				.collect(Collectors.toList());

		return new ResponseEntity<>(new InventoryValuationResponse(totalValue, stockDtos), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> adjustStock(Long stockId, Double quantity, String reason, String referenceType,
			Long referenceId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		Double quantityBefore = stock.getQuantityOnHand();
		stock.setQuantityOnHand(stock.getQuantityOnHand() + quantity);
		stock.recalculateAvailable();

		Stock savedStock = stockRepo.save(stock);

		// Create stock movement record
		StockMovement movement = new StockMovement();
		movement.setStock(stock);
		movement.setMovementType(quantity > 0 ? MovementType.ADJUSTMENT_IN : MovementType.ADJUSTMENT_OUT);
		movement.setQuantity(Math.abs(quantity));
		movement.setQuantityBefore(quantityBefore);
		movement.setQuantityAfter(savedStock.getQuantityOnHand());
		movement.setReferenceType(referenceType);
		movement.setReferenceId(referenceId);
		movement.setReason(reason);
		movement.setCreatedBy("SYSTEM"); // TODO: Get from security context
		stockMovementRepo.save(movement);

		return new ResponseEntity<>(mapToStockDto(savedStock), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> reserveStock(Long stockId, Double quantity, String referenceType, Long referenceId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		if (stock.getQuantityAvailable() < quantity) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Insufficient available stock. Available: " + stock.getQuantityAvailable() + ", Requested: "
							+ quantity);
		}

		Double quantityBefore = stock.getQuantityReserved();
		stock.setQuantityReserved(stock.getQuantityReserved() + quantity);
		stock.recalculateAvailable();

		Stock savedStock = stockRepo.save(stock);

		// Create stock movement record
		StockMovement movement = new StockMovement();
		movement.setStock(stock);
		movement.setMovementType(MovementType.RESERVATION);
		movement.setQuantity(quantity);
		movement.setQuantityBefore(quantityBefore);
		movement.setQuantityAfter(savedStock.getQuantityReserved());
		movement.setReferenceType(referenceType);
		movement.setReferenceId(referenceId);
		movement.setReason("Stock reservation for " + referenceType);
		movement.setCreatedBy("SYSTEM");
		stockMovementRepo.save(movement);

		return new ResponseEntity<>(mapToStockDto(savedStock), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> releaseReservation(Long stockId, Double quantity, String referenceType, Long referenceId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		if (stock.getQuantityReserved() < quantity) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Insufficient reserved stock. Reserved: " + stock.getQuantityReserved()
							+ ", Requested to release: " + quantity);
		}

		Double quantityBefore = stock.getQuantityReserved();
		stock.setQuantityReserved(stock.getQuantityReserved() - quantity);
		stock.recalculateAvailable();

		Stock savedStock = stockRepo.save(stock);

		// Create stock movement record
		StockMovement movement = new StockMovement();
		movement.setStock(stock);
		movement.setMovementType(MovementType.RELEASE_RESERVATION);
		movement.setQuantity(quantity);
		movement.setQuantityBefore(quantityBefore);
		movement.setQuantityAfter(savedStock.getQuantityReserved());
		movement.setReferenceType(referenceType);
		movement.setReferenceId(referenceId);
		movement.setReason("Release reservation for " + referenceType);
		movement.setCreatedBy("SYSTEM");
		stockMovementRepo.save(movement);

		return new ResponseEntity<>(mapToStockDto(savedStock), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> transferStock(Long fromStockId, Long toWarehouseId, Double quantity, String reason) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock fromStock = stockRepo.findById(fromStockId)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", fromStockId));

		Warehouse toWarehouse = warehouseRepo.findById(toWarehouseId)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", toWarehouseId));

		// Verify fromStock belongs to the organization
		if (!fromStock.getMaterial().getOrg().equals(orgId) || !fromStock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Source stock does not belong to this organization");
		}

		// Verify toWarehouse belongs to the organization
		if (!toWarehouse.getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Destination warehouse does not belong to this organization");
		}

		if (fromStock.getQuantityAvailable() < quantity) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Insufficient available stock for transfer. Available: " + fromStock.getQuantityAvailable());
		}

		// Reduce from source stock
		Double fromQuantityBefore = fromStock.getQuantityOnHand();
		fromStock.setQuantityOnHand(fromStock.getQuantityOnHand() - quantity);
		fromStock.recalculateAvailable();
		Stock savedFromStock = stockRepo.save(fromStock);

		// Create or get destination stock
		Stock toStock = stockRepo.findByMaterialAndWarehouse(fromStock.getMaterial(), toWarehouse)
				.orElseGet(() -> {
					Stock newStock = new Stock();
					newStock.setMaterial(fromStock.getMaterial());
					newStock.setWarehouse(toWarehouse);
					newStock.setQuantityOnHand(0.0);
					newStock.setQuantityReserved(0.0);
					newStock.setValuationMethod(fromStock.getValuationMethod());
					newStock.setAverageCost(fromStock.getAverageCost());
					return stockRepo.save(newStock);
				});

		Double toQuantityBefore = toStock.getQuantityOnHand();
		toStock.setQuantityOnHand(toStock.getQuantityOnHand() + quantity);
		toStock.recalculateAvailable();
		Stock savedToStock = stockRepo.save(toStock);

		// Create outbound movement
		StockMovement outMovement = new StockMovement();
		outMovement.setStock(fromStock);
		outMovement.setMovementType(MovementType.TRANSFER_OUT);
		outMovement.setQuantity(quantity);
		outMovement.setQuantityBefore(fromQuantityBefore);
		outMovement.setQuantityAfter(savedFromStock.getQuantityOnHand());
		outMovement.setReferenceType("TRANSFER");
		outMovement.setFromWarehouseId(fromStock.getWarehouse().getWarehouseId());
		outMovement.setToWarehouseId(toWarehouseId);
		outMovement.setReason(reason);
		outMovement.setCreatedBy("SYSTEM");
		stockMovementRepo.save(outMovement);

		// Create inbound movement
		StockMovement inMovement = new StockMovement();
		inMovement.setStock(toStock);
		inMovement.setMovementType(MovementType.TRANSFER_IN);
		inMovement.setQuantity(quantity);
		inMovement.setQuantityBefore(toQuantityBefore);
		inMovement.setQuantityAfter(savedToStock.getQuantityOnHand());
		inMovement.setReferenceType("TRANSFER");
		inMovement.setFromWarehouseId(fromStock.getWarehouse().getWarehouseId());
		inMovement.setToWarehouseId(toWarehouseId);
		inMovement.setReason(reason);
		inMovement.setCreatedBy("SYSTEM");
		stockMovementRepo.save(inMovement);

		return new ResponseEntity<>(new TransferResponse(
				mapToStockDto(savedFromStock),
				mapToStockDto(savedToStock)), HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> recordCycleCount(Long stockId, Double countedQuantity, String countedBy) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		Double quantityBefore = stock.getQuantityOnHand();
		Double adjustment = countedQuantity - quantityBefore;

		stock.setQuantityOnHand(countedQuantity);
		stock.setLastCountedAt(Timestamp.valueOf(LocalDateTime.now()));
		stock.setLastCountedBy(countedBy);
		stock.recalculateAvailable();

		Stock savedStock = stockRepo.save(stock);

		// Create stock movement record
		StockMovement movement = new StockMovement();
		movement.setStock(stock);
		movement.setMovementType(adjustment > 0 ? MovementType.ADJUSTMENT_IN : MovementType.ADJUSTMENT_OUT);
		movement.setQuantity(Math.abs(adjustment));
		movement.setQuantityBefore(quantityBefore);
		movement.setQuantityAfter(savedStock.getQuantityOnHand());
		movement.setReferenceType("CYCLE_COUNT");
		movement.setReason("Cycle count adjustment");
		movement.setCreatedBy(countedBy);
		stockMovementRepo.save(movement);

		return new ResponseEntity<>(mapToStockDto(savedStock), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getReorderSuggestions() {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		List<Stock> stocks = stockRepo.findBelowReorderPoint(orgId);

		List<ReorderSuggestionDto> suggestions = stocks.stream()
				.filter(s -> s.getReorderQuantity() != null && s.getReorderQuantity() > 0)
				.map(s -> new ReorderSuggestionDto(
						s.getStockId(),
						s.getMaterial().getMaterialId(),
						s.getMaterial().getCode(),
						s.getMaterial().getName(),
						s.getWarehouse().getWarehouseId(),
						s.getWarehouse().getCode(),
						s.getQuantityAvailable(),
						s.getReorderPoint(),
						s.getReorderQuantity(),
						s.getReorderQuantity() - s.getQuantityAvailable()))
				.collect(Collectors.toList());

		return new ResponseEntity<>(suggestions, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> updateStockSettings(Long stockId, Double reorderPoint, Double reorderQuantity,
			Double minStockLevel, Double maxStockLevel) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();

		Stock stock = stockRepo.findById(stockId)
				.orElseThrow(() -> new ResourceNotFoundException("Stock", "stockId", stockId));

		// Verify stock belongs to the organization
		if (!stock.getMaterial().getOrg().equals(orgId) || !stock.getWarehouse().getOrg().equals(orgId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Stock does not belong to this organization");
		}

		if (reorderPoint != null)
			stock.setReorderPoint(reorderPoint);
		if (reorderQuantity != null)
			stock.setReorderQuantity(reorderQuantity);
		if (minStockLevel != null)
			stock.setMinStockLevel(minStockLevel);
		if (maxStockLevel != null)
			stock.setMaxStockLevel(maxStockLevel);

		Stock savedStock = stockRepo.save(stock);
		return new ResponseEntity<>(mapToStockDto(savedStock), HttpStatus.OK);
	}

	/**
	 * Map Stock entity to StockDto with enrichment
	 */
	private StockDto mapToStockDto(Stock stock) {
		StockDto dto = modelMapper.map(stock, StockDto.class);
		dto.setMaterialCode(stock.getMaterial().getCode());
		dto.setMaterialName(stock.getMaterial().getName());
		dto.setWarehouseCode(stock.getWarehouse().getCode());
		dto.setWarehouseLocation(stock.getWarehouse().getLocation());
		dto.setBelowReorderPoint(stock.isBelowReorderPoint());
		dto.setAtOrBelowMinLevel(stock.isAtOrBelowMinLevel());
		dto.setAtOrAboveMaxLevel(stock.isAtOrAboveMaxLevel());
		dto.setTotalValue(stock.getQuantityOnHand() * (stock.getAverageCost() != null ? stock.getAverageCost() : 0.0));
		return dto;
	}

	// Inner classes for responses - using records for cleaner code
	public record InventoryValuationResponse(Double totalValue, List<StockDto> stocks) {
	}

	public record TransferResponse(StockDto fromStock, StockDto toStock) {
	}

	public record ReorderSuggestionDto(
			Long stockId,
			Long materialId,
			String materialCode,
			String materialName,
			Long warehouseId,
			String warehouseCode,
			Double quantityAvailable,
			Double reorderPoint,
			Double reorderQuantity,
			Double suggestedOrderQuantity) {
	}
}