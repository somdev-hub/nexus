package com.nexus.iam.service;

import java.util.Map;

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

	ResponseEntity<?> getAllProducts(String authToken, String orgIdHeader);

	ResponseEntity<?> addMaterial(Map<String, Object> materialDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getMaterial(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllMaterials(String authToken, String orgIdHeader);

	ResponseEntity<?> addWarehouse(Map<String, Object> warehouseDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getWarehouse(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllWarehouses(String authToken, String orgIdHeader);

	ResponseEntity<?> addOrder(Map<String, Object> orderDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getOrder(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllOrders(String authToken, String orgIdHeader);

	ResponseEntity<?> addPartnership(Map<String, Object> partnershipDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getPartnership(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllPartnerships(String authToken, String orgIdHeader);
}