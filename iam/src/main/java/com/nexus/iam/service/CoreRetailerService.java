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

	// Goods Receipt Endpoints
	ResponseEntity<?> createGoodsReceipt(Map<String, Object> grDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getGoodsReceipt(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllGoodsReceipts(String authToken, String orgIdHeader, Pageable pageable,
			String status, Long purchaseOrderId, Long supplierId);

	ResponseEntity<?> updateGoodsReceipt(Long id, Map<String, Object> grDto, String authToken, String orgIdHeader);

	ResponseEntity<?> transitionGoodsReceiptStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgIdHeader);

	// Invoice Endpoints
	ResponseEntity<?> createInvoice(Map<String, Object> invoiceDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getInvoice(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllInvoices(String authToken, String orgIdHeader, Pageable pageable,
			String status, Long purchaseOrderId, Long supplierId);

	ResponseEntity<?> updateInvoice(Long id, Map<String, Object> invoiceDto, String authToken, String orgIdHeader);

	ResponseEntity<?> transitionInvoiceStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgIdHeader);

	// Three-Way Matching Endpoints
	ResponseEntity<?> performThreeWayMatch(Long purchaseOrderId, String authToken, String orgIdHeader);

	ResponseEntity<?> canInvoice(Long purchaseOrderId, String authToken, String orgIdHeader);

	ResponseEntity<?> getMatchingSummary(Long purchaseOrderId, String authToken, String orgIdHeader);

	ResponseEntity<?> validateInvoiceMatch(Map<String, Object> invoiceDto, String authToken, String orgIdHeader);

	// Supplier Performance Endpoints
	ResponseEntity<?> createSupplierPerformance(Map<String, Object> performanceDto, String authToken,
			String orgIdHeader);

	ResponseEntity<?> getSupplierPerformance(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierPerformanceBySupplier(Long supplierId, String authToken, String orgIdHeader,
			Pageable pageable);

	ResponseEntity<?> getAllSupplierPerformance(String authToken, String orgIdHeader, Pageable pageable);

	ResponseEntity<?> getSupplierPerformanceByPeriod(String authToken, String orgIdHeader, Pageable pageable,
			java.sql.Date startDate, java.sql.Date endDate);

	ResponseEntity<?> getSupplierPerformanceBySupplierAndPeriod(Long supplierId, String authToken, String orgIdHeader,
			Pageable pageable,
			java.sql.Date startDate, java.sql.Date endDate);

	ResponseEntity<?> getSupplierPerformanceByTier(String authToken, String orgIdHeader, Pageable pageable,
			String tier);

	ResponseEntity<?> getLatestSupplierPerformance(Long supplierId, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierPerformanceSummaryByAccount(String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierPerformanceSummaryBySupplier(Long supplierId, String authToken, String orgIdHeader);

	ResponseEntity<?> calculateSupplierPerformance(Long supplierId, String authToken, String orgIdHeader,
			java.sql.Date startDate, java.sql.Date endDate, String calculatedBy);

	ResponseEntity<?> updateSupplierPerformance(Long id, Map<String, Object> performanceDto, String authToken,
			String orgIdHeader);

	ResponseEntity<?> deleteSupplierPerformance(Long id, String authToken, String orgIdHeader);

	// Supplier Contract Endpoints
	ResponseEntity<?> createSupplierContract(Map<String, Object> contractDto, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierContract(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierContractByNumber(String contractNumber, String authToken, String orgIdHeader);

	ResponseEntity<?> getAllSupplierContracts(
			String authToken, String orgIdHeader, Pageable pageable,
			String status, Long supplierId, String contractType,
			java.sql.Date effectiveStartDate, java.sql.Date effectiveEndDate,
			java.sql.Date expiryStartDate, java.sql.Date expiryEndDate,
			Boolean expiringOnly, java.sql.Date expiringBeforeDate,
			Boolean autoRenewalOnly, java.sql.Date autoRenewalBeforeDate);

	ResponseEntity<?> getExpiringSupplierContracts(String authToken, String orgIdHeader, java.sql.Date beforeDate);

	ResponseEntity<?> getAutoRenewalSupplierContracts(String authToken, String orgIdHeader, java.sql.Date beforeDate);

	ResponseEntity<?> getActiveSupplierContractBySupplierAndDate(Long supplierId, String authToken, String orgIdHeader,
			java.sql.Date date);

	ResponseEntity<?> getSupplierContractSummary(String authToken, String orgIdHeader);

	ResponseEntity<?> updateSupplierContract(Long id, Map<String, Object> contractDto, String authToken,
			String orgIdHeader);

	ResponseEntity<?> updateSupplierContractStatus(Long id, String status, String reason, String authToken,
			String orgIdHeader);

	ResponseEntity<?> uploadSupplierContractDocument(Long id, MultipartFile file, String documentName, String remarks,
			String authToken, String orgIdHeader);

	ResponseEntity<?> getSupplierContractDocument(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> deleteSupplierContractDocument(Long id, String authToken, String orgIdHeader);

	ResponseEntity<?> approveSupplierContract(Long id, String approvedBy, String authToken, String orgIdHeader);

	ResponseEntity<?> rejectSupplierContract(Long id, String rejectionReason, String authToken, String orgIdHeader);

	ResponseEntity<?> terminateSupplierContract(Long id, String reason, String authToken, String orgIdHeader);

	ResponseEntity<?> suspendSupplierContract(Long id, String reason, String authToken, String orgIdHeader);

	ResponseEntity<?> renewSupplierContract(Long id, java.sql.Date newExpiryDate, String authToken, String orgIdHeader);

	ResponseEntity<?> deleteSupplierContract(Long id, String authToken, String orgIdHeader);
}