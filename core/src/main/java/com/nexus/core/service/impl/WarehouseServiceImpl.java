package com.nexus.core.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.WarehouseDto;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.service.WarehouseService;

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
		return new ResponseEntity<>(modelMapper.map(savedWarehouse, WarehouseDto.class), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<?> getWarehouseByIdAndOrg(Long id, Long orgId) {
		Warehouse warehouse = warehouseRepo.findByWarehouseIdAndOrg(id, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("Warehouse", "warehouseId", id));
		return new ResponseEntity<>(modelMapper.map(warehouse, WarehouseDto.class), HttpStatus.OK);
	}

	@Override
	public ResponseEntity<?> getAllWarehousesByOrgId(Long orgId, Pageable pageable) {
		Page<Warehouse> warehouses = warehouseRepo.findByOrg(orgId, pageable);
		Page<WarehouseDto> warehouseDtos = warehouses.map(w -> modelMapper.map(w, WarehouseDto.class));
		return new ResponseEntity<>(warehouseDtos, HttpStatus.OK);
	}

}