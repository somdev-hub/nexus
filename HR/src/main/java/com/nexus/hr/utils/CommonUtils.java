package com.nexus.hr.utils;

import com.nexus.hr.payload.RestPayload;
import com.nexus.hr.payload.TokenPayloadDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommonUtils {

    private final WebConstants webConstants;
    private String token;

    public CommonUtils(WebConstants webConstants) {
        this.webConstants = webConstants;
    }

    public boolean validateToken(String token) {
        String authUrl = webConstants.getVerifyTokenUrl();
        try {
            Map<String, String> body = Map.of("token", token.substring(7));
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract isValid from response
            Map<String, String> responseBody = response.getBody();
            return !response.getStatusCode().is2xxSuccessful() ||
                    ObjectUtils.isEmpty(responseBody) ||
                    !Boolean.parseBoolean(responseBody.get("isValid"));

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public String getToken() {
        if (this.token == null || !validateToken(this.token)) {
            this.token = generateToken();
        }
        return this.token;
    }

    public String generateToken() {
        String authUrl = webConstants.getGenerateTokenUrl();
        Map<String, String> body = new HashMap<>();
        body.put("email", webConstants.getGenericUserId());
        body.put("password", webConstants.getGenericPassword());
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<Map<String, String>> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, String>>() {
                    });
            // extract token from response
            Map<String, String> responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null && responseBody.containsKey("accessToken")) {
                return "Bearer " + responseBody.get("accessToken");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public TokenPayloadDto decryptToken(String token) {
        String authUrl = webConstants.getDecryptTokenUrl();
        Map<String, String> body = Map.of("token", token.substring(7));
        try {
            RestClient restClient = RestClient.create();
            ResponseEntity<TokenPayloadDto> response = restClient.post().uri(authUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(TokenPayloadDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String jsonValidator(String jsonString) {
        if (ObjectUtils.isEmpty(jsonString)) {
            return "{}";
        }
        JsonNode jsonNode = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (JacksonException _) {
            jsonNode = objectMapper.createObjectNode().put("message", jsonString);
        }
        return objectMapper.writeValueAsString(jsonNode);
    }

    public RestPayload buildRestPayload(String url, Map<String, String> queriesParams, Map<Integer, String> pathVariables, String headerType) {
        RestPayload restPayload = new RestPayload();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        // Append path variables (sorted by index) to the URL
        if (!ObjectUtils.isEmpty(pathVariables)) {
            pathVariables.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> builder.pathSegment(entry.getValue()));
        }

        // Append query parameters
        if (!ObjectUtils.isEmpty(queriesParams)) {
            queriesParams.forEach(builder::queryParam);
        }

        // Build headers
        Map<String, String> headers = new HashMap<>();
        if (!ObjectUtils.isEmpty(headerType)) {
            headers.put(CommonConstants.AUTHORIZATION, getToken());
            if (headerType.equalsIgnoreCase("json")) {
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);
            } else if (headerType.equalsIgnoreCase("multipart")) {
                headers.put(CommonConstants.CONTENT_TYPE, CommonConstants.MULTIPART_FORM_DATA);
            }
            // Add other header types if needed
        }

        restPayload.setBuilder(builder);
        restPayload.setHeaders(headers);

        return restPayload;

    }
}
