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
			@RequestParam(required = false) String status, @RequestParam(required = false) String poNumber) {
		return supplierService.getAllSupplierOrders(auth, org, p, status, poNumber);
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
			@RequestParam(required = false) String status) {
		return supplierService.getAllQuotations(auth, org, p, status);
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

	// Analytics
	@LogActivity("Get Supplier Dashboard via IAM")
	@GetMapping("/analytics/dashboard")
	public ResponseEntity<?> dashboard(@RequestHeader("Authorization") String auth,
			@RequestHeader("X-Organization-ID") String org) {
		return supplierService.getSupplierDashboard(auth, org);
	}
}
