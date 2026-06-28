package com.nexus.iam.controller;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.dto.*;
import com.nexus.iam.service.AuthenticationService;
import com.nexus.iam.service.KeycloakAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import java.util.Map;

/**
 * Unified Authentication Controller
 * <p>
 * This controller provides a unified authentication interface that works with
 * both:
 * 1. Phase 1: Traditional JWT (AuthenticationService)
 * 2. Phase 2: Keycloak Direct Access Grant (KeycloakAuthenticationService)
 * <p>
 * The authentication method is determined by configuration
 * (keycloak.oauth2.enabled).
 * Routing is done based on the configuration value - no frontend changes
 * needed!
 * <p>
 * Frontend sees the same API regardless of which backend is used:
 * - POST /iam/auth/login → Same request/response format
 * - POST /iam/auth/register → Same request/response format
 * - POST /iam/auth/refresh → Same request/response format
 */
@Slf4j
@RestController
@RequestMapping("/iam/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UnifiedAuthController {

    private final AuthenticationService authenticationService;
    private final KeycloakAuthenticationService keycloakAuthenticationService;

    @Value("${keycloak.oauth2.enabled:false}")
    private boolean keycloakEnabled;

    /**
     * Unified Login Endpoint
     * <p>
     * Routes to either Phase 1 (Traditional JWT) or Phase 2 (Keycloak Direct Access
     * Grant)
     * based on configuration.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (keycloakEnabled) {
            log.debug("Using Keycloak authentication (Phase 2)");
            return keycloakAuthenticationService.login(loginRequest.getEmail(), loginRequest.getPassword());
        } else {
            log.debug("Using traditional JWT authentication (Phase 1)");
            try {
                LoginResponse response = authenticationService.authenticate(loginRequest);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Traditional authentication failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }
        }
    }

    /**
     * Unified Register Endpoint (Multipart Form Data)
     * <p>
     * Routes to either Phase 1 (Traditional) or Phase 2 (Keycloak)
     * based on configuration.
     * <p>
     * Handles multipart/form-data requests with optional profile picture upload.
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerFromForm(
            @RequestPart(name = "dto", required = true) UserRegisterDto userRegisterDto,
            @RequestPart(name = "profilePicture", required = false) MultipartFile profilePhoto) {

        if (keycloakEnabled) {
            log.debug("Using Keycloak registration (Phase 2)");
            return keycloakAuthenticationService.register(userRegisterDto, profilePhoto);
        } else {
            log.debug("Using traditional registration (Phase 1)");
            try {
                LoginResponse response = authenticationService.registerUser(userRegisterDto, profilePhoto);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } catch (Exception e) {
                log.error("Traditional registration failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Registration failed: " + e.getMessage()));
            }
        }
    }

    /**
     * Unified Register Endpoint (JSON)
     * <p>
     * Routes to either Phase 1 (Traditional) or Phase 2 (Keycloak)
     * based on configuration.
     * <p>
     * Handles application/json requests without profile picture upload.
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerFromJson(
            @RequestBody UserRegisterDto userRegisterDto) {

        if (keycloakEnabled) {
            log.debug("Using Keycloak registration (Phase 2)");
            return keycloakAuthenticationService.register(userRegisterDto);
        } else {
            log.debug("Using traditional registration (Phase 1)");
            try {
                LoginResponse response = authenticationService.registerUser(userRegisterDto, null);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } catch (Exception e) {
                log.error("Traditional registration failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Registration failed: " + e.getMessage()));
            }
        }
    }

    /**
     * Unified Refresh Token Endpoint
     * <p>
     * Routes to either Phase 1 (Traditional) or Phase 2 (Keycloak)
     * based on configuration.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        if (keycloakEnabled) {
            log.debug("Using Keycloak token refresh (Phase 2)");
            return keycloakAuthenticationService.refreshToken(refreshTokenRequest.getRefreshToken());
        } else {
            log.debug("Using traditional token refresh (Phase 1)");
            try {
                LoginResponse response = authenticationService.refreshToken(refreshTokenRequest);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Traditional token refresh failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token refresh failed"));
            }
        }
    }

    /**
     * Verify Token Endpoint
     * 
     * Verifies the validity of a JWT token and extracts its claims.
     * Routes to Phase 1 (Traditional JWT) or Phase 2 (Keycloak) based on
     * configuration.
     */
    @LogActivity("Token Verification Attempt")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> token) {
        if (ObjectUtils.isEmpty(token) || !token.containsKey("token")) {
            log.warn("Verify token attempt with missing token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Token is required"));
        }

        try {
            log.debug("Verifying token");
            if (keycloakEnabled) {
                log.debug("Using Keycloak token verification (Phase 2)");
                Map<String, String> result = keycloakAuthenticationService.verifyToken(token.get("token"));
                return ResponseEntity.ok(result);
            } else {
                log.debug("Using traditional token verification (Phase 1)");
                Map<String, String> result = authenticationService.verifyToken(token.get("token"));
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token verification failed: " + e.getMessage()));
        }
    }

    /**
     * Decrypt Token Endpoint
     * 
     * Decrypts and extracts claims from a JWT token.
     * Routes to Phase 1 (Traditional JWT) or Phase 2 (Keycloak) based on
     * configuration.
     */
    @LogActivity("Token Decryption Attempt")
    @PostMapping("/decrypt")
    public ResponseEntity<?> decryptToken(@RequestBody DecryptTokenRequest request) {
        if (ObjectUtils.isEmpty(request) || ObjectUtils.isEmpty(request.getToken())) {
            log.warn("Decrypt token attempt with missing token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Token is required"));
        }

        try {
            log.debug("Decrypting token");
            if (keycloakEnabled) {
                log.debug("Using Keycloak token decryption (Phase 2)");
                Map<String, Object> decryptedToken = keycloakAuthenticationService.decryptToken(request.getToken());
                return ResponseEntity.ok(decryptedToken);
            } else {
                log.debug("Using traditional token decryption (Phase 1)");
                Map<String, Object> decryptedToken = authenticationService.decryptToken(request.getToken());
                return ResponseEntity.ok(decryptedToken);
            }
        } catch (Exception e) {
            log.error("Token decryption failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token decryption failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/register/applicant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerApplicant(@RequestPart(name = "dto") ApplicantRegisterDto userRegisterDto, @RequestPart(name = "profilePicture", required = false) MultipartFile profilePicture){
        return keycloakAuthenticationService.registerApplicant(userRegisterDto, profilePicture);
    }
}
