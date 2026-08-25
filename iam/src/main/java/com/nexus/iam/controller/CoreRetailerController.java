package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllProducts(authToken, orgIdHeader, pageable);
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
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllMaterials(authToken, orgIdHeader, pageable);
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
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllWarehouses(authToken, orgIdHeader, pageable);
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
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllOrders(authToken, orgIdHeader, pageable);
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
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllPartnerships(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get Partnerships By Status for Retailer")
	@GetMapping("/partnerships/status/{status}")
	public ResponseEntity<?> getPartnershipsByStatus(@PathVariable String status,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getPartnershipsByStatus(status, authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get Active Partnerships for Retailer")
	@GetMapping("/partnerships/active")
	public ResponseEntity<?> getActivePartnerships(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getActivePartnerships(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Update Partnership Status for Retailer")
	@PostMapping("/partnerships/{id}/status")
	public ResponseEntity<?> updatePartnershipStatus(@PathVariable Long id,
			@RequestBody Map<String, Object> statusDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		String status = (String) statusDto.get("status");
		return coreRetailerService.updatePartnershipStatus(id, status, authToken, orgIdHeader);
	}

	// Partnership Invitation Endpoints
	@LogActivity("Create Partnership Invitation for Retailer")
	@PostMapping("/partnership-invitations/create")
	public ResponseEntity<?> createPartnershipInvitation(@RequestBody Map<String, Object> invitationDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createPartnershipInvitation(invitationDto, authToken, orgIdHeader);
	}

	@LogActivity("Respond to Partnership Invitation for Retailer")
	@PutMapping("/partnership-invitations/{id}/respond")
	public ResponseEntity<?> respondToPartnershipInvitation(@PathVariable Long id,
			@RequestBody Map<String, Object> responseDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.respondToPartnershipInvitation(id, responseDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Partnership Invitation for Retailer")
	@GetMapping("/partnership-invitations/{id}")
	public ResponseEntity<?> getPartnershipInvitation(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getPartnershipInvitation(id, authToken, orgIdHeader);
	}

	@LogActivity("Get Sent Partnership Invitations for Retailer")
	@GetMapping("/partnership-invitations/sent")
	public ResponseEntity<?> getSentPartnershipInvitations(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getSentPartnershipInvitations(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get Received Partnership Invitations for Retailer")
	@GetMapping("/partnership-invitations/received")
	public ResponseEntity<?> getReceivedPartnershipInvitations(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getReceivedPartnershipInvitations(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get Pending Partnership Invitations for Retailer")
	@GetMapping("/partnership-invitations/pending")
	public ResponseEntity<?> getPendingPartnershipInvitations(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getPendingPartnershipInvitations(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Withdraw Partnership Invitation for Retailer")
	@PutMapping("/partnership-invitations/{id}/withdraw")
	public ResponseEntity<?> withdrawPartnershipInvitation(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.withdrawPartnershipInvitation(id, authToken, orgIdHeader);
	}

	// Supplier Discovery Endpoints
	@LogActivity("Discover Suppliers for Retailer")
	@PostMapping("/suppliers/discover")
	public ResponseEntity<?> discoverSuppliers(@RequestBody Map<String, Object> filterDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.discoverSuppliers(filterDto, authToken, orgIdHeader, pageable);
	}

	@LogActivity("Qualify Supplier for Retailer")
	@PostMapping("/suppliers/qualify")
	public ResponseEntity<?> qualifySupplier(@RequestBody Map<String, Object> qualificationDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.qualifySupplier(qualificationDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Qualification for Retailer")
	@GetMapping("/suppliers/qualification/{id}")
	public ResponseEntity<?> getSupplierQualification(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierQualification(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Supplier Qualifications for Retailer")
	@GetMapping("/suppliers/qualifications")
	public ResponseEntity<?> getAllSupplierQualifications(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllSupplierQualifications(authToken, orgIdHeader, pageable);
	}

	// Supplier Management Endpoints
	@LogActivity("Create Supplier for Retailer")
	@PostMapping("/suppliers/add")
	public ResponseEntity<?> addSupplier(@RequestBody Map<String, Object> supplierDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addSupplier(supplierDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier for Retailer")
	@GetMapping("/suppliers/{id}")
	public ResponseEntity<?> getSupplier(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplier(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Suppliers for Retailer")
	@GetMapping("/suppliers/all")
	public ResponseEntity<?> getAllSuppliers(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllSuppliers(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get Suppliers By Account for Retailer")
	@GetMapping("/suppliers/account/{accountId}")
	public ResponseEntity<?> getSuppliersByAccount(@PathVariable Long accountId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getSuppliersByAccount(accountId, authToken, orgIdHeader, pageable);
	}
}