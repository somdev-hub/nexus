package com.nexus.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommonUtils {

	private final ObjectMapper objectMapper;
	private final CommonConstants commonConstants;
	private final RestClient restClient;

	public CommonUtils(ObjectMapper objectMapper, CommonConstants commonConstants, RestClient restClient) {
		this.objectMapper = objectMapper;
		this.commonConstants = commonConstants;
		this.restClient = restClient;
	}

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
			ResponseEntity<String> response = restClient.post()
					.uri(url)
					.headers(h -> headers.forEach(h::set))
					.retrieve()
					.toEntity(String.class);
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
		return restClient.get()
				.uri(url)
				.headers(h -> headers.forEach(h::set))
				.retrieve()
				.toEntity(String.class);
	}

	/**
	 * Calls IAM service to validate user's access to an organization.
	 */
	public boolean validateUserOrganizationAccess(Long userId, Long organizationId, String authToken) {
		try {
			String url = commonConstants.getIamServiceUrl() + commonConstants.getIamUserUrl() + "/" + userId
					+ "/organizations/" + organizationId + "/access";
			Map<String, String> headers = buildJsonHeaders(authToken);
			ResponseEntity<String> response = restClient.get()
					.uri(url)
					.headers(h -> headers.forEach(h::set))
					.retrieve()
					.toEntity(String.class);
			return response.getStatusCode().is2xxSuccessful();
		} catch (Exception e) {
			return false;
		}
	}
}
