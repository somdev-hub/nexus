package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.service.CoreRetailerService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;

/**
 * Core Retailer Service Implementation
 * <p>
 * Handles retailer-specific Core module operations through IAM gateway.
 * All HTTP calls to Core module are handled here using RestService.
 */
@Service
@RequiredArgsConstructor
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