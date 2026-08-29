package com.nexus.core.utils;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestService {

	private final RestClient restClient;
	private final WebConstants webConstants;
	private final CommonUtils commonUtils;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Parse JSON response body into a Map.
	 * Returns empty map if body is null, empty, or not valid JSON.
	 */
	public Map<String, Object> parseJsonResponse(String responseBody) {
		if (responseBody == null || responseBody.trim().isEmpty()) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			log.warn("Failed to parse JSON response: {}", e.getMessage());
			return Map.of();
		}
	}

	/**
	 * Parse JSON response body into a specific type.
	 * Returns null if body is null, empty, or not valid JSON for the target type.
	 */
	public <T> T parseJsonResponse(String responseBody, Class<T> targetType) {
		if (responseBody == null || responseBody.trim().isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(responseBody, targetType);
		} catch (Exception e) {
			log.warn("Failed to parse JSON response to {}: {}", targetType.getSimpleName(), e.getMessage());
			return null;
		}
	}

	/**
	 * Parse JSON response body into a generic type using TypeReference.
	 * Returns null if body is null, empty, or not valid JSON for the target type.
	 */
	public <T> T parseJsonResponse(String responseBody, TypeReference<T> typeReference) {
		if (responseBody == null || responseBody.trim().isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(responseBody, typeReference);
		} catch (Exception e) {
			log.warn("Failed to parse JSON response: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Centralized REST call method for Core module.
	 * Supports both multipart and regular requests.
	 * 
	 * @param url     The target URL
	 * @param payload The request payload (can be Map for multipart, or any object
	 *                for regular)
	 * @param headers HTTP headers
	 * @param method  HTTP method
	 * @param orgId   Organization ID for logging/context
	 * @return ResponseEntity with String body
	 */
	public ResponseEntity<String> coreRestCall(String url, Object payload, Map<String, String> headers,
			HttpMethod method, Long orgId) {
		ResponseEntity<String> responseEntity = null;
		String requestLog = null;
		try {
			boolean isMultipartByHeader = headers != null
					&& headers.entrySet().stream()
							.anyMatch(e -> "Content-Type".equalsIgnoreCase(e.getKey())
									&& e.getValue() != null
									&& e.getValue().toLowerCase().contains(MediaType.MULTIPART_FORM_DATA_VALUE));

			// Check if payload contains multipart files
			boolean hasMultipartFile = false;
			if (payload instanceof Map) {
				@SuppressWarnings("rawtypes")
				Map payloadMap = (Map) payload;
				for (Object value : payloadMap.values()) {
					if (value instanceof MultipartFile) {
						hasMultipartFile = true;
						break;
					}
				}
			}

			log.debug("coreRestCall: url={}, method={}, isMultipartByHeader={}, hasMultipartFile={}, orgId={}",
					url, method, isMultipartByHeader, hasMultipartFile, orgId);

			if (hasMultipartFile || isMultipartByHeader) {
				responseEntity = handleMultipartRequest(url, (Map<String, Object>) payload, headers, method);
				requestLog = serializePayload(payload);
			} else {
				responseEntity = handleRegularRequest(url, payload, headers, method);
				requestLog = payload != null ? serializePayload(payload) : null;
			}
		} catch (Exception e) {
			log.error("Exception during REST call to {}: {}", url, e.getMessage(), e);
			responseEntity = new ResponseEntity<>("Exception occurred during REST call: " + e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
			requestLog = payload != null ? serializePayload(payload) : null;
		} finally {
			// Log the request/response for audit purposes
			logRestCall(url, method, requestLog, responseEntity, orgId);
		}

		return responseEntity;
	}

	/**
	 * Log REST call for audit trail
	 */
	private void logRestCall(String url, HttpMethod method, String requestLog,
			ResponseEntity<String> responseEntity, Long orgId) {
		try {
			// Could save to audit log table here if needed
			log.debug("REST Call Log - URL: {}, Method: {}, OrgId: {}, Status: {}",
					url, method, orgId,
					responseEntity != null ? responseEntity.getStatusCode() : "ERROR");
		} catch (Exception e) {
			log.warn("Failed to log REST call: {}", e.getMessage());
		}
	}

	private String serializePayload(Object payload) {
		try {
			if (payload instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) payload;
				// Create a copy to avoid serializing MultipartFile objects
				Map<String, Object> safeMap = new java.util.HashMap<>();
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if (entry.getValue() instanceof MultipartFile) {
						safeMap.put(entry.getKey(), "MultipartFile");
					} else {
						safeMap.put(entry.getKey(), entry.getValue());
					}
				}
				return objectMapper.writeValueAsString(safeMap);
			} else {
				return objectMapper.writeValueAsString(payload);
			}
		} catch (Exception e) {
			return payload != null ? payload.toString() : "null";
		}
	}

	private ResponseEntity<String> handleMultipartRequest(String url, Map<String, Object> payload,
			Map<String, String> headers, HttpMethod method) throws IOException {
		log.debug("handleMultipartRequest: payload keys={}", payload.keySet());

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

		for (Map.Entry<String, Object> entry : payload.entrySet()) {
			Object value = entry.getValue();
			log.debug("handleMultipartRequest: key={}, valueClass={}", entry.getKey(),
					value != null ? value.getClass().getName() : "null");

			if (value instanceof MultipartFile) {
				MultipartFile file = (MultipartFile) value;
				ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
					@Override
					public String getFilename() {
						return file.getOriginalFilename();
					}
				};
				HttpHeaders fileHeaders = new HttpHeaders();
				if (file.getContentType() != null) {
					fileHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
				}
				HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileResource, fileHeaders);
				body.add(entry.getKey(), filePart);
			} else {
				String jsonPart;
				if (value instanceof String) {
					String s = (String) value;
					String current = s;
					String normalized = null;
					for (int i = 0; i < 5; i++) {
						try {
							JsonNode node = objectMapper.readTree(current);
							if (node.isTextual()) {
								current = node.textValue();
								continue;
							} else {
								normalized = objectMapper.writeValueAsString(node);
								break;
							}
						} catch (Exception ex) {
							break;
						}
					}
					jsonPart = normalized != null ? normalized : current;
				} else {
					jsonPart = objectMapper.writeValueAsString(value);
				}
				HttpHeaders partHeaders = new HttpHeaders();
				partHeaders.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<String> partEntity = new HttpEntity<>(jsonPart, partHeaders);
				body.add(entry.getKey(), partEntity);
			}
		}

		// Build request headers — skip Content-Type (RestClient sets multipart boundary
		// automatically)
		HttpHeaders httpHeaders = new HttpHeaders();
		if (headers != null) {
			headers.forEach((key, value) -> {
				if (!key.equalsIgnoreCase("Content-Type")) {
					httpHeaders.set(key, value);
				}
			});
		}

		return restClient.method(method)
				.uri(url)
				.headers(h -> h.addAll(httpHeaders))
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.toEntity(String.class);
	}

	private ResponseEntity<String> handleRegularRequest(String url, Object payload, Map<String, String> headers,
			HttpMethod method) {
		HttpHeaders httpHeaders = new HttpHeaders();
		if (headers != null) {
			headers.forEach(httpHeaders::set);
		}

		var requestSpec = restClient.method(method)
				.uri(url)
				.headers(h -> h.addAll(httpHeaders));

		// Only add body for non-GET/HEAD requests with non-null payload
		if (payload != null && method != HttpMethod.GET && method != HttpMethod.HEAD) {
			requestSpec.body(payload);
		}

		return requestSpec.retrieve().toEntity(String.class);
	}

	// ============================================
	// Convenience methods for common DMS operations
	// ============================================

	/**
	 * Upload a file to DMS using org upload endpoint
	 * 
	 * @param file         The file to upload
	 * @param fileName     The name of the file
	 * @param orgId        Organization ID
	 * @param remarks      Remarks for the document
	 * @param documentType Document type (e.g., CONTRACT, INVOICE)
	 * @param orgType      Organization type (RETAILER, SUPPLIER, LOGISTICS, COMMON)
	 * @param authToken    Authorization token
	 * @param orgIdForLog  Organization ID for logging
	 * @return ResponseEntity with DMS response
	 */
	public ResponseEntity<String> uploadToDmsOrg(MultipartFile file, String fileName, Long orgId,
			String remarks, String documentType, String orgType, String authToken, Long orgIdForLog) {
		try {
			String dmsUploadUrl = webConstants.getOrgFileUploadUrl();

			Map<String, Object> dto = new java.util.HashMap<>();
			dto.put("fileName", fileName != null ? fileName : file.getOriginalFilename());
			dto.put("orgId", orgId);
			dto.put("remarks", remarks);
			dto.put("documentType", documentType);
			dto.put("orgType", orgType);

			Map<String, Object> docPayload = new java.util.HashMap<>();
			docPayload.put("dto", dto);
			docPayload.put("file", file);

			Map<String, String> dmsHeaders = new java.util.HashMap<>();
			dmsHeaders.put("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE);
			if (authToken != null) {
				dmsHeaders.put("Authorization", authToken);
			}

			return coreRestCall(dmsUploadUrl, docPayload, dmsHeaders, HttpMethod.POST, orgIdForLog);
		} catch (Exception e) {
			log.error("Error uploading to DMS org: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error uploading to DMS: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Get a document from DMS
	 * 
	 * @param documentId  DMS document ID
	 * @param authToken   Authorization token
	 * @param orgIdForLog Organization ID for logging
	 * @return ResponseEntity with DMS response
	 */
	public ResponseEntity<String> getFromDms(String documentId, String authToken, Long orgIdForLog) {
		try {
			String dmsGetUrl = webConstants.getDmsServiceUrl() + "/dms/files/org/" + documentId;

			Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);

			return coreRestCall(dmsGetUrl, null, headers, HttpMethod.GET, orgIdForLog);
		} catch (Exception e) {
			log.error("Error getting from DMS: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error getting from DMS: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Delete a document from DMS
	 * 
	 * @param documentId  DMS document ID
	 * @param authToken   Authorization token
	 * @param orgIdForLog Organization ID for logging
	 * @return ResponseEntity with DMS response
	 */
	public ResponseEntity<String> deleteFromDms(String documentId, String authToken, Long orgIdForLog) {
		try {
			String dmsDeleteUrl = webConstants.getDmsServiceUrl() + "/dms/files/org/" + documentId;

			Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);

			return coreRestCall(dmsDeleteUrl, null, headers, HttpMethod.DELETE, orgIdForLog);
		} catch (Exception e) {
			log.error("Error deleting from DMS: {}", e.getMessage(), e);
			return new ResponseEntity<>("Error deleting from DMS: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}