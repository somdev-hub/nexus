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
}