package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
	public ResponseEntity<?> getAllProducts(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreProductAllUrl(),
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
	public ResponseEntity<?> getAllMaterials(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreMaterialAllUrl(),
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
	public ResponseEntity<?> getAllWarehouses(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreWarehouseAllUrl(),
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
	public ResponseEntity<?> getAllOrders(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCoreOrderAllUrl(),
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
	public ResponseEntity<?> getAllPartnerships(String authToken, String orgIdHeader) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgIdHeader);
		return restService.iamRestCall(
				webConstants.getCorePartnershipAllUrl(),
				null,
				headers,
				HttpMethod.GET,
				null);
	}
}