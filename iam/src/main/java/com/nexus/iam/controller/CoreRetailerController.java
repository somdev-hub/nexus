package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

	// Partnership Agreement with DMS Integration
	@LogActivity("Upload Partnership Agreement for Retailer")
	@PostMapping("/partnerships/{id}/agreement")
	public ResponseEntity<?> uploadPartnershipAgreement(@PathVariable Long id,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "documentName", required = false) String documentName,
			@RequestParam(value = "remarks", required = false) String remarks,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.uploadPartnershipAgreement(id, file, documentName, remarks, authToken, orgIdHeader);
	}

	@LogActivity("Get Partnership Agreement for Retailer")
	@GetMapping("/partnerships/{id}/agreement")
	public ResponseEntity<?> getPartnershipAgreement(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getPartnershipAgreement(id, authToken, orgIdHeader);
	}

	@LogActivity("Delete Partnership Agreement for Retailer")
	@DeleteMapping("/partnerships/{id}/agreement")
	public ResponseEntity<?> deletePartnershipAgreement(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.deletePartnershipAgreement(id, authToken, orgIdHeader);
	}

	// Partnership Lifecycle Management
	@LogActivity("Transition Partnership Status for Retailer")
	@PostMapping("/partnerships/{id}/transition")
	public ResponseEntity<?> transitionPartnershipStatus(@PathVariable Long id,
			@RequestBody Map<String, Object> transitionDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.transitionPartnershipStatus(id, transitionDto, authToken, orgIdHeader);
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

	@LogActivity("Update Supplier Qualification Status for Retailer")
	@PutMapping("/suppliers/qualification/{id}/status")
	public ResponseEntity<?> updateSupplierQualificationStatus(@PathVariable Long id,
			@RequestParam String status,
			@RequestParam(required = false) String rejectionReason,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateSupplierQualificationStatus(id, status, rejectionReason, authToken,
				orgIdHeader);
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
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) Long accountId,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String location,
			@RequestParam(required = false) Double minRating,
			@RequestParam(required = false) String certification) {
		return coreRetailerService.getAllSuppliers(authToken, orgIdHeader, pageable, accountId, category, location,
				minRating, certification);
	}

	// Purchase Order Endpoints
	@LogActivity("Create Purchase Order for Retailer")
	@PostMapping("/purchase-orders/create")
	public ResponseEntity<?> createPurchaseOrder(@RequestBody Map<String, Object> poDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createPurchaseOrder(poDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Purchase Order for Retailer")
	@GetMapping("/purchase-orders/{id}")
	public ResponseEntity<?> getPurchaseOrder(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getPurchaseOrder(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Purchase Orders for Retailer")
	@GetMapping("/purchase-orders/all")
	public ResponseEntity<?> getAllPurchaseOrders(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) String status) {
		return coreRetailerService.getAllPurchaseOrders(authToken, orgIdHeader, pageable, status);
	}

	@LogActivity("Update Purchase Order for Retailer")
	@PutMapping("/purchase-orders/{id}/update")
	public ResponseEntity<?> updatePurchaseOrder(@PathVariable Long id,
			@RequestBody Map<String, Object> poDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updatePurchaseOrder(id, poDto, authToken, orgIdHeader);
	}

	@LogActivity("Transition Purchase Order Status for Retailer")
	@PutMapping("/purchase-orders/{id}/transition")
	public ResponseEntity<?> transitionPurchaseOrderStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.transitionPurchaseOrderStatus(id, newStatus, params, authToken, orgIdHeader);
	}

	@LogActivity("Create Purchase Order Amendment for Retailer")
	@PostMapping("/purchase-orders/{parentPoId}/amend")
	public ResponseEntity<?> createPurchaseOrderAmendment(@PathVariable Long parentPoId,
			@RequestBody Map<String, Object> amendmentDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createPurchaseOrderAmendment(parentPoId, amendmentDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Purchase Order Amendments for Retailer")
	@GetMapping("/purchase-orders/{parentPoId}/amendments")
	public ResponseEntity<?> getPurchaseOrderAmendments(@PathVariable Long parentPoId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getPurchaseOrderAmendments(parentPoId, authToken, orgIdHeader);
	}

	// Stock/Inventory Endpoints
	@LogActivity("Add Stock for Retailer")
	@PostMapping("/stock/add")
	public ResponseEntity<?> addStock(@RequestBody Map<String, Object> stockDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addStock(stockDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Stock for Retailer")
	@GetMapping("/stock/{id}")
	public ResponseEntity<?> getStock(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getStock(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Stocks for Retailer")
	@GetMapping("/stock/all")
	public ResponseEntity<?> getAllStocks(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) Long warehouseId,
			@RequestParam(required = false) Long materialId,
			@RequestParam(required = false) Boolean belowReorderPoint,
			@RequestParam(required = false) Boolean atOrBelowMinLevel) {
		return coreRetailerService.getAllStocks(authToken, orgIdHeader, pageable, warehouseId, materialId,
				belowReorderPoint, atOrBelowMinLevel);
	}

	@LogActivity("Get Inventory Valuation for Retailer")
	@GetMapping("/stock/valuation")
	public ResponseEntity<?> getInventoryValuation(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getInventoryValuation(authToken, orgIdHeader);
	}

	@LogActivity("Get Warehouse Inventory Valuation for Retailer")
	@GetMapping("/stock/valuation/warehouse/{warehouseId}")
	public ResponseEntity<?> getWarehouseInventoryValuation(@PathVariable Long warehouseId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getWarehouseInventoryValuation(warehouseId, authToken, orgIdHeader);
	}

	@LogActivity("Adjust Stock for Retailer")
	@PostMapping("/stock/{stockId}/adjust")
	public ResponseEntity<?> adjustStock(@PathVariable Long stockId,
			@RequestParam Double quantity,
			@RequestParam String reason,
			@RequestParam String referenceType,
			@RequestParam Long referenceId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.adjustStock(stockId, quantity, reason, referenceType, referenceId, authToken,
				orgIdHeader);
	}

	@LogActivity("Reserve Stock for Retailer")
	@PostMapping("/stock/{stockId}/reserve")
	public ResponseEntity<?> reserveStock(@PathVariable Long stockId,
			@RequestParam Double quantity,
			@RequestParam String referenceType,
			@RequestParam Long referenceId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.reserveStock(stockId, quantity, referenceType, referenceId, authToken, orgIdHeader);
	}

	@LogActivity("Release Reservation for Retailer")
	@PostMapping("/stock/{stockId}/release-reservation")
	public ResponseEntity<?> releaseReservation(@PathVariable Long stockId,
			@RequestParam Double quantity,
			@RequestParam String referenceType,
			@RequestParam Long referenceId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.releaseReservation(stockId, quantity, referenceType, referenceId, authToken,
				orgIdHeader);
	}

	@LogActivity("Transfer Stock for Retailer")
	@PostMapping("/stock/transfer")
	public ResponseEntity<?> transferStock(@RequestParam Long fromStockId,
			@RequestParam Long toWarehouseId,
			@RequestParam Double quantity,
			@RequestParam String reason,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.transferStock(fromStockId, toWarehouseId, quantity, reason, authToken, orgIdHeader);
	}

	@LogActivity("Record Cycle Count for Retailer")
	@PostMapping("/stock/{stockId}/cycle-count")
	public ResponseEntity<?> recordCycleCount(@PathVariable Long stockId,
			@RequestParam Double countedQuantity,
			@RequestParam String countedBy,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.recordCycleCount(stockId, countedQuantity, countedBy, authToken, orgIdHeader);
	}

	@LogActivity("Get Reorder Suggestions for Retailer")
	@GetMapping("/stock/reorder-suggestions")
	public ResponseEntity<?> getReorderSuggestions(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getReorderSuggestions(authToken, orgIdHeader);
	}

	@LogActivity("Update Stock Settings for Retailer")
	@PutMapping("/stock/{stockId}/settings")
	public ResponseEntity<?> updateStockSettings(@PathVariable Long stockId,
			@RequestParam Double reorderPoint,
			@RequestParam Double reorderQuantity,
			@RequestParam Double minStockLevel,
			@RequestParam Double maxStockLevel,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateStockSettings(stockId, reorderPoint, reorderQuantity, minStockLevel,
				maxStockLevel, authToken, orgIdHeader);
	}

	// Stock Movement Endpoints
	@LogActivity("Add Stock Movement for Retailer")
	@PostMapping("/stock-movements/add")
	public ResponseEntity<?> addStockMovement(@RequestBody Map<String, Object> movementDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.addStockMovement(movementDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Stock Movement for Retailer")
	@GetMapping("/stock-movements/{id}")
	public ResponseEntity<?> getStockMovement(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getStockMovement(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Stock Movements for Retailer")
	@GetMapping("/stock-movements/all")
	public ResponseEntity<?> getAllStockMovements(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) Long stockId,
			@RequestParam(required = false) Long warehouseId,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String referenceType,
			@RequestParam(required = false) Long referenceId,
			@RequestParam(required = false) String batchNumber,
			@RequestParam(required = false) java.sql.Timestamp beforeDate,
			@RequestParam(required = false) java.sql.Timestamp start,
			@RequestParam(required = false) java.sql.Timestamp end,
			@RequestParam(required = false) Long materialId) {
		return coreRetailerService.getAllStockMovements(authToken, orgIdHeader, pageable, stockId, warehouseId, type,
				referenceType, referenceId, batchNumber, beforeDate, start, end, materialId);
	}

	@LogActivity("Get Movement Summary for Retailer")
	@GetMapping("/stock-movements/summary")
	public ResponseEntity<?> getMovementSummary(@RequestParam java.sql.Timestamp start,
			@RequestParam java.sql.Timestamp end,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getMovementSummary(start, end, authToken, orgIdHeader);
	}

	// Goods Receipt Endpoints
	@LogActivity("Create Goods Receipt for Retailer")
	@PostMapping("/goods-receipts/add")
	public ResponseEntity<?> createGoodsReceipt(@RequestBody Map<String, Object> grDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createGoodsReceipt(grDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Goods Receipt for Retailer")
	@GetMapping("/goods-receipts/{id}")
	public ResponseEntity<?> getGoodsReceipt(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getGoodsReceipt(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Goods Receipts for Retailer")
	@GetMapping("/goods-receipts/all")
	public ResponseEntity<?> getAllGoodsReceipts(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long purchaseOrderId,
			@RequestParam(required = false) Long supplierId) {
		return coreRetailerService.getAllGoodsReceipts(authToken, orgIdHeader, pageable, status, purchaseOrderId,
				supplierId);
	}

	@LogActivity("Update Goods Receipt for Retailer")
	@PutMapping("/goods-receipts/{id}/update")
	public ResponseEntity<?> updateGoodsReceipt(@PathVariable Long id,
			@RequestBody Map<String, Object> grDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateGoodsReceipt(id, grDto, authToken, orgIdHeader);
	}

	@LogActivity("Transition Goods Receipt Status for Retailer")
	@PutMapping("/goods-receipts/{id}/transition")
	public ResponseEntity<?> transitionGoodsReceiptStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.transitionGoodsReceiptStatus(id, newStatus, params, authToken, orgIdHeader);
	}

	// Invoice Endpoints
	@LogActivity("Create Invoice for Retailer")
	@PostMapping("/invoices/add")
	public ResponseEntity<?> createInvoice(@RequestBody Map<String, Object> invoiceDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createInvoice(invoiceDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Invoice for Retailer")
	@GetMapping("/invoices/{id}")
	public ResponseEntity<?> getInvoice(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getInvoice(id, authToken, orgIdHeader);
	}

	@LogActivity("Get All Invoices for Retailer")
	@GetMapping("/invoices/all")
	public ResponseEntity<?> getAllInvoices(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long purchaseOrderId,
			@RequestParam(required = false) Long supplierId) {
		return coreRetailerService.getAllInvoices(authToken, orgIdHeader, pageable, status, purchaseOrderId,
				supplierId);
	}

	@LogActivity("Update Invoice for Retailer")
	@PutMapping("/invoices/{id}/update")
	public ResponseEntity<?> updateInvoice(@PathVariable Long id,
			@RequestBody Map<String, Object> invoiceDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateInvoice(id, invoiceDto, authToken, orgIdHeader);
	}

	@LogActivity("Transition Invoice Status for Retailer")
	@PutMapping("/invoices/{id}/transition")
	public ResponseEntity<?> transitionInvoiceStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.transitionInvoiceStatus(id, newStatus, params, authToken, orgIdHeader);
	}

	// Three-Way Matching Endpoints
	@LogActivity("Perform Three-Way Match for Retailer")
	@GetMapping("/three-way-match/match/{purchaseOrderId}")
	public ResponseEntity<?> performThreeWayMatch(@PathVariable Long purchaseOrderId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.performThreeWayMatch(purchaseOrderId, authToken, orgIdHeader);
	}

	@LogActivity("Check Can Invoice for Retailer")
	@GetMapping("/three-way-match/can-invoice/{purchaseOrderId}")
	public ResponseEntity<?> canInvoice(@PathVariable Long purchaseOrderId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.canInvoice(purchaseOrderId, authToken, orgIdHeader);
	}

	@LogActivity("Get Matching Summary for Retailer")
	@GetMapping("/three-way-match/summary/{purchaseOrderId}")
	public ResponseEntity<?> getMatchingSummary(@PathVariable Long purchaseOrderId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getMatchingSummary(purchaseOrderId, authToken, orgIdHeader);
	}

	@LogActivity("Validate Invoice Match for Retailer")
	@PostMapping("/three-way-match/validate-invoice")
	public ResponseEntity<?> validateInvoiceMatch(@RequestBody Map<String, Object> invoiceDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.validateInvoiceMatch(invoiceDto, authToken, orgIdHeader);
	}

	// Supplier Performance Endpoints
	@LogActivity("Create Supplier Performance for Retailer")
	@PostMapping("/supplier-performance/create")
	public ResponseEntity<?> createSupplierPerformance(@RequestBody Map<String, Object> performanceDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createSupplierPerformance(performanceDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Performance for Retailer")
	@GetMapping("/supplier-performance/{id}")
	public ResponseEntity<?> getSupplierPerformance(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierPerformance(id, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Performance by Supplier for Retailer")
	@GetMapping("/supplier-performance/supplier/{supplierId}")
	public ResponseEntity<?> getSupplierPerformanceBySupplier(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getSupplierPerformanceBySupplier(supplierId, authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get All Supplier Performance for Retailer")
	@GetMapping("/supplier-performance/all")
	public ResponseEntity<?> getAllSupplierPerformance(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllSupplierPerformance(authToken, orgIdHeader, pageable);
	}

	@LogActivity("Get Supplier Performance by Period for Retailer")
	@GetMapping("/supplier-performance/period")
	public ResponseEntity<?> getSupplierPerformanceByPeriod(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam java.sql.Date startDate,
			@RequestParam java.sql.Date endDate) {
		return coreRetailerService.getSupplierPerformanceByPeriod(authToken, orgIdHeader, pageable, startDate, endDate);
	}

	@LogActivity("Get Supplier Performance by Supplier and Period for Retailer")
	@GetMapping("/supplier-performance/supplier/{supplierId}/period")
	public ResponseEntity<?> getSupplierPerformanceBySupplierAndPeriod(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam java.sql.Date startDate,
			@RequestParam java.sql.Date endDate) {
		return coreRetailerService.getSupplierPerformanceBySupplierAndPeriod(supplierId, authToken, orgIdHeader,
				pageable, startDate, endDate);
	}

	@LogActivity("Get Supplier Performance by Tier for Retailer")
	@GetMapping("/supplier-performance/tier/{tier}")
	public ResponseEntity<?> getSupplierPerformanceByTier(@PathVariable String tier,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getSupplierPerformanceByTier(authToken, orgIdHeader, pageable, tier);
	}

	@LogActivity("Get Latest Supplier Performance for Retailer")
	@GetMapping("/supplier-performance/supplier/{supplierId}/latest")
	public ResponseEntity<?> getLatestSupplierPerformance(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getLatestSupplierPerformance(supplierId, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Performance Summary by Account for Retailer")
	@GetMapping("/supplier-performance/summary/account")
	public ResponseEntity<?> getSupplierPerformanceSummaryByAccount(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierPerformanceSummaryByAccount(authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Performance Summary by Supplier for Retailer")
	@GetMapping("/supplier-performance/summary/supplier/{supplierId}")
	public ResponseEntity<?> getSupplierPerformanceSummaryBySupplier(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierPerformanceSummaryBySupplier(supplierId, authToken, orgIdHeader);
	}

	@LogActivity("Calculate Supplier Performance for Retailer")
	@PostMapping("/supplier-performance/calculate")
	public ResponseEntity<?> calculateSupplierPerformance(@RequestParam Long supplierId,
			@RequestParam java.sql.Date startDate,
			@RequestParam java.sql.Date endDate,
			@RequestParam String calculatedBy,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.calculateSupplierPerformance(supplierId, authToken, orgIdHeader, startDate, endDate,
				calculatedBy);
	}

	@LogActivity("Update Supplier Performance for Retailer")
	@PutMapping("/supplier-performance/{id}/update")
	public ResponseEntity<?> updateSupplierPerformance(@PathVariable Long id,
			@RequestBody Map<String, Object> performanceDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateSupplierPerformance(id, performanceDto, authToken, orgIdHeader);
	}

	@LogActivity("Delete Supplier Performance for Retailer")
	@DeleteMapping("/supplier-performance/{id}/delete")
	public ResponseEntity<?> deleteSupplierPerformance(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.deleteSupplierPerformance(id, authToken, orgIdHeader);
	}

	// Supplier Risk Monitoring Endpoints (FR-RET-024)
	@LogActivity("Create Supplier Risk Monitoring for Retailer")
	@PostMapping("/supplier-risk-monitoring/create")
	public ResponseEntity<?> createSupplierRiskMonitoring(@RequestBody Map<String, Object> riskDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createSupplierRiskMonitoring(riskDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Monitoring for Retailer")
	@GetMapping("/supplier-risk-monitoring/{id}")
	public ResponseEntity<?> getSupplierRiskMonitoring(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskMonitoring(id, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Monitoring by Supplier for Retailer")
	@GetMapping("/supplier-risk-monitoring/supplier/{supplierId}")
	public ResponseEntity<?> getSupplierRiskMonitoringBySupplier(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskMonitoringBySupplier(supplierId, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Monitoring by Partnership for Retailer")
	@GetMapping("/supplier-risk-monitoring/partnership/{partnershipId}")
	public ResponseEntity<?> getSupplierRiskMonitoringByPartnership(@PathVariable Long partnershipId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskMonitoringByPartnership(partnershipId, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Monitoring by Risk Level for Retailer")
	@GetMapping("/supplier-risk-monitoring/risk-level/{riskLevel}")
	public ResponseEntity<?> getSupplierRiskMonitoringByRiskLevel(@PathVariable String riskLevel,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskMonitoringByRiskLevel(riskLevel, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Monitoring Due for Review for Retailer")
	@GetMapping("/supplier-risk-monitoring/due-for-review")
	public ResponseEntity<?> getSupplierRiskMonitoringDueForReview(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskMonitoringDueForReview(authToken, orgIdHeader);
	}

	@LogActivity("Update Supplier Risk Monitoring for Retailer")
	@PutMapping("/supplier-risk-monitoring/{id}/update")
	public ResponseEntity<?> updateSupplierRiskMonitoring(@PathVariable Long id,
			@RequestBody Map<String, Object> riskDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateSupplierRiskMonitoring(id, riskDto, authToken, orgIdHeader);
	}

	@LogActivity("Delete Supplier Risk Monitoring for Retailer")
	@DeleteMapping("/supplier-risk-monitoring/{id}")
	public ResponseEntity<?> deleteSupplierRiskMonitoring(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.deleteSupplierRiskMonitoring(id, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Summary for Retailer")
	@GetMapping("/supplier-risk-monitoring/supplier/{supplierId}/summary")
	public ResponseEntity<?> getSupplierRiskSummary(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskSummary(supplierId, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Risk Monitoring by Category for Retailer")
	@GetMapping("/supplier-risk-monitoring/supplier/{supplierId}/category/{riskCategory}")
	public ResponseEntity<?> getSupplierRiskMonitoringByCategory(@PathVariable Long supplierId,
			@PathVariable String riskCategory,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierRiskMonitoringByCategory(supplierId, riskCategory, authToken,
				orgIdHeader);
	}

	@LogActivity("Get All Supplier Risk Monitoring for Retailer")
	@GetMapping("/supplier-risk-monitoring/all")
	public ResponseEntity<?> getAllSupplierRiskMonitoring(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable) {
		return coreRetailerService.getAllSupplierRiskMonitoring(authToken, orgIdHeader, pageable);
	}

	// Supplier Contract Endpoints
	@LogActivity("Create Supplier Contract for Retailer")
	@PostMapping("/supplier-contracts/create")
	public ResponseEntity<?> createSupplierContract(@RequestBody Map<String, Object> contractDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.createSupplierContract(contractDto, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Contract for Retailer")
	@GetMapping("/supplier-contracts/{id}")
	public ResponseEntity<?> getSupplierContract(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierContract(id, authToken, orgIdHeader);
	}

	@LogActivity("Get Supplier Contract by Number for Retailer")
	@GetMapping("/supplier-contracts/number/{contractNumber}")
	public ResponseEntity<?> getSupplierContractByNumber(@PathVariable String contractNumber,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierContractByNumber(contractNumber, authToken, orgIdHeader);
	}

	@LogActivity("Get All Supplier Contracts for Retailer")
	@GetMapping("/supplier-contracts/all")
	public ResponseEntity<?> getAllSupplierContracts(
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@PageableDefault(size = 20) Pageable pageable,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long supplierId,
			@RequestParam(required = false) String contractType,
			@RequestParam(required = false) java.sql.Date effectiveStartDate,
			@RequestParam(required = false) java.sql.Date effectiveEndDate,
			@RequestParam(required = false) java.sql.Date expiryStartDate,
			@RequestParam(required = false) java.sql.Date expiryEndDate,
			@RequestParam(required = false) Boolean expiringOnly,
			@RequestParam(required = false) java.sql.Date expiringBeforeDate,
			@RequestParam(required = false) Boolean autoRenewalOnly,
			@RequestParam(required = false) java.sql.Date autoRenewalBeforeDate) {
		return coreRetailerService.getAllSupplierContracts(
				authToken, orgIdHeader, pageable,
				status, supplierId, contractType,
				effectiveStartDate, effectiveEndDate,
				expiryStartDate, expiryEndDate,
				expiringOnly, expiringBeforeDate,
				autoRenewalOnly, autoRenewalBeforeDate);
	}

	@LogActivity("Get Expiring Supplier Contracts for Retailer")
	@GetMapping("/supplier-contracts/expiring")
	public ResponseEntity<?> getExpiringSupplierContracts(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@RequestParam java.sql.Date beforeDate) {
		return coreRetailerService.getExpiringSupplierContracts(authToken, orgIdHeader, beforeDate);
	}

	@LogActivity("Get Auto Renewal Supplier Contracts for Retailer")
	@GetMapping("/supplier-contracts/auto-renewal")
	public ResponseEntity<?> getAutoRenewalSupplierContracts(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@RequestParam java.sql.Date beforeDate) {
		return coreRetailerService.getAutoRenewalSupplierContracts(authToken, orgIdHeader, beforeDate);
	}

	@LogActivity("Get Active Supplier Contract by Supplier and Date for Retailer")
	@GetMapping("/supplier-contracts/supplier/{supplierId}/active")
	public ResponseEntity<?> getActiveSupplierContractBySupplierAndDate(@PathVariable Long supplierId,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader,
			@RequestParam java.sql.Date date) {
		return coreRetailerService.getActiveSupplierContractBySupplierAndDate(supplierId, authToken, orgIdHeader, date);
	}

	@LogActivity("Get Supplier Contract Summary for Retailer")
	@GetMapping("/supplier-contracts/summary")
	public ResponseEntity<?> getSupplierContractSummary(@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierContractSummary(authToken, orgIdHeader);
	}

	@LogActivity("Update Supplier Contract for Retailer")
	@PutMapping("/supplier-contracts/{id}/update")
	public ResponseEntity<?> updateSupplierContract(@PathVariable Long id,
			@RequestBody Map<String, Object> contractDto,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateSupplierContract(id, contractDto, authToken, orgIdHeader);
	}

	@LogActivity("Update Supplier Contract Status for Retailer")
	@PutMapping("/supplier-contracts/{id}/status")
	public ResponseEntity<?> updateSupplierContractStatus(@PathVariable Long id,
			@RequestParam String status,
			@RequestParam(required = false) String reason,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.updateSupplierContractStatus(id, status, reason, authToken, orgIdHeader);
	}

	@LogActivity("Upload Supplier Contract Document for Retailer")
	@PostMapping("/supplier-contracts/{id}/documents")
	public ResponseEntity<?> uploadSupplierContractDocument(@PathVariable Long id,
			@RequestParam MultipartFile file,
			@RequestParam String documentName,
			@RequestParam(required = false) String remarks,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.uploadSupplierContractDocument(id, file, documentName, remarks, authToken,
				orgIdHeader);
	}

	@LogActivity("Get Supplier Contract Document for Retailer")
	@GetMapping("/supplier-contracts/{id}/documents")
	public ResponseEntity<?> getSupplierContractDocument(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.getSupplierContractDocument(id, authToken, orgIdHeader);
	}

	@LogActivity("Delete Supplier Contract Document for Retailer")
	@DeleteMapping("/supplier-contracts/{id}/documents")
	public ResponseEntity<?> deleteSupplierContractDocument(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.deleteSupplierContractDocument(id, authToken, orgIdHeader);
	}

	@LogActivity("Approve Supplier Contract for Retailer")
	@PostMapping("/supplier-contracts/{id}/approve")
	public ResponseEntity<?> approveSupplierContract(@PathVariable Long id,
			@RequestParam String approvedBy,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.approveSupplierContract(id, approvedBy, authToken, orgIdHeader);
	}

	@LogActivity("Reject Supplier Contract for Retailer")
	@PostMapping("/supplier-contracts/{id}/reject")
	public ResponseEntity<?> rejectSupplierContract(@PathVariable Long id,
			@RequestParam String rejectionReason,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.rejectSupplierContract(id, rejectionReason, authToken, orgIdHeader);
	}

	@LogActivity("Terminate Supplier Contract for Retailer")
	@PostMapping("/supplier-contracts/{id}/terminate")
	public ResponseEntity<?> terminateSupplierContract(@PathVariable Long id,
			@RequestParam String reason,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.terminateSupplierContract(id, reason, authToken, orgIdHeader);
	}

	@LogActivity("Suspend Supplier Contract for Retailer")
	@PostMapping("/supplier-contracts/{id}/suspend")
	public ResponseEntity<?> suspendSupplierContract(@PathVariable Long id,
			@RequestParam String reason,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.suspendSupplierContract(id, reason, authToken, orgIdHeader);
	}

	@LogActivity("Renew Supplier Contract for Retailer")
	@PostMapping("/supplier-contracts/{id}/renew")
	public ResponseEntity<?> renewSupplierContract(@PathVariable Long id,
			@RequestParam java.sql.Date newExpiryDate,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.renewSupplierContract(id, newExpiryDate, authToken, orgIdHeader);
	}

	@LogActivity("Delete Supplier Contract for Retailer")
	@DeleteMapping("/supplier-contracts/{id}/delete")
	public ResponseEntity<?> deleteSupplierContract(@PathVariable Long id,
			@RequestHeader("Authorization") String authToken,
			@RequestHeader("X-Organization-ID") String orgIdHeader) {
		return coreRetailerService.deleteSupplierContract(id, authToken, orgIdHeader);
	}
}