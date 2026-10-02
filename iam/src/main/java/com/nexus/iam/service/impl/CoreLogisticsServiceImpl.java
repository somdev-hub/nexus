package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.service.CoreLogisticsService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;

/**
 * Core Logistics Service Implementation
 * <p>
 * Handles logistics-specific Core module operations through IAM gateway.
 * All HTTP calls to Core module are handled here using RestService.
 */
@Service
@RequiredArgsConstructor
public class CoreLogisticsServiceImpl implements CoreLogisticsService {

    private final RestService restService;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;

    // Fleet assets
    @Override
    public ResponseEntity<?> createAsset(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsFleetAssetCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getAsset(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllAssets(String auth, String org, Pageable p, String status, String assetType, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsFleetAssetAllUrl(), p,
                "status", status, "assetType", assetType, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateAsset(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionAssetStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteAsset(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetSummary(String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetSummaryUrl(), auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetShipments(Long assetId, String auth, String org, Pageable p, String status, String from, String to) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + assetId + "/shipments", p,
                "status", status, "from", from, "to", to), auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetDrivers(Long assetId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + assetId + "/drivers", auth, org);
    }

    @Override
    public ResponseEntity<?> getAssetCurrentShipment(Long assetId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsFleetAssetGetUrl() + "/" + assetId + "/current-shipment", auth, org);
    }

    // Drivers
    @Override
    public ResponseEntity<?> createDriver(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsDriverCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getDriver(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllDrivers(String auth, String org, Pageable p, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsDriverAllUrl(), p,
                "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateDriver(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionDriverStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteDriver(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsDriverGetUrl() + "/" + id, auth, org);
    }

    // Maintenance
    @Override
    public ResponseEntity<?> createMaintenance(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsMaintenanceCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getMaintenance(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllMaintenance(String auth, String org, Pageable p, Long assetId, String status, Boolean isBreakdown, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsMaintenanceAllUrl(), p,
                "assetId", assetId, "status", status, "isBreakdown", isBreakdown, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> transitionMaintenanceStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> updateMaintenance(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteMaintenance(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsMaintenanceGetUrl() + "/" + id, auth, org);
    }

    // Load board + quotes + rates
    @Override
    public ResponseEntity<?> getLoadBoard(String auth, String org, Pageable p, String mode, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsLoadBoardUrl(), p,
                "mode", mode, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> createQuote(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsQuoteCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getQuote(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllQuotes(String auth, String org, Pageable p, Long shipmentId, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsQuoteAllUrl(), p,
                "shipmentId", shipmentId, "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateQuote(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionQuoteStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteQuote(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsQuoteGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> createRate(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsRateCreateUrl(), dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getRate(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsRateGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllRates(String auth, String org, Pageable p, String rateType, String equipmentType, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsRateAllUrl(), p,
                "rateType", rateType, "equipmentType", equipmentType, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateRate(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsRateGetUrl() + "/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteRate(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsRateGetUrl() + "/" + id, auth, org);
    }

    // Execution
    @Override
    public ResponseEntity<?> assignBooking(Long shipmentId, Map<String, Object> body, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/assign", body, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionShipmentStatus(Long shipmentId, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> getShipmentPosition(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/position", auth, org);
    }

    @Override
    public ResponseEntity<?> updateShipmentRoute(Long shipmentId, Map<String, Object> body, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/route", body, auth, org);
    }
    @Override
    public ResponseEntity<?> getShipmentEta(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/shipments/" + shipmentId + "/eta", auth, org);
    }

    @Override
    public ResponseEntity<?> capturePod(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/capture", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> updatePod(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getPodByShipment(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/shipment/" + shipmentId, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllPods(String auth, String org, Pageable p, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsExecutionBaseUrl() + "/pod/all", p,
                "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> reportIncident(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/report", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> updateIncident(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getIncident(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllIncidents(String auth, String org, Pageable p, Long shipmentId, String incidentType, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/all", p,
                "shipmentId", shipmentId, "incidentType", incidentType, "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> transitionIncidentStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsExecutionBaseUrl() + "/incidents/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    // Operations
    @Override
    public ResponseEntity<?> createGroup(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/create", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getGroup(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllGroups(String auth, String org, Pageable p, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/all", p,
                "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateGroup(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> transitionGroupStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> addShipmentsToGroup(Long id, Map<String, Object> body, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id + "/shipments", body, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteGroup(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsOperationsBaseUrl() + "/consolidation/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> createCapacity(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/create", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getCapacity(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllCapacities(String auth, String org, Pageable p, String equipmentType, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/all", p,
                "equipmentType", equipmentType, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> updateCapacity(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> deleteCapacity(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsOperationsBaseUrl() + "/capacity/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> createPayable(Map<String, Object> dto, String auth, String org) {
        return callPost(webConstants.getCoreLogisticsOperationsBaseUrl() + "/payables/create", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> updatePayable(Long id, Map<String, Object> dto, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/payables/" + id + "/update", dto, auth, org);
    }

    @Override
    public ResponseEntity<?> getPayable(Long id, String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/payables/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getAllPayables(String auth, String org, Pageable p, Long shipmentId, String status, String search) {
        return callGet(buildPaginatedUrlWithFilters(webConstants.getCoreLogisticsOperationsBaseUrl() + "/payables/all", p,
                "shipmentId", shipmentId, "status", status, "search", search), auth, org);
    }

    @Override
    public ResponseEntity<?> transitionPayableStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org) {
        return callPut(webConstants.getCoreLogisticsOperationsBaseUrl() + "/payables/" + id + "/status?newStatus=" + newStatus, params, auth, org);
    }

    @Override
    public ResponseEntity<?> deletePayable(Long id, String auth, String org) {
        return callDelete(webConstants.getCoreLogisticsOperationsBaseUrl() + "/payables/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getLogisticsDashboard(String auth, String org) {
        return callGet(webConstants.getCoreLogisticsOperationsBaseUrl() + "/analytics/dashboard", auth, org);
    }

    // Partnership Invitations (same core URLs as retailer service)
    @Override
    public ResponseEntity<?> createPartnershipInvitation(Map<String, Object> invitationDto, String auth, String org) {
        return callPost(webConstants.getCorePartnershipInvitationCreateUrl(), invitationDto, auth, org);
    }

    @Override
    public ResponseEntity<?> respondToPartnershipInvitation(Long id, Map<String, Object> responseDto, String auth, String org) {
        return callPut(webConstants.getCorePartnershipInvitationRespondUrl() + "/" + id + "/respond", responseDto, auth, org);
    }

    @Override
    public ResponseEntity<?> getPartnershipInvitation(Long id, String auth, String org) {
        return callGet(webConstants.getCorePartnershipInvitationGetUrl() + "/" + id, auth, org);
    }

    @Override
    public ResponseEntity<?> getSentPartnershipInvitations(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipInvitationSentUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> getReceivedPartnershipInvitations(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipInvitationReceivedUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> getPendingPartnershipInvitations(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipInvitationPendingUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> withdrawPartnershipInvitation(Long id, String auth, String org) {
        return callPut(webConstants.getCorePartnershipInvitationWithdrawUrl() + "/" + id + "/withdraw", null, auth, org);
    }

    // Logistics shipment list (Core GET /core/shipments/by-logistics-org)
    @Override
    public ResponseEntity<?> getMyShipments(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCoreServiceUrl() + "/core/shipments/by-logistics-org", p), auth, org);
    }

    @Override
    public ResponseEntity<?> getAllPartnerships(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCorePartnershipMineUrl(), p), auth, org);
    }

    @Override
    public ResponseEntity<?> updatePartnership(Long id, Map<String, Object> partnershipDto, String auth, String org) {
        return callPut(webConstants.getCorePartnershipGetUrl() + "/" + id + "/update", partnershipDto, auth, org);
    }

    @Override
    public ResponseEntity<?> updatePartnershipStatus(Long id, String status, String auth, String org) {
        return callPost(webConstants.getCorePartnershipUpdateStatusUrl() + "/" + id + "/status", Map.of("status", status), auth, org);
    }

    // Freight invoice passthroughs (Core P1)
    @Override
    public ResponseEntity<?> getFreightInvoicesByLogisticsOrg(String auth, String org, Pageable p) {
        return callGet(buildPaginatedUrl(webConstants.getCoreFreightInvoiceBaseUrl() + "/by-logistics-org", p), auth, org);
    }

    @Override
    public ResponseEntity<?> getFreightInvoicesByShipment(Long shipmentId, String auth, String org) {
        return callGet(webConstants.getCoreFreightInvoiceBaseUrl() + "/shipment/" + shipmentId, auth, org);
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
