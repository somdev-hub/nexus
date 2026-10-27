package com.nexus.core.service.implementations;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.core.model.entities.Stock;
import com.nexus.core.model.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.WarehouseDto;
import com.nexus.core.repository.StockRepo;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.service.interfaces.WarehouseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

	private final WarehouseRepo warehouseRepo;
	private final StockRepo stockRepo;
	private final ModelMapper modelMapper;

	@Override
	public ResponseEntity<?> addWarehouse(WarehouseDto warehouseDto) {
		Warehouse warehouse = modelMapper.map(warehouseDto, Warehouse.class);
		Warehouse savedWarehouse = warehouseRepo.save(warehouse);
		return new ResponseEntity<>(toDto(savedWarehouse), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getWarehouseByIdAndOrg(Long id, Long orgId) {
		Warehouse warehouse = warehouseRepo.findByWarehouseIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", id));
		return new ResponseEntity<>(toDto(warehouse), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllWarehousesByOrgId(Long orgId, Pageable pageable) {
		ensureDefaultWarehouses(orgId);
		Page<Warehouse> warehouses = warehouseRepo.findByOrg(orgId, pageable);
		Page<WarehouseDto> warehouseDtos = warehouses.map(WarehouseServiceImpl::toDto);
		return new ResponseEntity<>(warehouseDtos, HttpStatus.OK);
	}

	@Override
	@Transactional
	public ResponseEntity<?> ensureDefaultWarehouses(Long orgId) {
		cleanupDuplicateProductionWarehouses(orgId);
		List<Warehouse> existing = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent();
		boolean hasProduction = existing.stream().anyMatch(w -> "PRODUCTION".equalsIgnoreCase(w.getWarehouseType()));
		if (hasProduction) return ResponseEntity.ok(existing);

		// Convert a legacy auto-created "MAIN" warehouse into the production warehouse
		Warehouse legacyMain = existing.stream().filter(w -> "MAIN".equalsIgnoreCase(w.getCode())).findFirst().orElse(null);
		if (legacyMain != null) {
			legacyMain.setCode("PROD");
			legacyMain.setLocation("Production");
			legacyMain.setWarehouseType("PRODUCTION");
			legacyMain.setCapacityMultiplier(1.0);
			if (legacyMain.getCapacity() == null) legacyMain.setCapacity(0.0);
			if (legacyMain.getStorageCapacity() == null) legacyMain.setStorageCapacity(0.0);
			legacyMain.setFillOrder(0);
			warehouseRepo.save(legacyMain);
		} else {
			Warehouse prod = new Warehouse();
			prod.setOrg(orgId);
			prod.setCode("PROD");
			prod.setLocation("Production");
			prod.setWarehouseType("PRODUCTION");
			prod.setCapacityMultiplier(1.0);
			prod.setCapacity(0.0);
			prod.setStorageCapacity(0.0);
			prod.setFillOrder(0);
			warehouseRepo.save(prod);
		}
		// create production + 3 reserves
		Warehouse prod = new Warehouse();
		prod.setOrg(orgId);
		prod.setCode("PROD");
		prod.setLocation("Production");
		prod.setWarehouseType("PRODUCTION");
		prod.setCapacityMultiplier(1.0);
		prod.setCapacity(0.0);
		prod.setStorageCapacity(0.0);
		prod.setFillOrder(0);
		warehouseRepo.save(prod);

		List<Warehouse> afterProd = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent();
		int[] mults = { 2, 3, 4 };
		for (int i = 0; i < mults.length; i++) {
			String code = "RESERVE-" + (i + 1);
			boolean exists = afterProd.stream().anyMatch(w -> code.equalsIgnoreCase(w.getCode()));
			if (exists) continue;
			Warehouse r = new Warehouse();
			r.setOrg(orgId);
			r.setCode(code);
			r.setLocation("Reserve " + (i + 1));
			r.setWarehouseType("RESERVE");
			r.setCapacityMultiplier((double) mults[i]);
			r.setCapacity(0.0);
			r.setStorageCapacity(0.0);
			r.setFillOrder(i + 1);
			warehouseRepo.save(r);
		}
		return ResponseEntity.ok(warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent());
	}

	@Override
	@Transactional
	public ResponseEntity<?> setProductionCapacity(Long orgId, Double capacity) {
		List<Warehouse> warehouses = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent();
		ensureDefaultWarehouses(orgId);
		warehouses = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent();
		Warehouse prod = warehouses.stream().filter(w -> "PRODUCTION".equalsIgnoreCase(w.getWarehouseType())).findFirst().orElse(null);
		if (prod == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Production warehouse not found");
		double cap = capacity != null ? capacity : 0.0;
		prod.setCapacity(cap);
		prod.setStorageCapacity(cap);
		warehouseRepo.save(prod);
		for (Warehouse w : warehouses) {
			if ("RESERVE".equalsIgnoreCase(w.getWarehouseType())) {
				double m = w.getCapacityMultiplier() != null ? w.getCapacityMultiplier() : 1.0;
				w.setCapacity(cap * m);
				w.setStorageCapacity(cap * m);
				warehouseRepo.save(w);
			}
		}
		return ResponseEntity.ok(warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent());
	}

	private void cleanupDuplicateProductionWarehouses(Long orgId) {
		List<Warehouse> prods = warehouseRepo.findByOrg(orgId, Pageable.unpaged()).getContent().stream()
				.filter(w -> "PRODUCTION".equalsIgnoreCase(w.getWarehouseType()))
				.sorted((a, b) -> Long.compare(a.getWarehouseId(), b.getWarehouseId()))
				.toList();
		if (prods.size() <= 1) return;
		Warehouse primary = prods.get(0);
		for (int i = 1; i < prods.size(); i++) {
			Warehouse dup = prods.get(i);
			// reassign stock rows to primary
			for (Stock s : stockRepo.findActiveByWarehouse(dup)) {
				Stock existing = stockRepo.findByMaterialAndWarehouse(s.getMaterial(), primary).orElse(null);
				if (existing != null) {
					existing.setQuantityOnHand((existing.getQuantityOnHand() != null ? existing.getQuantityOnHand() : 0.0)
							+ (s.getQuantityOnHand() != null ? s.getQuantityOnHand() : 0.0));
					existing.setQuantityReserved((existing.getQuantityReserved() != null ? existing.getQuantityReserved() : 0.0)
							+ (s.getQuantityReserved() != null ? s.getQuantityReserved() : 0.0));
					existing.recalculateAvailable();
					stockRepo.save(existing);
					stockRepo.delete(s);
				} else {
					s.setWarehouse(primary);
					stockRepo.save(s);
				}
			}
			warehouseRepo.delete(dup);
		}
	}

	/**
	 * Explicit mapping: ModelMapper cannot disambiguate Warehouse.materials[].*
	 * hierarchies for the scalar destinations, and entity graphs must never
	 * leak into JSON (infinite nesting).
	 */
	private static WarehouseDto toDto(Warehouse warehouse) {
		WarehouseDto dto = new WarehouseDto();
		dto.setWarehouseId(warehouse.getWarehouseId());
		dto.setCode(warehouse.getCode());
		dto.setWarehouseManager(warehouse.getWarehouseManager());
		dto.setOrg(warehouse.getOrg());
		dto.setLocation(warehouse.getLocation());
		dto.setStorageCapacity(warehouse.getStorageCapacity());
		dto.setCurrentUtilization(warehouse.getCurrentUtilization());
		dto.setWarehouseType(warehouse.getWarehouseType());
		dto.setCapacityMultiplier(warehouse.getCapacityMultiplier());
		dto.setCapacity(warehouse.getCapacity());
		dto.setFillOrder(warehouse.getFillOrder());
		return dto;
	}

}