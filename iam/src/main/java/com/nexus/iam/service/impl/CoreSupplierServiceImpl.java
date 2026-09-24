package com.nexus.iam.service.impl;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.service.CoreSupplierService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreSupplierServiceImpl implements CoreSupplierService {

	private final RestService restService;
	private final WebConstants webConstants;
	private final CommonUtils commonUtils;

	// Catalog
	@Override
	public ResponseEntity<?> createCatalog(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierCatalogCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getCatalog(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierCatalogGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllCatalogs(String authToken, String orgId, Pageable pageable, String status,
			String category, String family, String accessLevel, Boolean isPublished, String search) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierCatalogAllUrl(), pageable, "status",
				status, "category", category, "family", family, "accessLevel", accessLevel, "isPublished", isPublished,
				"search", search);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> updateCatalog(Long id, Map<String, Object> dto, String authToken, String orgId) {
		return callPut(webConstants.getCoreSupplierCatalogGetUrl() + "/" + id + "/update", dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> transitionCatalogStatus(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgId) {
		String url = webConstants.getCoreSupplierCatalogGetUrl() + "/" + id + "/status?newStatus=" + newStatus;
		return callPut(url, params, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deleteCatalog(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierCatalogGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getCatalogSummary(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierCatalogSummaryUrl(), authToken, orgId);
	}

	// Variant
	@Override
	public ResponseEntity<?> createVariant(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierVariantCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getVariant(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierVariantGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllVariants(String authToken, String orgId, Pageable pageable, Long catalogId,
			String variantType, Long bomMaterialId) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierVariantAllUrl(), pageable, "catalogId",
				catalogId, "variantType", variantType, "bomMaterialId", bomMaterialId);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> updateVariant(Long id, Map<String, Object> dto, String authToken, String orgId) {
		return callPut(webConstants.getCoreSupplierVariantGetUrl() + "/" + id + "/update", dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deleteVariant(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierVariantGetUrl() + "/" + id, authToken, orgId);
	}

	// Price Tier
	@Override
	public ResponseEntity<?> createPriceTier(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierPriceTierCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getPriceTier(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierPriceTierGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllPriceTiers(String authToken, String orgId, Pageable pageable, Long catalogId,
			String customerSegment, Long contractId) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierPriceTierAllUrl(), pageable, "catalogId",
				catalogId, "customerSegment", customerSegment, "contractId", contractId);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> updatePriceTier(Long id, Map<String, Object> dto, String authToken, String orgId) {
		return callPut(webConstants.getCoreSupplierPriceTierGetUrl() + "/" + id + "/update", dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deletePriceTier(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierPriceTierGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getPriceForQuantity(Long catalogId, Double quantity, String customerSegment,
			String authToken, String orgId) {
		String url = UriComponentsBuilder
				.fromUriString(webConstants.getCoreSupplierPriceTierAllUrl().replace("/all", "/price-for-quantity"))
				.queryParam("catalogId", catalogId).queryParam("quantity", quantity)
				.queryParamIfPresent("customerSegment", Optional.ofNullable(customerSegment)).toUriString();
		// fallback to direct price-tiers/price-for-quantity endpoint
		url = webConstants.getCoreServiceUrl() + "/core/supplier/price-tiers/price-for-quantity?catalogId=" + catalogId
				+ "&quantity=" + quantity + (customerSegment != null ? "&customerSegment=" + customerSegment : "");
		return callGet(url, authToken, orgId);
	}

	// Digital Asset
	@Override
	public ResponseEntity<?> createDigitalAsset(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierDigitalAssetCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getDigitalAsset(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierDigitalAssetGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllDigitalAssets(String authToken, String orgId, Pageable pageable, Long catalogId,
			String assetType) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierDigitalAssetAllUrl(), pageable,
				"catalogId", catalogId, "assetType", assetType);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deleteDigitalAsset(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierDigitalAssetGetUrl() + "/" + id, authToken, orgId);
	}

	// Capacity
	@Override
	public ResponseEntity<?> createCapacity(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierCapacityCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getCapacity(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierCapacityGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllCapacities(String authToken, String orgId, Pageable pageable, String productLine,
			String shift) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierCapacityAllUrl(), pageable, "productLine",
				productLine, "shift", shift);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> updateCapacity(Long id, Map<String, Object> dto, String authToken, String orgId) {
		return callPut(webConstants.getCoreSupplierCapacityGetUrl() + "/" + id + "/update", dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deleteCapacity(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierCapacityGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getCapacitySummary(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierCapacitySummaryUrl(), authToken, orgId);
	}

	// ATP
	@Override
	public ResponseEntity<?> getAtpForCatalog(Long catalogId, Double quantity, String authToken, String orgId) {
		String url = webConstants.getCoreSupplierAtpCatalogUrl() + "/" + catalogId
				+ (quantity != null ? "?requestedQuantity=" + quantity : "");
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAtpForProductLine(String productLine, String authToken, String orgId) {
		String url = webConstants.getCoreSupplierAtpProductLineUrl()
				+ (productLine != null ? "?productLine=" + productLine : "");
		return callGet(url, authToken, orgId);
	}

	// Orders
	@Override
	public ResponseEntity<?> getSupplierOrder(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierOrderGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllSupplierOrders(String authToken, String orgId, Pageable pageable, String status,
			String poNumber) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierOrderAllUrl(), pageable, "status", status,
				"poNumber", poNumber);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> acknowledgeOrder(Long id, String authToken, String orgId, String confirmedDeliveryDate,
			String notes) {
		String url = webConstants.getCoreSupplierOrderAcknowledgeUrl() + "/" + id + "/acknowledge";
		if (confirmedDeliveryDate != null || notes != null) {
			UriComponentsBuilder b = UriComponentsBuilder.fromUriString(url);
			if (confirmedDeliveryDate != null)
				b.queryParam("confirmedDeliveryDate", confirmedDeliveryDate);
			if (notes != null)
				b.queryParam("notes", notes);
			url = b.toUriString();
		}
		return callPut(url, null, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> updateOrderFulfillment(Long id, Map<String, Object> updates, String authToken,
			String orgId) {
		String url = webConstants.getCoreSupplierOrderGetUrl() + "/" + id + "/fulfillment";
		return callPut(url, updates, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> createPartialShipment(Long purchaseOrderId, Map<String, Object> dto, String authToken,
			String orgId) {
		String url = webConstants.getCoreServiceUrl() + "/core/supplier/orders/" + purchaseOrderId
				+ "/partial-shipment";
		return callPost(url, dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getOrderSummary(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierOrderSummaryUrl(), authToken, orgId);
	}

	// Quality
	@Override
	public ResponseEntity<?> createQualityCert(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierQualityCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getQualityCert(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierQualityGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllQualityCerts(String authToken, String orgId, Pageable pageable, Long poId,
			Long catalogId, String certType) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierQualityAllUrl(), pageable,
				"purchaseOrderId", poId, "catalogId", catalogId, "certificateType", certType);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deleteQualityCert(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierQualityGetUrl() + "/" + id, authToken, orgId);
	}

	// Quotation
	@Override
	public ResponseEntity<?> createQuotation(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierQuotationCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getQuotation(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierQuotationGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllQuotations(String authToken, String orgId, Pageable pageable, String status) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierQuotationAllUrl(), pageable, "status",
				status);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> transitionQuotation(Long id, String newStatus, Map<String, Object> params,
			String authToken, String orgId) {
		String url = webConstants.getCoreSupplierQuotationGetUrl() + "/" + id + "/status?newStatus=" + newStatus;
		return callPut(url, params, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> convertQuotation(Long id, String authToken, String orgId) {
		String url = webConstants.getCoreSupplierQuotationGetUrl() + "/" + id + "/convert-to-order";
		return callPost(url, null, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getQuotationSummary(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierQuotationSummaryUrl(), authToken, orgId);
	}

	// Forecast
	@Override
	public ResponseEntity<?> createForecast(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierForecastCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getForecast(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierForecastGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllForecasts(String authToken, String orgId, Pageable pageable) {
		return callGet(buildPaginatedUrl(webConstants.getCoreSupplierForecastAllUrl(), pageable), authToken, orgId);
	}

	@Override
	public ResponseEntity<?> updateForecast(Long id, Map<String, Object> dto, String authToken, String orgId) {
		return callPut(webConstants.getCoreSupplierForecastGetUrl() + "/" + id + "/update", dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> deleteForecast(Long id, String authToken, String orgId) {
		return callDelete(webConstants.getCoreSupplierForecastGetUrl() + "/" + id, authToken, orgId);
	}

	// Customer Portal
	@Override
	public ResponseEntity<?> getCustomerOrders(Long buyerOrgId, String authToken, String orgId, Pageable pageable) {
		String url = buildPaginatedUrlWithFilters(webConstants.getCoreSupplierCustomerPortalOrdersUrl(), pageable,
				"buyerOrgId", buyerOrgId);
		return callGet(url, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getCustomerSummary(Long buyerOrgId, String authToken, String orgId) {
		String url = webConstants.getCoreSupplierCustomerPortalSummaryUrl()
				+ (buyerOrgId != null ? "?buyerOrgId=" + buyerOrgId : "");
		return callGet(url, authToken, orgId);
	}

	// Account Health
	@Override
	public ResponseEntity<?> getAccountHealth(Long buyerOrgId, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierAccountHealthGetUrl() + "/" + buyerOrgId, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllAccountHealth(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierAccountHealthAllUrl(), authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAccountHealthSummary(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierAccountHealthSummaryUrl(), authToken, orgId);
	}

	// Consignment
	@Override
	public ResponseEntity<?> createConsignment(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierConsignmentCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getConsignment(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierConsignmentGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllConsignments(String authToken, String orgId, Pageable pageable) {
		return callGet(buildPaginatedUrl(webConstants.getCoreSupplierConsignmentAllUrl(), pageable), authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getConsignmentSummary(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierConsignmentSummaryUrl(), authToken, orgId);
	}

	// VMI
	@Override
	public ResponseEntity<?> createVmi(Map<String, Object> dto, String authToken, String orgId) {
		return callPost(webConstants.getCoreSupplierVmiCreateUrl(), dto, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getVmi(Long id, String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierVmiGetUrl() + "/" + id, authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getAllVmi(String authToken, String orgId, Pageable pageable) {
		return callGet(buildPaginatedUrl(webConstants.getCoreSupplierVmiAllUrl(), pageable), authToken, orgId);
	}

	@Override
	public ResponseEntity<?> getVmiSuggestions(String authToken, String orgId) {
		return callGet(webConstants.getCoreSupplierVmiSuggestionsUrl(), authToken, orgId);
	}

	// Analytics
	@Override
	public ResponseEntity<?> getSupplierDashboard(String authToken, String orgId) {
		return callGet(webConstants.getCoreAnalyticsSupplierDashboardUrl(), authToken, orgId);
	}

	private ResponseEntity<?> callGet(String url, String authToken, String orgId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgId);
		return restService.iamRestCall(url, null, headers, HttpMethod.GET, null);
	}

	private ResponseEntity<?> callPost(String url, Object body, String authToken, String orgId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgId);
		return restService.iamRestCall(url, body, headers, HttpMethod.POST, null);
	}

	private ResponseEntity<?> callPut(String url, Object body, String authToken, String orgId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgId);
		return restService.iamRestCall(url, body, headers, HttpMethod.PUT, null);
	}

	private ResponseEntity<?> callDelete(String url, String authToken, String orgId) {
		Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
		headers.put("X-Organization-ID", orgId);
		return restService.iamRestCall(url, null, headers, HttpMethod.DELETE, null);
	}

	private String buildPaginatedUrl(String baseUrl, Pageable pageable) {
		if (pageable == null || pageable.isUnpaged())
			return baseUrl;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
				.queryParam("page", pageable.getPageNumber()).queryParam("size", pageable.getPageSize());
		if (pageable.getSort().isSorted())
			for (Sort.Order o : pageable.getSort())
				builder.queryParam("sort", o.getProperty() + "," + o.getDirection().name());
		return builder.toUriString();
	}

	private String buildPaginatedUrlWithFilters(String baseUrl, Pageable pageable, Object... filters) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
		if (pageable != null && !pageable.isUnpaged()) {
			builder.queryParam("page", pageable.getPageNumber()).queryParam("size", pageable.getPageSize());
			if (pageable.getSort().isSorted())
				for (Sort.Order o : pageable.getSort())
					builder.queryParam("sort", o.getProperty() + "," + o.getDirection().name());
		}
		for (int i = 0; i < filters.length; i += 2)
			if (i + 1 < filters.length && filters[i + 1] != null)
				builder.queryParam((String) filters[i], filters[i + 1]);
		return builder.toUriString();
	}
}
