package com.nexus.core.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.nexus.core.entities.Warehouse;
import com.nexus.core.exception.ResourceNotFoundException;
import com.nexus.core.payload.ErrorResponse;
import com.nexus.core.payload.WarehouseDto;
import com.nexus.core.repository.WarehouseRepo;
import com.nexus.core.service.WarehouseService;

@Service
public class WarehouseServiceImpl implements WarehouseService {

	@Autowired
	private WarehouseRepo warehouseRepo;

	@Autowired
	private ModelMapper modelMapper;

	@Override
	public ResponseEntity<?> addWarehouse(WarehouseDto warehouseDto) {
		if (ObjectUtils.isEmpty(warehouseDto) || ObjectUtils.isEmpty(warehouseDto.getOrg())) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse("Empty Details sent", HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()), "Necessary details are not sent!"),
					HttpStatus.BAD_REQUEST);

		}
		try {

			Warehouse warehouse = modelMapper.map(warehouseDto, Warehouse.class);
			Warehouse savedWarehouse = warehouseRepo.save(warehouse);
			return new ResponseEntity<>(modelMapper.map(savedWarehouse, WarehouseDto.class), HttpStatus.CREATED);

		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse("Failed to add warehouse", HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()), e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public ResponseEntity<?> getWarehouseByIdAndOrg(Long id, Long orgId) {
		if (ObjectUtils.isEmpty(id) || ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Warehouse ID and Organization ID cannot be null or empty",
							HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							"Invalid Warehouse ID or Organization ID"),
					HttpStatus.BAD_REQUEST);
		}
		try {
			Warehouse warehouse = warehouseRepo.findByIdAndOrg(id, orgId).orElse(null);
			if (ObjectUtils.isEmpty(warehouse)) {
				return new ResponseEntity<ErrorResponse>(
						new ErrorResponse(
								"Warehouse not found in organization",
								HttpStatus.NOT_FOUND.value(),
								Timestamp.valueOf(LocalDateTime.now()),
								"No warehouse found with the given ID in the organization"),
						HttpStatus.NOT_FOUND);
			}
			return new ResponseEntity<>(modelMapper.map(warehouse, WarehouseDto.class), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Failed to retrieve warehouse",
							HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<?> getAllWarehousesByOrgId(Long orgId) {
		if (ObjectUtils.isEmpty(orgId)) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Organization ID cannot be null or empty",
							HttpStatus.BAD_REQUEST.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							"Invalid Organization ID"),
					HttpStatus.BAD_REQUEST);

		}
		try {
			List<Warehouse> warehouses = warehouseRepo.findByOrg(orgId).orElseThrow(() -> {
				throw new ResourceNotFoundException("Warehouses", "orgId", orgId);
			});
			List<WarehouseDto> warehouseDtos = new java.util.ArrayList<>();
			for (Warehouse warehouse : warehouses) {
				warehouseDtos.add(modelMapper.map(warehouse, WarehouseDto.class));
			}
			return new ResponseEntity<>(warehouseDtos, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<ErrorResponse>(
					new ErrorResponse(
							"Failed to retrieve warehouses",
							HttpStatus.INTERNAL_SERVER_ERROR.value(),
							Timestamp.valueOf(LocalDateTime.now()),
							e.getMessage()),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}