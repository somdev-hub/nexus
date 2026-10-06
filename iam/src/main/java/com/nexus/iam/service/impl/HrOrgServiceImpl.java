package com.nexus.iam.service.impl;

import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.nexus.iam.service.HrOrgService;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;

/**
 * HR Organization Service Implementation
 * <p>
 * Proxies organization bank/account details and organization addresses to
 * the HR module ({@code /hr/orgs/...}). The caller's Authorization token is
 * forwarded so HR can validate it.
 */
@Service
@RequiredArgsConstructor
public class HrOrgServiceImpl implements HrOrgService {

    private final RestService restService;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;

    private String baseUrl() {
        return webConstants.getHrOrgBaseUrl();
    }

    private Map<String, String> headers(String authToken) {
        return commonUtils.buildJsonHeaders(authToken);
    }

    @Override
    public ResponseEntity<?> createOrgAccountInfo(Long orgId, Map<String, Object> dto, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/account-info", dto, headers(authToken),
                HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<?> getOrgAccountInfo(Long orgId, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/account-info", null, headers(authToken),
                HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<?> updateOrgAccountInfo(Long orgId, Map<String, Object> dto, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/account-info", dto, headers(authToken),
                HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<?> deleteOrgAccountInfo(Long orgId, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/account-info", null, headers(authToken),
                HttpMethod.DELETE, null);
    }

    @Override
    public ResponseEntity<?> createOrgAddress(Long orgId, Map<String, Object> dto, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/addresses", dto, headers(authToken),
                HttpMethod.POST, null);
    }

    @Override
    public ResponseEntity<?> getOrgAddresses(Long orgId, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/addresses", null, headers(authToken),
                HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<?> getOrgAddress(Long orgId, Long addressId, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/addresses/" + addressId, null,
                headers(authToken), HttpMethod.GET, null);
    }

    @Override
    public ResponseEntity<?> updateOrgAddress(Long orgId, Long addressId, Map<String, Object> dto,
            String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/addresses/" + addressId, dto,
                headers(authToken), HttpMethod.PUT, null);
    }

    @Override
    public ResponseEntity<?> deleteOrgAddress(Long orgId, Long addressId, String authToken) {
        return restService.iamRestCall(baseUrl() + "/" + orgId + "/addresses/" + addressId, null,
                headers(authToken), HttpMethod.DELETE, null);
    }
}
