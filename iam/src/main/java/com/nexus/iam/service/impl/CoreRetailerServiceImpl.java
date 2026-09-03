package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.service.CoreRetailerService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core Retailer Service Implementation
 * <p>
 * Handles retailer-specific Core module operations through IAM gateway.
 * All HTTP calls to Core module are handled here using RestService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoreRetailerServiceImpl implements CoreRetailerService {

	private final RestService restService;
	private final WebConstants webConstants;
	private final CommonUtils commonUtils;

	@Override
	public ResponseEntity<?> addProduct(Map<String, Object> productDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreProductAddUrl(),
				productDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getProduct(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreProductGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllProducts(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreProductAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> addMaterial(Map<String, Object> materialDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreMaterialAddUrl(),
				materialDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getMaterial(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreMaterialGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllMaterials(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreMaterialAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> addWarehouse(Map<String, Object> warehouseDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreWarehouseAddUrl(),
				warehouseDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getWarehouse(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreWarehouseGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllWarehouses(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreWarehouseAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> addOrder(Map<String, Object> orderDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreOrderAddUrl(),
				orderDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getOrder(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreOrderGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllOrders(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreOrderAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> addPartnership(Map<String, Object> partnershipDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCorePartnershipAddUrl(),
				partnershipDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getPartnership(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllPartnerships(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePartnershipAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getPartnershipsByStatus(String status, String authToken, String orgIdHeader,
			Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePartnershipStatusUrl() + "/" + status, pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getActivePartnerships(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePartnershipActiveUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updatePartnershipStatus(Long id, String status, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipUpdateStatusUrl() + "/" + id + "/status";
		Map<String, Object> body = Map.of("status", status);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	// Partnership Agreement with DMS Integration
	@Override
	public ResponseEntity<?> uploadPartnershipAgreement(Long id, MultipartFile file, String documentName,
			String remarks, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildMultipartHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipAgreementUploadUrl() + "/" + id + "/agreement";

		org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
		try {
			body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
				@Override
				public String getFilename() {
					return file.getOriginalFilename();
				}
			});
		} catch (java.io.IOException e) {
			log.error("Failed to read file bytes: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(Map.of("error", "Failed to read file"));
		}
		if (documentName != null) {
			body.add("documentName", documentName);
		}
		if (remarks != null) {
			body.add("remarks", remarks);
		}

		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getPartnershipAgreement(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipAgreementGetUrl() + "/" + id + "/agreement";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> deletePartnershipAgreement(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipAgreementDeleteUrl() + "/" + id + "/agreement";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.DELETE,
				null);
	}

	// Partnership Lifecycle Management
	@Override
	public ResponseEntity<?> transitionPartnershipStatus(Long id, Map<String, Object> transitionDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipTransitionUrl() + "/" + id + "/transition";
		return restService.iamRestCall(
				url,
				transitionDto,
				headers,
				HttpMethod.POST,
				null);
	}

	// Partnership Invitation Endpoints
	@Override
	public ResponseEntity<?> createPartnershipInvitation(Map<String, Object> invitationDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCorePartnershipInvitationCreateUrl(),
				invitationDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> respondToPartnershipInvitation(Long id, Map<String, Object> responseDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipInvitationRespondUrl() + "/" + id + "/respond";
		return restService.iamRestCall(
				url,
				responseDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> getPartnershipInvitation(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipInvitationGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSentPartnershipInvitations(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePartnershipInvitationSentUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getReceivedPartnershipInvitations(String authToken, String orgIdHeader,
			Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePartnershipInvitationReceivedUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getPendingPartnershipInvitations(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePartnershipInvitationPendingUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> withdrawPartnershipInvitation(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePartnershipInvitationWithdrawUrl() + "/" + id + "/withdraw";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.PUT,
				null);
	}

	// Supplier Discovery Endpoints
	@Override
	public ResponseEntity<?> discoverSuppliers(Map<String, Object> filterDto, String authToken, String orgIdHeader,
			Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierDiscoverUrl(), pageable);
		return restService.iamRestCall(
				url,
				filterDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> qualifySupplier(Map<String, Object> qualificationDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierQualifyUrl(),
				qualificationDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierQualification(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierQualificationGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllSupplierQualifications(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierQualificationAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updateSupplierQualificationStatus(Long id, String status, String rejectionReason,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierQualificationUpdateStatusUrl() + "/" + id + "/status";
		Map<String, Object> body = Map.of(
				"status", status,
				"rejectionReason", rejectionReason);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.PUT,
				null);
	}

	// Supplier Management Endpoints
	@Override
	public ResponseEntity<?> addSupplier(Map<String, Object> supplierDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierAddUrl(),
				supplierDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplier(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllSuppliers(String authToken, String orgIdHeader, Pageable pageable,
			Long accountId, String category, String location, Double minRating, String certification) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierAllUrl(), pageable,
				"accountId", accountId,
				"category", category,
				"location", location,
				"minRating", minRating,
				"certification", certification);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	// Purchase Order Endpoints
	@Override
	public ResponseEntity<?> createPurchaseOrder(Map<String, Object> poDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCorePurchaseOrderCreateUrl(),
				poDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getPurchaseOrder(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePurchaseOrderGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllPurchaseOrders(String authToken, String orgIdHeader, Pageable pageable,
			String status) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCorePurchaseOrderAllUrl(), pageable,
				"status", status);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updatePurchaseOrder(Long id, Map<String, Object> poDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePurchaseOrderUpdateUrl() + "/" + id + "/update";
		return restService.iamRestCall(
				url,
				poDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> transitionPurchaseOrderStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePurchaseOrderTransitionUrl() + "/" + id + "/transition?newStatus=" + newStatus;
		return restService.iamRestCall(
				url,
				params,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> createPurchaseOrderAmendment(Long parentPoId, Map<String, Object> amendmentDto,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePurchaseOrderAmendUrl() + "/" + parentPoId + "/amend";
		return restService.iamRestCall(
				url,
				amendmentDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getPurchaseOrderAmendments(Long parentPoId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCorePurchaseOrderAmendmentsUrl() + "/" + parentPoId + "/amendments";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	// Stock/Inventory Endpoints
	@Override
	public ResponseEntity<?> addStock(Map<String, Object> stockDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreStockAddUrl(),
				stockDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getStock(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllStocks(String authToken, String orgIdHeader, Pageable pageable,
			Long warehouseId, Long materialId, Boolean belowReorderPoint, Boolean atOrBelowMinLevel) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreStockAllUrl(), pageable,
				"warehouseId", warehouseId,
				"materialId", materialId,
				"belowReorderPoint", belowReorderPoint,
				"atOrBelowMinLevel", atOrBelowMinLevel);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getInventoryValuation(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreStockValuationUrl(),
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getWarehouseInventoryValuation(Long warehouseId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockWarehouseValuationUrl() + "/" + warehouseId;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> adjustStock(Long stockId, Double quantity, String reason, String referenceType,
			Long referenceId,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockAdjustUrl() + "/" + stockId + "/adjust";
		Map<String, Object> body = Map.of(
				"quantity", quantity,
				"reason", reason,
				"referenceType", referenceType,
				"referenceId", referenceId);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> reserveStock(Long stockId, Double quantity, String referenceType, Long referenceId,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockReserveUrl() + "/" + stockId + "/reserve";
		Map<String, Object> body = Map.of(
				"quantity", quantity,
				"referenceType", referenceType,
				"referenceId", referenceId);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> releaseReservation(Long stockId, Double quantity, String referenceType, Long referenceId,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockReleaseReservationUrl() + "/" + stockId + "/release-reservation";
		Map<String, Object> body = Map.of(
				"quantity", quantity,
				"referenceType", referenceType,
				"referenceId", referenceId);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> transferStock(Long fromStockId, Long toWarehouseId, Double quantity, String reason,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockTransferUrl() + "/" + fromStockId + "/transfer";
		Map<String, Object> body = Map.of(
				"toWarehouseId", toWarehouseId,
				"quantity", quantity,
				"reason", reason);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> recordCycleCount(Long stockId, Double countedQuantity, String countedBy,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockCycleCountUrl() + "/" + stockId + "/cycle-count";
		Map<String, Object> body = Map.of(
				"countedQuantity", countedQuantity,
				"countedBy", countedBy);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getReorderSuggestions(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreStockReorderSuggestionsUrl(),
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updateStockSettings(Long stockId, Double reorderPoint, Double reorderQuantity,
			Double minStockLevel, Double maxStockLevel, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockSettingsUrl() + "/" + stockId + "/settings";
		Map<String, Object> body = new java.util.HashMap<>();
		if (reorderPoint != null)
			body.put("reorderPoint", reorderPoint);
		if (reorderQuantity != null)
			body.put("reorderQuantity", reorderQuantity);
		if (minStockLevel != null)
			body.put("minStockLevel", minStockLevel);
		if (maxStockLevel != null)
			body.put("maxStockLevel", maxStockLevel);
		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.PUT,
				null);
	}

	// Stock Movement Endpoints
	@Override
	public ResponseEntity<?> addStockMovement(Map<String, Object> movementDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreStockMovementAddUrl(),
				movementDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getStockMovement(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockMovementGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllStockMovements(String authToken, String orgIdHeader, Pageable pageable,
			Long stockId, Long warehouseId, String type, String referenceType, Long referenceId,
			String batchNumber, java.sql.Timestamp beforeDate, java.sql.Timestamp start,
			java.sql.Timestamp end, Long materialId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreStockMovementAllUrl(), pageable,
				"stockId", stockId,
				"warehouseId", warehouseId,
				"type", type,
				"referenceType", referenceType,
				"referenceId", referenceId,
				"batchNumber", batchNumber,
				"beforeDate", beforeDate,
				"start", start,
				"end", end,
				"materialId", materialId);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getMovementSummary(java.sql.Timestamp start, java.sql.Timestamp end, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreStockMovementSummaryUrl() + "?start=" + start + "&end=" + end;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	// Goods Receipt Endpoints
	@Override
	public ResponseEntity<?> createGoodsReceipt(Map<String, Object> grDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreGoodsReceiptCreateUrl(),
				grDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getGoodsReceipt(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreGoodsReceiptGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllGoodsReceipts(String authToken, String orgIdHeader, Pageable pageable,
			String status, Long purchaseOrderId, Long supplierId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreGoodsReceiptAllUrl(), pageable,
				"status", status,
				"purchaseOrderId", purchaseOrderId,
				"supplierId", supplierId);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updateGoodsReceipt(Long id, Map<String, Object> grDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreGoodsReceiptUpdateUrl() + "/" + id + "/update";
		return restService.iamRestCall(
				url,
				grDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> transitionGoodsReceiptStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreGoodsReceiptTransitionUrl() + "/" + id + "/transition?newStatus=" + newStatus;
		return restService.iamRestCall(
				url,
				params,
				headers,
				HttpMethod.PUT,
				null);
	}

	// Invoice Endpoints
	@Override
	public ResponseEntity<?> createInvoice(Map<String, Object> invoiceDto, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreInvoiceCreateUrl(),
				invoiceDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getInvoice(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreInvoiceGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllInvoices(String authToken, String orgIdHeader, Pageable pageable,
			String status, Long purchaseOrderId, Long supplierId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreInvoiceAllUrl(), pageable,
				"status", status,
				"purchaseOrderId", purchaseOrderId,
				"supplierId", supplierId);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updateInvoice(Long id, Map<String, Object> invoiceDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreInvoiceUpdateUrl() + "/" + id + "/update";
		return restService.iamRestCall(
				url,
				invoiceDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> transitionInvoiceStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreInvoiceTransitionUrl() + "/" + id + "/transition?newStatus=" + newStatus;
		return restService.iamRestCall(
				url,
				params,
				headers,
				HttpMethod.PUT,
				null);
	}

	// Three-Way Matching Endpoints
	@Override
	public ResponseEntity<?> performThreeWayMatch(Long purchaseOrderId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreThreeWayMatchMatchUrl() + "/match/" + purchaseOrderId;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> canInvoice(Long purchaseOrderId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreThreeWayMatchCanInvoiceUrl() + "/can-invoice/" + purchaseOrderId;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getMatchingSummary(Long purchaseOrderId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreThreeWayMatchSummaryUrl() + "/summary/" + purchaseOrderId;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> validateInvoiceMatch(Map<String, Object> invoiceDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreThreeWayMatchValidateInvoiceUrl(),
				invoiceDto,
				headers,
				HttpMethod.POST,
				null);
	}

	// Supplier Performance Endpoints
	@Override
	public ResponseEntity<?> createSupplierPerformance(Map<String, Object> performanceDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierPerformanceCreateUrl(),
				performanceDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformance(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierPerformanceGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformanceBySupplier(Long supplierId, String authToken, String orgIdHeader,
			Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierPerformanceBySupplierUrl() + "/" + supplierId,
				pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllSupplierPerformance(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierPerformanceAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformanceByPeriod(String authToken, String orgIdHeader, Pageable pageable,
			java.sql.Date startDate, java.sql.Date endDate) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierPerformanceByPeriodUrl(), pageable,
				"startDate", startDate,
				"endDate", endDate);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformanceBySupplierAndPeriod(Long supplierId, String authToken,
			String orgIdHeader, Pageable pageable,
			java.sql.Date startDate, java.sql.Date endDate) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(
				webConstants.getCoreSupplierPerformanceBySupplierAndPeriodUrl() + "/" + supplierId, pageable,
				"startDate", startDate,
				"endDate", endDate);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformanceByTier(String authToken, String orgIdHeader, Pageable pageable,
			String tier) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierPerformanceByTierUrl() + "/" + tier, pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	// Supplier Risk Monitoring Endpoints (FR-RET-024)
	@Override
	public ResponseEntity<?> createSupplierRiskMonitoring(Map<String, Object> riskDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierRiskMonitoringCreateUrl(),
				riskDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskMonitoring(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskMonitoringBySupplier(Long supplierId, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringBySupplierUrl() + "/" + supplierId;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskMonitoringByPartnership(Long partnershipId, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringByPartnershipUrl() + "/" + partnershipId;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskMonitoringByRiskLevel(String riskLevel, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringByRiskLevelUrl() + "/" + riskLevel;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskMonitoringDueForReview(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierRiskMonitoringDueForReviewUrl(),
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updateSupplierRiskMonitoring(Long id, Map<String, Object> riskDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringUpdateUrl() + "/" + id + "/update";
		return restService.iamRestCall(
				url,
				riskDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> deleteSupplierRiskMonitoring(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringDeleteUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.DELETE,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskSummary(Long supplierId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringSummaryUrl() + "/" + supplierId + "/summary";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierRiskMonitoringByCategory(Long supplierId, String riskCategory, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierRiskMonitoringByCategoryUrl() + "/" + supplierId + "/category/"
				+ riskCategory;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllSupplierRiskMonitoring(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierRiskMonitoringAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getLatestSupplierPerformance(Long supplierId, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierPerformanceLatestUrl() + "/" + supplierId + "/latest";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformanceSummaryByAccount(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierPerformanceSummaryAccountUrl(),
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierPerformanceSummaryBySupplier(Long supplierId, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierPerformanceSummarySupplierUrl() + "/" + supplierId + "/summary";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> calculateSupplierPerformance(Long supplierId, String authToken, String orgIdHeader,
			java.sql.Date startDate, java.sql.Date endDate, String calculatedBy) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierPerformanceCalculateUrl(), null,
				"supplierId", supplierId,
				"startDate", startDate,
				"endDate", endDate,
				"calculatedBy", calculatedBy);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> updateSupplierPerformance(Long id, Map<String, Object> performanceDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierPerformanceUpdateUrl() + "/" + id + "/update";
		return restService.iamRestCall(
				url,
				performanceDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> deleteSupplierPerformance(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierPerformanceDeleteUrl() + "/" + id + "/delete";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.DELETE,
				null);
	}

	// Supplier Contract Endpoints
	@Override
	public ResponseEntity<?> createSupplierContract(Map<String, Object> contractDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierContractAddUrl(),
				contractDto,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierContract(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractGetUrl() + "/" + id;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierContractByNumber(String contractNumber, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractGetByNumberUrl() + "/" + contractNumber;
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAllSupplierContracts(
			String authToken, String orgIdHeader, Pageable pageable,
			String status, Long supplierId, String contractType,
			java.sql.Date effectiveStartDate, java.sql.Date effectiveEndDate,
			java.sql.Date expiryStartDate, java.sql.Date expiryEndDate,
			Boolean expiringOnly, java.sql.Date expiringBeforeDate,
			Boolean autoRenewalOnly, java.sql.Date autoRenewalBeforeDate) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierContractAllUrl(), pageable,
				"status", status,
				"supplierId", supplierId,
				"contractType", contractType,
				"effectiveStartDate", effectiveStartDate,
				"effectiveEndDate", effectiveEndDate,
				"expiryStartDate", expiryStartDate,
				"expiryEndDate", expiryEndDate,
				"expiringOnly", expiringOnly,
				"expiringBeforeDate", expiringBeforeDate,
				"autoRenewalOnly", autoRenewalOnly,
				"autoRenewalBeforeDate", autoRenewalBeforeDate);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getExpiringSupplierContracts(String authToken, String orgIdHeader,
			java.sql.Date beforeDate) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder.fromUriString(webConstants.getCoreSupplierContractExpiringUrl())
				.queryParam("beforeDate", beforeDate)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getAutoRenewalSupplierContracts(String authToken, String orgIdHeader,
			java.sql.Date beforeDate) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder.fromUriString(webConstants.getCoreSupplierContractAutoRenewalUrl())
				.queryParam("beforeDate", beforeDate)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getActiveSupplierContractBySupplierAndDate(Long supplierId, String authToken,
			String orgIdHeader,
			java.sql.Date date) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractActiveBySupplierUrl() + "/" + supplierId)
				.queryParam("date", date)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierContractSummary(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreSupplierContractSummaryUrl(),
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> updateSupplierContract(Long id, Map<String, Object> contractDto, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractUpdateUrl() + "/" + id + "/update";
		return restService.iamRestCall(
				url,
				contractDto,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> updateSupplierContractStatus(Long id, String status, String reason, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractStatusUrl() + "/" + id + "/status")
				.queryParam("status", status)
				.queryParamIfPresent("reason", java.util.Optional.ofNullable(reason))
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.PUT,
				null);
	}

	@Override
	public ResponseEntity<?> uploadSupplierContractDocument(Long id, MultipartFile file, String documentName,
			String remarks,
			String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildMultipartHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractDocumentUrl() + "/" + id + "/documents";

		org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
		try {
			body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
				@Override
				public String getFilename() {
					return file.getOriginalFilename();
				}
			});
		} catch (java.io.IOException e) {
			log.error("Failed to read file bytes: {}", e.getMessage());
			return ResponseEntity.internalServerError().body(Map.of("error", "Failed to read file"));
		}
		if (documentName != null) {
			body.add("documentName", documentName);
		}
		if (remarks != null) {
			body.add("remarks", remarks);
		}

		return restService.iamRestCall(
				url,
				body,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> getSupplierContractDocument(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractDocumentUrl() + "/" + id + "/documents";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> deleteSupplierContractDocument(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractDocumentUrl() + "/" + id + "/documents";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.DELETE,
				null);
	}

	@Override
	public ResponseEntity<?> approveSupplierContract(Long id, String approvedBy, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractApproveUrl() + "/" + id + "/approve")
				.queryParam("approvedBy", approvedBy)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> rejectSupplierContract(Long id, String rejectionReason, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractRejectUrl() + "/" + id + "/reject")
				.queryParam("rejectionReason", rejectionReason)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> terminateSupplierContract(Long id, String reason, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractTerminateUrl() + "/" + id + "/terminate")
				.queryParam("reason", reason)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> suspendSupplierContract(Long id, String reason, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractSuspendUrl() + "/" + id + "/suspend")
				.queryParam("reason", reason)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> renewSupplierContract(Long id, java.sql.Date newExpiryDate, String authToken,
			String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierContractRenewUrl() + "/" + id + "/renew")
				.queryParam("newExpiryDate", newExpiryDate)
				.toUriString();
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.POST,
				null);
	}

	@Override
	public ResponseEntity<?> deleteSupplierContract(Long id, String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = webConstants.getCoreSupplierContractDeleteUrl() + "/" + id + "/delete";
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.DELETE,
				null);
	}

	/**
	 * Builds a URL with pagination parameters from Pageable
	 */
	private String buildPaginatedUrl(String baseUrl, Pageable pageable) {
		return UriComponentsBuilder.fromUriString(baseUrl)
				.queryParam("page", pageable.getPageNumber())
				.queryParam("size", pageable.getPageSize())
				.queryParam("sort", pageable.getSort().toString())
				.toUriString();
	}

	/**
	 * Builds a URL with pagination and optional filter parameters
	 */
	private String buildPaginatedUrlWithFilters(String baseUrl, Pageable pageable, Object... filterParams) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
				.queryParam("page", pageable.getPageNumber())
				.queryParam("size", pageable.getPageSize())
				.queryParam("sort", pageable.getSort().toString());

		// filterParams should be key-value pairs: key1, value1, key2, value2, ...
		for (int i = 0; i < filterParams.length; i += 2) {
			if (i + 1 < filterParams.length && filterParams[i + 1] != null) {
				builder.queryParam((String) filterParams[i], filterParams[i + 1]);
			}
		}

		return builder.toUriString();
	}
}