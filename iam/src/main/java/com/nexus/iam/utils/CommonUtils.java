package com.nexus.iam.utils;

import com.nexus.iam.dto.LoginRequest;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.security.JwtUtil;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.service.KeycloakAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonUtils {

    private final JwtUtil jwtUtil;
    private final ObjectProvider<AuthenticationService> authenticationServiceProvider;
    private final ObjectMapper objectMapper;
    private final WebConstants webConstants;
    private final ObjectProvider<KeycloakAuthenticationService> keycloakAuthenticationServices;

    public String jsonValidator(String jsonString) {
        if (ObjectUtils.isEmpty(jsonString)) {
            return "{}";
        }
        JsonNode jsonNode = null;
        ObjectMapper objectMapper = new ObjectMapper();
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

    public Map<String, String> buildJsonHeaders(String authToken) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (!ObjectUtils.isEmpty(authToken)) {
            headers.put(HttpHeaders.AUTHORIZATION, authToken);
        } else {
//            AuthenticationService authenticationService = authenticationServiceProvider.getIfAvailable();
            KeycloakAuthenticationService authenticationService = keycloakAuthenticationServices.getIfAvailable();
            if (authenticationService != null) {
                ResponseEntity<LoginResponse> loginResponse = authenticationService
                        .login(webConstants.getGenericUserId(),
                                webConstants.getGenericPassword());
                if (loginResponse.getStatusCode().is2xxSuccessful() && loginResponse.getBody() != null) {
                    headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + loginResponse.getBody().getAccessToken());
                }else{
                    log.error("Failed to obtain generic user token. Status: {}, Body: {}", loginResponse.getStatusCode(), loginResponse.getBody());
                }
            }
        }

        return headers;
    }
}
