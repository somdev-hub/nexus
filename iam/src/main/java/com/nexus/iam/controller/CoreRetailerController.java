package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.CoreRetailerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Retailer Controller
 * <p>
 * Handles retailer-specific Core module APIs through IAM gateway.
 * Frontend calls these endpoints for retailer operations.
 */
@Slf4j
@RestController
@RequestMapping("/iam/core/retailer")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CoreRetailerController {

	private final CoreRetailerService coreRetailerService;

	@LogActivity("Create Product for Retailer")
	@PostMapping("/products/add")
	public ResponseEntity<?> addProduct(@RequestBody Map<String, Object> productDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addProduct(productDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Product for Retailer")
	@GetMapping("/products/{id}")
	public ResponseEntity<?> getProduct(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getProduct(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Products for Retailer")
	@GetMapping("/products/all")
	public ResponseEntity<?> getAllProducts(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getAllProducts(authToken, orgIdHeader);
	}

	@LogActivity("Create Material for Retailer")
	@PostMapping("/materials/add")
	public ResponseEntity<?> addMaterial(@RequestBody Map<String, Object> materialDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addMaterial(materialDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Material for Retailer")
	@GetMapping("/materials/{id}")
	public ResponseEntity<?> getMaterial(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getMaterial(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Materials for Retailer")
	@GetMapping("/materials/all")
	public ResponseEntity<?> getAllMaterials(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getAllMaterials(authToken, orgIdHeader);
	}

	@LogActivity("Create Warehouse for Retailer")
	@PostMapping("/warehouses/add")
	public ResponseEntity<?> addWarehouse(@RequestBody Map<String, Object> warehouseDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addWarehouse(warehouseDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Warehouse for Retailer")
	@GetMapping("/warehouses/{id}")
	public ResponseEntity<?> getWarehouse(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getWarehouse(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Warehouses for Retailer")
	@GetMapping("/warehouses/all")
	public ResponseEntity<?> getAllWarehouses(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getAllWarehouses(authToken, orgIdHeader);
	}

	@LogActivity("Create Order for Retailer")
	@PostMapping("/orders/add")
	public ResponseEntity<?> addOrder(@RequestBody Map<String, Object> orderDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addOrder(orderDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Order for Retailer")
	@GetMapping("/orders/{id}")
	public ResponseEntity<?> getOrder(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getOrder(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Orders for Retailer")
	@GetMapping("/orders/all")
	public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getAllOrders(authToken, orgIdHeader);
	}

	@LogActivity("Create Partnership for Retailer")
	@PostMapping("/partnerships/add")
	public ResponseEntity<?> addPartnership(@RequestBody Map<String, Object> partnershipDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addPartnership(partnershipDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Partnership for Retailer")
	@GetMapping("/partnerships/{id}")
	public ResponseEntity<?> getPartnership(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getPartnership(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Partnerships for Retailer")
	@GetMapping("/partnerships/all")
	public ResponseEntity<?> getAllPartnerships(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getAllPartnerships(authToken, orgIdHeader);
	}
}