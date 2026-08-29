package com.nexus.iam.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

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

	// Partnership Agreement with DMS Integration
	ResponseEntity<?> uploadPartnershipAgreement(Long id, MultipartFile file, String documentName, String remarks,
			String authToken, String orgIdHeader);

	ResponseEntity<?> getPartnershipAgreement(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> deletePartnershipAgreement(Long id, String authToken, String orgIdHeader);

	// Partnership Lifecycle Management
	ResponseEntity<?> transitionPartnershipStatus(Long id, Map<String, Object> transitionDto, String authToken,
			String orgIdHeader);

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

	ResponseEntity<?> updateSupplierQualificationStatus(Long id, String status, String rejectionReason,
			String authToken, String orgIdHeader);

	// Supplier Management Endpoints
	ResponseEntity<?> addSupplier(Map<String, Object> supplierDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplier(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllSuppliers(String authToken, String orgIdHeader, Pageable pageable,
			Long accountId, String category, String location, Double minRating, String certification);

	// Purchase Order Endpoints
	ResponseEntity<?> createPurchaseOrder(Map<String, Object> poDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getPurchaseOrder(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllPurchaseOrders(String authToken, String orgIdHeader, Pageable pageable,
			String status);

	ResponseEntity<?> updatePurchaseOrder(Long id, Map<String, Object> poDto, String authToken, String orgIdHeader);

	ResponseEntity<?> transitionPurchaseOrderStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgIdHeader);

	ResponseEntity<?> createPurchaseOrderAmendment(Long parentPoId, Map<String, Object> amendmentDto, String authToken,
			String orgIdHeader);

	ResponseEntity<?> getPurchaseOrderAmendments(Long parentPoId, String authToken, String orgIdHeader);

	// Stock/Inventory Endpoints
	ResponseEntity<?> addStock(Map<String, Object> stockDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getStock(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllStocks(String authToken, String orgIdHeader, Pageable pageable,
			Long warehouseId, Long materialId, Boolean belowReorderPoint, Boolean atOrBelowMinLevel);

	ResponseEntity<?> getInventoryValuation(String authToken, String orgIdHeader);

	ResponseEntity<?> getWarehouseInventoryValuation(Long warehouseId, String authToken, String orgIdHeader);

	ResponseEntity<?> adjustStock(Long stockId, Double quantity, String reason, String referenceType, Long referenceId,
			String authToken, String orgIdHeader);

	ResponseEntity<?> reserveStock(Long stockId, Double quantity, String referenceType, Long referenceId,
			String authToken, String orgIdHeader);

	ResponseEntity<?> releaseReservation(Long stockId, Double quantity, String referenceType, Long referenceId,
			String authToken, String orgIdHeader);

	ResponseEntity<?> transferStock(Long fromStockId, Long toWarehouseId, Double quantity, String reason,
			String authToken, String orgIdHeader);

	ResponseEntity<?> recordCycleCount(Long stockId, Double countedQuantity, String countedBy,
			String authToken, String orgIdHeader);

	ResponseEntity<?> getReorderSuggestions(String authToken, String orgIdHeader);

	ResponseEntity<?> updateStockSettings(Long stockId, Double reorderPoint, Double reorderQuantity,
			Double minStockLevel, Double maxStockLevel, String authToken, String orgIdHeader);

	// Stock Movement Endpoints
	ResponseEntity<?> addStockMovement(Map<String, Object> movementDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getStockMovement(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllStockMovements(String authToken, String orgIdHeader, Pageable pageable,
			Long stockId, Long warehouseId, String type, String referenceType, Long referenceId,
			String batchNumber, java.sql.Timestamp beforeDate, java.sql.Timestamp start,
			java.sql.Timestamp end, Long materialId);

	ResponseEntity<?> getMovementSummary(java.sql.Timestamp start, java.sql.Timestamp end, String authToken,
			String orgIdHeader);
}