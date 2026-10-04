package com.nexus.core.service.implementations;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.model.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.WarehouseDto;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.service.interfaces.WarehouseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

	private final WarehouseRepo warehouseRepo;
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
		Page<Warehouse> warehouses = warehouseRepo.findByOrg(orgId, pageable);
		Page<WarehouseDto> warehouseDtos = warehouses.map(WarehouseServiceImpl::toDto);
		return new ResponseEntity<>(warehouseDtos, HttpStatus.OK);
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
		return dto;
	}

}