package com.nexus.iam.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.service.CoreSupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/iam/core/supplier")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CoreSupplierController {

	private final CoreSupplierService supplierService;

	// Catalog
	@LogActivity("Create Supplier Catalog via IAM")
	@PostMapping("/catalog/create")
	public ResponseEntity<?> createCatalog(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createCatalog(dto, auth, org);
	}

	@LogActivity("Get Supplier Catalog via IAM")
	@GetMapping("/catalog/{id}")
	public ResponseEntity<?> getCatalog(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getCatalog(id, auth, org);
	}

	@LogActivity("Get All Supplier Catalogs via IAM")
	@GetMapping("/catalog/all")
	public ResponseEntity<?> getAllCatalogs(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String status, @RequestParam(required = false) String category,
			@RequestParam(required = false) String family, @RequestParam(required = false) String accessLevel,
			@RequestParam(required = false) Boolean isPublished, @RequestParam(required = false) String search) {
		return supplierService.getAllCatalogs(auth, org, p, status, category, family, accessLevel, isPublished, search);
	}

	@LogActivity("Update Supplier Catalog via IAM")
	@PutMapping("/catalog/{id}/update")
	public ResponseEntity<?> updateCatalog(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updateCatalog(id, dto, auth, org);
	}

	@LogActivity("Transition Catalog Status via IAM")
	@PutMapping("/catalog/{id}/status")
	public ResponseEntity<?> transitionCatalog(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.transitionCatalogStatus(id, newStatus, params, auth, org);
	}

	@LogActivity("Delete Catalog via IAM")
	@DeleteMapping("/catalog/{id}")
	public ResponseEntity<?> deleteCatalog(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteCatalog(id, auth, org);
	}

	@LogActivity("Get Catalog Summary via IAM")
	@GetMapping("/catalog/summary")
	public ResponseEntity<?> catalogSummary(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getCatalogSummary(auth, org);
	}

	// Variant
	@LogActivity("Create Variant via IAM")
	@PostMapping("/variants/create")
	public ResponseEntity<?> createVariant(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createVariant(dto, auth, org);
	}

	@LogActivity("Get Variant via IAM")
	@GetMapping("/variants/{id}")
	public ResponseEntity<?> getVariant(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getVariant(id, auth, org);
	}

	@LogActivity("Get All Variants via IAM")
	@GetMapping("/variants/all")
	public ResponseEntity<?> getAllVariants(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long catalogId, @RequestParam(required = false) String variantType,
			@RequestParam(required = false) Long bomMaterialId) {
		return supplierService.getAllVariants(auth, org, p, catalogId, variantType, bomMaterialId);
	}

	@LogActivity("Delete Variant via IAM")
	@DeleteMapping("/variants/{id}")
	public ResponseEntity<?> deleteVariant(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteVariant(id, auth, org);
	}

	// Price Tier
	@LogActivity("Create Price Tier via IAM")
	@PostMapping("/price-tiers/create")
	public ResponseEntity<?> createPriceTier(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createPriceTier(dto, auth, org);
	}

	@LogActivity("Get Price Tier via IAM")
	@GetMapping("/price-tiers/{id}")
	public ResponseEntity<?> getPriceTier(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getPriceTier(id, auth, org);
	}

	@LogActivity("Get All Price Tiers via IAM")
	@GetMapping("/price-tiers/all")
	public ResponseEntity<?> getAllPriceTiers(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long catalogId, @RequestParam(required = false) String customerSegment,
			@RequestParam(required = false) Long contractId) {
		return supplierService.getAllPriceTiers(auth, org, p, catalogId, customerSegment, contractId);
	}

	@LogActivity("Delete Price Tier via IAM")
	@DeleteMapping("/price-tiers/{id}")
	public ResponseEntity<?> deletePriceTier(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deletePriceTier(id, auth, org);
	}

	@LogActivity("Update Price Tier via IAM")
	@PutMapping("/price-tiers/{id}/update")
	public ResponseEntity<?> updatePriceTier(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updatePriceTier(id, dto, auth, org);
	}

	@LogActivity("Get Price For Quantity via IAM")
	@GetMapping("/price-tiers/price-for-quantity")
	public ResponseEntity<?> getPriceForQty(@RequestParam Long catalogId, @RequestParam Double quantity,
			@RequestParam(required = false) String customerSegment, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getPriceForQuantity(catalogId, quantity, customerSegment, auth, org);
	}

	// Digital Asset
	@LogActivity("Create Digital Asset via IAM")
	@PostMapping("/digital-assets/create")
	public ResponseEntity<?> createAsset(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createDigitalAsset(dto, auth, org);
	}

	@LogActivity("Get Digital Asset via IAM")
	@GetMapping("/digital-assets/{id}")
	public ResponseEntity<?> getAsset(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getDigitalAsset(id, auth, org);
	}

	@LogActivity("Get All Digital Assets via IAM")
	@GetMapping("/digital-assets/all")
	public ResponseEntity<?> getAllAssets(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long catalogId, @RequestParam(required = false) String assetType) {
		return supplierService.getAllDigitalAssets(auth, org, p, catalogId, assetType);
	}

	@LogActivity("Delete Digital Asset via IAM")
	@DeleteMapping("/digital-assets/{id}")
	public ResponseEntity<?> deleteAsset(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteDigitalAsset(id, auth, org);
	}

	@LogActivity("Upload Digital Asset File via IAM")
	@PostMapping("/digital-assets/{id}/file")
	public ResponseEntity<?> uploadAssetFile(@PathVariable Long id,
			@RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error",
					"File is required to add a digital asset. Only JPG, PNG and PDF files are allowed."));
		}
		return supplierService.uploadDigitalAssetFile(id, file, auth, org);
	}

	// Capacity
	@LogActivity("Create Capacity via IAM")
	@PostMapping("/capacity/create")
	public ResponseEntity<?> createCap(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createCapacity(dto, auth, org);
	}

	@LogActivity("Get Capacity via IAM")
	@GetMapping("/capacity/{id}")
	public ResponseEntity<?> getCap(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getCapacity(id, auth, org);
	}

	@LogActivity("Get All Capacities via IAM")
	@GetMapping("/capacity/all")
	public ResponseEntity<?> getAllCaps(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String productLine, @RequestParam(required = false) String shift) {
		return supplierService.getAllCapacities(auth, org, p, productLine, shift);
	}

	@LogActivity("Get Capacity Summary via IAM")
	@GetMapping("/capacity/summary")
	public ResponseEntity<?> capSummary(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getCapacitySummary(auth, org);
	}

	@LogActivity("Update Capacity via IAM")
	@PutMapping("/capacity/{id}/update")
	public ResponseEntity<?> updateCap(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updateCapacity(id, dto, auth, org);
	}

	@LogActivity("Delete Capacity via IAM")
	@DeleteMapping("/capacity/{id}")
	public ResponseEntity<?> deleteCap(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteCapacity(id, auth, org);
	}

	// ATP
	@LogActivity("Get ATP via IAM")
	@GetMapping("/atp/catalog/{catalogId}")
	public ResponseEntity<?> getAtp(@PathVariable Long catalogId,
			@RequestParam(required = false) Double requestedQuantity, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getAtpForCatalog(catalogId, requestedQuantity, auth, org);
	}

	@LogActivity("Get ATP Product Line via IAM")
	@GetMapping("/atp/product-line")
	public ResponseEntity<?> getAtpPL(@RequestParam(required = false) String productLine,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getAtpForProductLine(productLine, auth, org);
	}

	// Orders
	@LogActivity("Get Supplier Order via IAM")
	@GetMapping("/orders/{id}")
	public ResponseEntity<?> getOrder(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getSupplierOrder(id, auth, org);
	}

	@LogActivity("Get All Supplier Orders via IAM")
	@GetMapping("/orders/all")
	public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String status, @RequestParam(required = false) String poNumber,
			@RequestParam(required = false) Long buyerOrgId) {
		return supplierService.getAllSupplierOrders(auth, org, p, status, poNumber, buyerOrgId);
	}

	@LogActivity("Acknowledge Order via IAM")
	@PutMapping("/orders/{id}/acknowledge")
	public ResponseEntity<?> ackOrder(@PathVariable Long id,
			@RequestParam(required = false) String confirmedDeliveryDate, @RequestParam(required = false) String notes,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.acknowledgeOrder(id, auth, org, confirmedDeliveryDate, notes);
	}

	@LogActivity("Update Order Fulfillment via IAM")
	@PutMapping("/orders/{id}/fulfillment")
	public ResponseEntity<?> updateFulfill(@PathVariable Long id, @RequestBody Map<String, Object> updates,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updateOrderFulfillment(id, updates, auth, org);
	}

	@LogActivity("Create Partial Shipment via IAM")
	@PostMapping("/orders/{purchaseOrderId}/partial-shipment")
	public ResponseEntity<?> createPartial(@PathVariable Long purchaseOrderId, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createPartialShipment(purchaseOrderId, dto, auth, org);
	}

	@LogActivity("Get Order Summary via IAM")
	@GetMapping("/orders/summary")
	public ResponseEntity<?> orderSummary(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getOrderSummary(auth, org);
	}

	// Quality
	@LogActivity("Create Quality Cert via IAM")
	@PostMapping("/quality-certificates/create")
	public ResponseEntity<?> createQC(@RequestBody Map<String, Object> dto, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.createQualityCert(dto, auth, org);
	}

	@LogActivity("Get Quality Cert via IAM")
	@GetMapping("/quality-certificates/{id}")
	public ResponseEntity<?> getQC(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getQualityCert(id, auth, org);
	}

	@LogActivity("Get All Quality Certs via IAM")
	@GetMapping("/quality-certificates/all")
	public ResponseEntity<?> getAllQC(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) Long purchaseOrderId, @RequestParam(required = false) Long catalogId,
			@RequestParam(required = false) String certificateType) {
		return supplierService.getAllQualityCerts(auth, org, p, purchaseOrderId, catalogId, certificateType);
	}

	@LogActivity("Delete Quality Cert via IAM")
	@DeleteMapping("/quality-certificates/{id}")
	public ResponseEntity<?> delQC(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteQualityCert(id, auth, org);
	}

	// Quotation
	@LogActivity("Create Quotation via IAM")
	@PostMapping("/quotations/create")
	public ResponseEntity<?> createQ(@RequestBody Map<String, Object> dto, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.createQuotation(dto, auth, org);
	}

	@LogActivity("Get Quotation via IAM")
	@GetMapping("/quotations/{id}")
	public ResponseEntity<?> getQ(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getQuotation(id, auth, org);
	}

	@LogActivity("Get All Quotations via IAM")
	@GetMapping("/quotations/all")
	public ResponseEntity<?> getAllQ(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) Long buyerOrgId,
			@RequestParam(required = false) String quotationNumber) {
		return supplierService.getAllQuotations(auth, org, p, status, buyerOrgId, quotationNumber);
	}

	@LogActivity("Update Quotation via IAM")
	@PutMapping("/quotations/{id}/update")
	public ResponseEntity<?> updateQ(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updateQuotation(id, dto, auth, org);
	}

	@LogActivity("Transition Quotation via IAM")
	@PutMapping("/quotations/{id}/status")
	public ResponseEntity<?> transQ(@PathVariable Long id, @RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.transitionQuotation(id, newStatus, params, auth, org);
	}

	@LogActivity("Convert Quotation via IAM")
	@PostMapping("/quotations/{id}/convert-to-order")
	public ResponseEntity<?> convQ(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.convertQuotation(id, auth, org);
	}

	@LogActivity("Delete Quotation via IAM")
	@DeleteMapping("/quotations/{id}")
	public ResponseEntity<?> deleteQ(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteQuotation(id, auth, org);
	}

	@LogActivity("Get Quotation Summary via IAM")
	@GetMapping("/quotations/summary")
	public ResponseEntity<?> qSummary(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getQuotationSummary(auth, org);
	}

	// Forecast
	@LogActivity("Create Forecast via IAM")
	@PostMapping("/forecasts/create")
	public ResponseEntity<?> createF(@RequestBody Map<String, Object> dto, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.createForecast(dto, auth, org);
	}

	@LogActivity("Get Forecast via IAM")
	@GetMapping("/forecasts/{id}")
	public ResponseEntity<?> getF(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getForecast(id, auth, org);
	}

	@LogActivity("Get All Forecasts via IAM")
	@GetMapping("/forecasts/all")
	public ResponseEntity<?> getAllF(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getAllForecasts(auth, org, p);
	}

	@LogActivity("Delete Forecast via IAM")
	@DeleteMapping("/forecasts/{id}")
	public ResponseEntity<?> delF(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteForecast(id, auth, org);
	}

	@LogActivity("Update Forecast via IAM")
	@PutMapping("/forecasts/{id}/update")
	public ResponseEntity<?> updateF(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updateForecast(id, dto, auth, org);
	}

	@LogActivity("Transition Forecast via IAM")
	@PutMapping("/forecasts/{id}/status")
	public ResponseEntity<?> transitionF(@PathVariable Long id, @RequestParam String newStatus,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.transitionForecast(id, newStatus, auth, org);
	}

	// Customer Portal
	@LogActivity("Get Customer Orders via IAM")
	@GetMapping("/customer-portal/orders")
	public ResponseEntity<?> custOrders(@RequestParam(required = false) Long buyerOrgId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org,
			@PageableDefault(size = 20) Pageable p) {
		return supplierService.getCustomerOrders(buyerOrgId, auth, org, p);
	}

	@LogActivity("Get Customer Summary via IAM")
	@GetMapping("/customer-portal/summary")
	public ResponseEntity<?> custSummary(@RequestParam(required = false) Long buyerOrgId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getCustomerSummary(buyerOrgId, auth, org);
	}

	// Account Health
	@LogActivity("Get Account Health via IAM")
	@GetMapping("/account-health/{buyerOrgId}")
	public ResponseEntity<?> accHealth(@PathVariable Long buyerOrgId, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getAccountHealth(buyerOrgId, auth, org);
	}

	@LogActivity("Get All Account Health via IAM")
	@GetMapping("/account-health/all")
	public ResponseEntity<?> allHealth(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getAllAccountHealth(auth, org);
	}

	@LogActivity("Get Account Health Summary via IAM")
	@GetMapping("/account-health/summary")
	public ResponseEntity<?> healthSummary(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getAccountHealthSummary(auth, org);
	}

	// Consignment
	@LogActivity("Create Consignment via IAM")
	@PostMapping("/consignment/create")
	public ResponseEntity<?> createCons(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createConsignment(dto, auth, org);
	}

	@LogActivity("Get Consignment via IAM")
	@GetMapping("/consignment/{id}")
	public ResponseEntity<?> getCons(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getConsignment(id, auth, org);
	}

	@LogActivity("Get All Consignments via IAM")
	@GetMapping("/consignment/all")
	public ResponseEntity<?> getAllCons(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getAllConsignments(auth, org, p);
	}

	@LogActivity("Get Consignment Summary via IAM")
	@GetMapping("/consignment/summary")
	public ResponseEntity<?> consSummary(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getConsignmentSummary(auth, org);
	}

	@LogActivity("Adjust Consignment via IAM")
	@PostMapping("/consignment/{id}/adjust")
	public ResponseEntity<?> adjustCons(@PathVariable Long id, @RequestParam Double quantity,
			@RequestParam(required = false) String reason, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.adjustConsignment(id, quantity, reason, auth, org);
	}

	@LogActivity("Delete Consignment via IAM")
	@DeleteMapping("/consignment/{id}")
	public ResponseEntity<?> deleteCons(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteConsignment(id, auth, org);
	}

	// VMI
	@LogActivity("Create VMI via IAM")
	@PostMapping("/vmi/create")
	public ResponseEntity<?> createVmi(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createVmi(dto, auth, org);
	}

	@LogActivity("Get VMI via IAM")
	@GetMapping("/vmi/{id}")
	public ResponseEntity<?> getVmi(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getVmi(id, auth, org);
	}

	@LogActivity("Get All VMI via IAM")
	@GetMapping("/vmi/all")
	public ResponseEntity<?> getAllVmi(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getAllVmi(auth, org, p);
	}

	@LogActivity("Get VMI Suggestions via IAM")
	@GetMapping("/vmi/replenishment-suggestions")
	public ResponseEntity<?> vmiSug(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getVmiSuggestions(auth, org);
	}

	@LogActivity("Update VMI via IAM")
	@PutMapping("/vmi/{id}/update")
	public ResponseEntity<?> updateVmi(@PathVariable Long id, @RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updateVmi(id, dto, auth, org);
	}

	@LogActivity("Delete VMI via IAM")
	@DeleteMapping("/vmi/{id}")
	public ResponseEntity<?> deleteVmi(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.deleteVmi(id, auth, org);
	}

	@LogActivity("Trigger VMI Replenishment via IAM")
	@PostMapping("/vmi/{id}/replenish")
	public ResponseEntity<?> replenishVmi(@PathVariable Long id, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.triggerVmiReplenish(id, auth, org);
	}

	// Analytics
	@LogActivity("Get Supplier Dashboard via IAM")
	@GetMapping("/analytics/dashboard")
	public ResponseEntity<?> dashboard(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getSupplierDashboard(auth, org);
	}

	// Partnership Invitations (mirrors retailer invitation routes, same core URLs)
	@LogActivity("Create Partnership Invitation for Supplier")
	@PostMapping("/partnership-invitations/create")
	public ResponseEntity<?> createPartnershipInvitation(@RequestBody Map<String, Object> invitationDto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createPartnershipInvitation(invitationDto, auth, org);
	}

	@LogActivity("Respond to Partnership Invitation for Supplier")
	@PutMapping("/partnership-invitations/{id}/respond")
	public ResponseEntity<?> respondToPartnershipInvitation(@PathVariable Long id,
			@RequestBody Map<String, Object> responseDto, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.respondToPartnershipInvitation(id, responseDto, auth, org);
	}

	@LogActivity("Get Partnership Invitation for Supplier")
	@GetMapping("/partnership-invitations/{id}")
	public ResponseEntity<?> getPartnershipInvitation(@PathVariable Long id,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getPartnershipInvitation(id, auth, org);
	}

	@LogActivity("Get Sent Partnership Invitations for Supplier")
	@GetMapping("/partnership-invitations/sent")
	public ResponseEntity<?> getSentPartnershipInvitations(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getSentPartnershipInvitations(auth, org, p);
	}

	@LogActivity("Get Received Partnership Invitations for Supplier")
	@GetMapping("/partnership-invitations/received")
	public ResponseEntity<?> getReceivedPartnershipInvitations(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getReceivedPartnershipInvitations(auth, org, p);
	}

	@LogActivity("Get Pending Partnership Invitations for Supplier")
	@GetMapping("/partnership-invitations/pending")
	public ResponseEntity<?> getPendingPartnershipInvitations(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getPendingPartnershipInvitations(auth, org, p);
	}

	@LogActivity("Withdraw Partnership Invitation for Supplier")
	@PutMapping("/partnership-invitations/{id}/withdraw")
	public ResponseEntity<?> withdrawPartnershipInvitation(@PathVariable Long id,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.withdrawPartnershipInvitation(id, auth, org);
	}

	// ASN / POD passthroughs (Core P1)
	@LogActivity("Get ASN by Purchase Order for Supplier")
	@GetMapping("/asn/purchase-order/{poId}")
	public ResponseEntity<?> getAsnByPurchaseOrder(@PathVariable Long poId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getAsnByPurchaseOrder(poId, auth, org);
	}

	@LogActivity("Get Shipment POD for Supplier")
	@GetMapping("/shipments/{shipmentId}/pod")
	public ResponseEntity<?> getShipmentPod(@PathVariable Long shipmentId,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getShipmentPod(shipmentId, auth, org);
	}

	@LogActivity("Get All Partnerships for Supplier")
	@GetMapping("/partnerships/all")
	public ResponseEntity<?> getAllPartnerships(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org,
			@PageableDefault(size = 20) Pageable pageable) {
		return supplierService.getAllPartnerships(auth, org, pageable);
	}

	@LogActivity("Update Partnership for Supplier")
	@PutMapping("/partnerships/{id}/update")
	public ResponseEntity<?> updatePartnership(@PathVariable Long id,
			@RequestBody Map<String, Object> partnershipDto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.updatePartnership(id, partnershipDto, auth, org);
	}

	@LogActivity("Update Partnership Status for Supplier")
	@PostMapping("/partnerships/{id}/status")
	public ResponseEntity<?> updatePartnershipStatus(@PathVariable Long id,
			@RequestBody Map<String, Object> statusDto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		String status = statusDto != null ? (String) statusDto.get("status") : null;
		return supplierService.updatePartnershipStatus(id, status, auth, org);
	}

	// Supplier contracts shared with this supplier org (countersign flow)
	@LogActivity("Get Supplier Contracts for Supplier")
	@GetMapping("/contracts/all")
	public ResponseEntity<?> getSupplierContracts(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org,
			@PageableDefault(size = 20) Pageable pageable) {
		return supplierService.getSupplierContracts(auth, org, pageable);
	}

	@LogActivity("Get Supplier Contract for Supplier")
	@GetMapping("/contracts/{id}")
	public ResponseEntity<?> getSupplierContract(@PathVariable Long id,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getSupplierContract(id, auth, org);
	}

	@LogActivity("Approve Supplier Contract for Supplier")
	@PostMapping("/contracts/{id}/approve")
	public ResponseEntity<?> approveSupplierContract(@PathVariable Long id,
			@RequestParam(required = false) String decidedBy,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.approveSupplierContract(id, decidedBy, auth, org);
	}

	@LogActivity("Reject Supplier Contract for Supplier")
	@PostMapping("/contracts/{id}/reject")
	public ResponseEntity<?> rejectSupplierContract(@PathVariable Long id,
			@RequestParam String comments,
			@RequestParam(required = false) String decidedBy,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.rejectSupplierContract(id, comments, decidedBy, auth, org);
	}

	@LogActivity("Request Supplier Contract Amendments for Supplier")
	@PostMapping("/contracts/{id}/request-amendments")
	public ResponseEntity<?> requestSupplierContractAmendments(@PathVariable Long id,
			@RequestParam String comments,
			@RequestParam(required = false) String decidedBy,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.requestSupplierContractAmendments(id, comments, decidedBy, auth, org);
	}

	@LogActivity("Get Supplier Contract Document Content for Supplier")
	@GetMapping("/contracts/{id}/document-content")
	public ResponseEntity<?> getSupplierContractDocumentContent(@PathVariable Long id,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.getSupplierContractDocumentContent(id, auth, org);
	}

	// Supplier-owned delivery: logistics marketplace + shipment handover
	@LogActivity("Browse Logistics Marketplace for Supplier")
	@GetMapping("/logistics-marketplace/available")
	public ResponseEntity<?> getAvailableLogistics(@RequestParam(required = false) String search,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org,
			@PageableDefault(size = 20) Pageable p) {
		return supplierService.getAvailableLogistics(search, auth, org, p);
	}

	@LogActivity("Get Supplier Logistics Partnerships")
	@GetMapping("/logistics-marketplace/partnerships")
	public ResponseEntity<?> getSupplierLogisticsPartnerships(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org, @PageableDefault(size = 20) Pageable p) {
		return supplierService.getSupplierLogisticsPartnerships(auth, org, p);
	}

	@LogActivity("Propose Supplier Logistics Partnership")
	@PostMapping("/logistics-marketplace/proposals")
	public ResponseEntity<?> createLogisticsProposal(@RequestBody Map<String, Object> dto,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org) {
		return supplierService.createLogisticsProposal(dto, auth, org);
	}

	@LogActivity("Get Supplier Shipments")
	@GetMapping("/logistics-marketplace/shipments")
	public ResponseEntity<?> getSupplierShipments(@RequestParam(required = false) String status,
			@RequestHeader("Authorization") String auth, @RequestHeader("X-Organization-ID") String org,
			@PageableDefault(size = 20) Pageable p) {
		return supplierService.getSupplierShipments(status, auth, org, p);
	}

	@LogActivity("Handover Supplier Shipment to Logistics")
	@PostMapping("/logistics-marketplace/shipments/{shipmentId}/handover")
	public ResponseEntity<?> handoverShipment(@PathVariable Long shipmentId,
			@RequestBody Map<String, Object> dto, @RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.handoverShipment(shipmentId, dto, auth, org);
	}
}
