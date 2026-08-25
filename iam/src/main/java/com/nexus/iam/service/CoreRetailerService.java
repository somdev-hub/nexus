package com.nexus.iam.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/**
 * Core Retailer Service Interface
 * <p>
 * Defines retailer-specific Core module operations through IAM gateway.
 * All HTTP calls to Core module are handled here.
 */
public interface CoreRetailerService {

	ResponseEntity<?> addProduct(Map<String, Object> productDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getProduct(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllProducts(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> addMaterial(Map<String, Object> materialDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getMaterial(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllMaterials(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> addWarehouse(Map<String, Object> warehouseDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getWarehouse(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllWarehouses(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> addOrder(Map<String, Object> orderDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getOrder(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllOrders(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> addPartnership(Map<String, Object> partnershipDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getPartnership(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllPartnerships(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> getPartnershipsByStatus(String status, String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> getActivePartnerships(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> updatePartnershipStatus(Long id, String status, String authToken, String orgIdHeader);

	// Partnership Invitation Endpoints
	ResponseEntity<?> createPartnershipInvitation(Map<String, Object> invitationDto, String authToken,
			String orgIdHeader);

	ResponseEntity<?> respondToPartnershipInvitation(Long id, Map<String, Object> responseDto, String authToken,
			String orgIdHeader);

	ResponseEntity<?> getPartnershipInvitation(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getSentPartnershipInvitations(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> getReceivedPartnershipInvitations(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> getPendingPartnershipInvitations(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> withdrawPartnershipInvitation(Long id, String authToken, String orgIdHeader);

	// Supplier Discovery Endpoints
	ResponseEntity<?> discoverSuppliers(Map<String, Object> filterDto, String authToken, String orgIdHeader,
			Pageable pageable);

	ResponseEntity<?> qualifySupplier(Map<String, Object> qualificationDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierQualification(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllSupplierQualifications(String authToken, String orgIdHeader, Pageable pageable);

	// Supplier Management Endpoints
	ResponseEntity<?> addSupplier(Map<String, Object> supplierDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplier(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllSuppliers(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> getSuppliersByAccount(Long accountId, String authToken, String orgIdHeader, Pageable pageable);
}