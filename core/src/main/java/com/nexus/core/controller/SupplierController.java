package com.nexus.core.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.SupplierDiscoveryDto;
import com.nexus.core.payload.SupplierDto;
import com.nexus.core.service.SupplierService;
import com.nexus.core.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/suppliers")
@RequiredArgsConstructor
public class SupplierController {

	private final SupplierService supplierService;
	private final CommonUtils commonUtils;

	@PostMapping("/add")
	@LogActivity("Create Supplier")
	public ResponseEntity<?> addSupplier(@Valid @RequestBody SupplierDto supplierDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return supplierService.addSupplier(supplierDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Supplier")
	public ResponseEntity<?> getSupplier(@PathVariable Long id, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return supplierService.getSupplierById(id);
	}

	@GetMapping("/all")
	@LogActivity("Get All Suppliers")
	public ResponseEntity<?> getAllSuppliers(@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return supplierService.getAllSuppliers(pageable);
	}

	@GetMapping("/account/{accountId}")
	@LogActivity("Get Suppliers By Account")
	public ResponseEntity<?> getSuppliersByAccount(@PathVariable Long accountId,
			@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return supplierService.getSuppliersByAccount(accountId, pageable);
	}

	@PostMapping("/discover")
	@LogActivity("Discover Suppliers")
	public ResponseEntity<?> discoverSuppliers(@Valid @RequestBody SupplierDiscoveryDto discoveryDto,
			@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		return supplierService.discoverSuppliers(discoveryDto, pageable);
	}
}