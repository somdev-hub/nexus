package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.SupplierDiscoveryDto;
import com.nexus.core.payload.SupplierDto;
import com.nexus.core.service.SupplierService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/suppliers")
@RequiredArgsConstructor
public class SupplierController {

	private final SupplierService supplierService;

	@PostMapping("/add")
	@LogActivity("Create Supplier")
	public ResponseEntity<?> addSupplier(@Valid @RequestBody SupplierDto supplierDto) {
		return supplierService.addSupplier(supplierDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Supplier")
	public ResponseEntity<?> getSupplier(@PathVariable Long id) {
		return supplierService.getSupplierById(id);
	}

	@GetMapping("/all")
	@LogActivity("Get All Suppliers")
	public ResponseEntity<?> getAllSuppliers(
			@RequestParam(required = false) Long accountId,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String location,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) String certification,
			@PageableDefault(size = 20) Pageable pageable) {
		return supplierService.getAllSuppliers(accountId, category, location, minRating, certification, pageable);
	}

	@PostMapping("/discover")
	@LogActivity("Discover Suppliers")
	public ResponseEntity<?> discoverSuppliers(@Valid @RequestBody SupplierDiscoveryDto discoveryDto,
			@PageableDefault(size = 20) Pageable pageable) {
		return supplierService.discoverSuppliers(discoveryDto, pageable);
	}
}