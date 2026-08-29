package com.nexus.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommonUtils {

	private final ObjectMapper objectMapper;
	private final CommonConstants commonConstants;
	private final RestService restService;

	/**
	 * Validates JSON string and returns valid JSON.
	 * If invalid, wraps the string in a JSON object with "message" key.
	 */
	public String jsonValidator(String jsonString) {
		if (ObjectUtils.isEmpty(jsonString)) {
			return "{}";
		}
		JsonNode jsonNode = null;
		try {
			jsonNode = objectMapper.readTree(jsonString);
		} catch (JsonProcessingException ex) {
			jsonNode = objectMapper.createObjectNode().put("message", jsonString);
		}
		try {
			return objectMapper.writeValueAsString(jsonNode);
		} catch (JsonProcessingException e) {
			return "{}";
		}
	}

	/**
	 * Builds standard JSON headers with Authorization token.
	 * If no token provided, attempts to get a generic service token from IAM.
	 */
	public Map<String, String> buildJsonHeaders(String authToken) {
		Map<String, String> headers = new ConcurrentHashMap<>();
		headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		if (!ObjectUtils.isEmpty(authToken)) {
			headers.put(HttpHeaders.AUTHORIZATION, authToken);
		}
		return headers;
	}

	/**
	 * Builds headers with organization context for IAM calls.
	 */
	public Map<String, String> buildJsonHeadersWithOrg(String authToken, Long organizationId) {
		Map<String, String> headers = buildJsonHeaders(authToken);
		if (organizationId != null) {
			headers.put(CommonConstants.X_ORGANIZATION_ID, organizationId.toString());
		}
		return headers;
	}

	/**
	 * Validates a JWT token by calling IAM's token validation endpoint.
	 */
	public boolean validateToken(String token) {
		if (ObjectUtils.isEmpty(token)) {
			return false;
		}
		try {
			String url = commonConstants.getIamServiceUrl() + commonConstants.getVerifyTokenUrl();
			Map<String, String> headers = buildJsonHeaders(token);
			ResponseEntity<String> response = restService.coreRestCall(url, null, headers,
					org.springframework.http.HttpMethod.POST, null);
			return response.getStatusCode().is2xxSuccessful();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Calls IAM service to get organization details.
	 */
	public ResponseEntity<String> getOrganizationFromIam(Long organizationId, String authToken) {
		String url = commonConstants.getIamServiceUrl() + commonConstants.getIamOrganizationUrl() + "/"
				+ organizationId;
		Map<String, String> headers = buildJsonHeaders(authToken);
		return restService.coreRestCall(url, null, headers, org.springframework.http.HttpMethod.GET, null);
	}

	/**
	 * Calls IAM service to validate user's access to an organization.
	 */
	public boolean validateUserOrganizationAccess(Long userId, Long organizationId, String authToken) {
		try {
			String url = commonConstants.getIamServiceUrl() + commonConstants.getIamUserUrl() + "/" + userId
					+ "/organizations/" + organizationId + "/access";
			Map<String, String> headers = buildJsonHeaders(authToken);
			ResponseEntity<String> response = restService.coreRestCall(url, null, headers,
					org.springframework.http.HttpMethod.GET, null);
			return response.getStatusCode().is2xxSuccessful();
		} catch (Exception e) {
			return false;
		}
	}
}
