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
	public ResponseEntity<?> getAllSuppliers(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getSuppliersByAccount(Long accountId, String authToken, String orgIdHeader,
			Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCoreSupplierByAccountUrl() + "/" + accountId, pageable);
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
	public ResponseEntity<?> getAllPurchaseOrders(String authToken, String orgIdHeader, Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePurchaseOrderAllUrl(), pageable);
		return restService.iamRestCall(
				url,
				null,
				headers,
				HttpMethod.GET,
				null);
	}

	@Override
	public ResponseEntity<?> getPurchaseOrdersByStatus(String status, String authToken, String orgIdHeader,
			Pageable pageable) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		String url = buildPaginatedUrl(webConstants.getCorePurchaseOrderByStatusUrl() + "/" + status, pageable);
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
}