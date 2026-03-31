package com.nexus.iam.controller;

import com.nexus.iam.dto.KeycloakUserDto;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.service.KeycloakUserSyncService;
import com.nexus.iam.utils.WebConstants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 Controller for Keycloak Integration (Phase 2)
 * 
 * This controller handles OAuth2 authentication flow with Keycloak.
 * 
 * Endpoints:
 * - POST /iam/auth/oauth2/callback - Handle authorization code from Keycloak
 * - POST /iam/auth/oauth2/validate-and-sync - Validate Keycloak token and sync user
 * - GET /iam/auth/oauth2/logout - Logout from Keycloak
 */
@Slf4j
@RestController
@RequestMapping("/iam/auth/oauth2")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final KeycloakUserSyncService keycloakUserSyncService;
    private final JwtDecoder jwtDecoder;
    private final WebConstants webConstants;
    private final RestClient restClient;

    /**
     * Handle OAuth2 callback from Keycloak
     * Frontend redirects here after user authenticates with Keycloak
     * 
     * @param code Authorization code from Keycloak
     * @param state State parameter (CSRF protection)
     * @return LoginResponse with tokens and user data
     */
    @PostMapping("/callback")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "callbackFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        
        try {
            log.info("OAuth2 callback received with code: {}", code.substring(0, 10) + "...");

            // Step 1: Exchange authorization code for tokens with Keycloak
            Map<String, Object> tokenResponse = exchangeCodeForToken(code);
            
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to get access token from Keycloak");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Failed to exchange authorization code"));
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Long expiresIn = ((Number) tokenResponse.getOrDefault("expires_in", 300L)).longValue();

            log.debug("Successfully exchanged code for tokens");

            // Step 2: Validate and sync user
            return validateAndSyncUser(accessToken, refreshToken, expiresIn);

        } catch (Exception e) {
            log.error("Error in OAuth2 callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "OAuth2 authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Exchange authorization code for tokens with Keycloak
     * 
     * @param code Authorization code
     * @return Token response from Keycloak
     */
    private Map<String, Object> exchangeCodeForToken(String code) {
        try {
            log.debug("Exchanging authorization code with Keycloak");

            String tokenUrl = webConstants.getKeycloakTokenUrl();
            
            Map<String, String> body = Map.of(
                    "grant_type", "authorization_code",
                    "code", code,
                    "client_id", webConstants.getKeycloakClientId(),
                    "client_secret", webConstants.getKeycloakClientSecret(),
                    "redirect_uri", "http://localhost:3000/auth/callback"  // Frontend callback URL
            );

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully obtained tokens from Keycloak");
                return response.getBody();
            }

            log.error("Keycloak token exchange failed with status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error exchanging authorization code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to exchange authorization code", e);
        }
    }

    /**
     * Validate Keycloak JWT token and sync user to IAM database (lazy-load)
     * 
     * @param authHeader Authorization header with Bearer token
     * @return LoginResponse with synced user data
     */
    @PostMapping("/validate-and-sync")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "validateAndSyncFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> validateAndSync(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid authorization header format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid authorization header"));
            }

            String token = authHeader.substring(7); // Remove "Bearer "

            // Validate JWT (JwtDecoder will verify signature and expiration using Keycloak's public key)
            Jwt jwt = jwtDecoder.decode(token);
            log.debug("JWT signature validated successfully");

            // Extract tokens (might be passed in request body instead)
            return validateAndSyncUser(token, null, getTokenExpirySeconds(jwt));

        } catch (Exception e) {
            log.error("Error validating and syncing token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token validation failed: " + e.getMessage()));
        }
    }

    /**
     * Internal method to validate token and sync user
     */
    private ResponseEntity<?> validateAndSyncUser(String accessToken, String refreshToken, Long expiresIn) {
        try {
            // Decode JWT without verifying signature (already verified above or by JwtDecoder)
            Jwt jwt = jwtDecoder.decode(accessToken);

            // Extract user data from JWT
            KeycloakUserDto userDto = keycloakUserSyncService.extractUserFromToken(accessToken);
            
            if (userDto == null) {
                log.error("Failed to extract user data from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token: missing user data"));
            }

            // Extract roles from JWT
            Set<String> roles = keycloakUserSyncService.extractRolesFromToken(accessToken);
            log.debug("Extracted roles from token: {}", roles);

            // Sync user to IAM database (lazy-load on first login)
            Long userId = keycloakUserSyncService.syncUserFromKeycloak(userDto, roles);
            log.info("User synced to IAM database: userId={}, email={}", userId, userDto.getEmail());

            // Build response
            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken != null ? refreshToken : accessToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .email(userDto.getEmail())
                    .name(userDto.getFirstName() + " " + userDto.getLastName())
                    .userId(userId)
                    .orgId(null) // Will be loaded from database if available
                    .role(roles.stream().findFirst().map(r -> "ROLE_" + r).orElse("ROLE_USER"))
                    .build();

            log.info("Login successful for user: {}", userDto.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in validate and sync: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token validation failed: " + e.getMessage()));
        }
    }

    /**
     * Logout from Keycloak
     * 
     * @param refreshToken Refresh token to revoke
     * @return Success response
     */
    @PostMapping("/logout")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "logoutFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("Logout called without refresh token");
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }

            log.debug("Revoking refresh token with Keycloak");

            String tokenUrl = webConstants.getKeycloakTokenUrl();
            String revokeUrl = tokenUrl.replace("/token", "/revoke");

            Map<String, String> revokeBody = Map.of(
                    "client_id", webConstants.getKeycloakClientId(),
                    "client_secret", webConstants.getKeycloakClientSecret(),
                    "token", refreshToken,
                    "token_type_hint", "refresh_token"
            );

            ResponseEntity<Void> response = restClient.post()
                    .uri(revokeUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(revokeBody)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Token revoked successfully");
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }

            log.error("Token revocation failed");
            return ResponseEntity.ok(Map.of("message", "Logout processed"));

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            // Don't fail logout even if token revocation fails
            return ResponseEntity.ok(Map.of("message", "Logged out (with warnings)"));
        }
    }

    /**
     * Get OAuth2 authorization URL
     * Frontend redirects to this URL to start the OAuth2 flow
     * 
     * @return OAuth2 authorization URL
     */
    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthUrl() {
        try {
            String authorizationUrl = webConstants.getKeycloakServerUrl() 
                    + "/realms/" + webConstants.getKeycloakRealm()
                    + "/protocol/openid-connect/auth"
                    + "?client_id=" + webConstants.getKeycloakClientId()
                    + "&response_type=code"
                    + "&scope=openid%20profile%20email"
                    + "&redirect_uri=http://localhost:3000/auth/callback"
                    + "&state=" + System.currentTimeMillis(); // Simple state for CSRF protection

            return ResponseEntity.ok(Map.of("authUrl", authorizationUrl));

        } catch (Exception e) {
            log.error("Error generating auth URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate auth URL"));
        }
    }

    /**
     * Refresh access token using refresh token
     * 
     * @param body Request body with refreshToken
     * @return New tokens
     */
    @PostMapping("/refresh")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "refreshTokenFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("Token refresh called without refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token required"));
            }

            log.debug("Refreshing access token");

            String tokenUrl = webConstants.getKeycloakTokenUrl();
            
            Map<String, String> refreshBody = Map.of(
                    "grant_type", "refresh_token",
                    "refresh_token", refreshToken,
                    "client_id", webConstants.getKeycloakClientId(),
                    "client_secret", webConstants.getKeycloakClientSecret()
            );

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(refreshBody)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tokens = response.getBody();
                String newAccessToken = (String) tokens.get("access_token");
                Long expiresIn = ((Number) tokens.getOrDefault("expires_in", 300L)).longValue();

                log.info("Token refreshed successfully");
                return ResponseEntity.ok(Map.of(
                        "accessToken", newAccessToken,
                        "refreshToken", refreshToken,
                        "tokenType", "Bearer",
                        "expiresIn", expiresIn
                ));
            }

            log.error("Token refresh failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Failed to refresh token"));

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }

    /**
     * Get token expiry time in seconds
     */
    private long getTokenExpirySeconds(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return 300; // Default 5 minutes
        }
        long expiresAtMillis = expiresAt.toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        return (expiresAtMillis - nowMillis) / 1000;
    }

    // ===== Fallback Methods =====

    public ResponseEntity<?> callbackFallback(String code, String state, Exception e) {
        log.error("Circuit breaker fallback for callback: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Keycloak service temporarily unavailable"));
    }

    public ResponseEntity<?> validateAndSyncFallback(String authHeader, Exception e) {
        log.error("Circuit breaker fallback for validate-and-sync: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Authentication service temporarily unavailable"));
    }

    public ResponseEntity<?> logoutFallback(Map<String, String> body, Exception e) {
        log.error("Circuit breaker fallback for logout: {}", e.getMessage());
        return ResponseEntity.ok(Map.of("message", "Logout processed"));
    }

    public ResponseEntity<?> refreshTokenFallback(Map<String, String> body, Exception e) {
        log.error("Circuit breaker fallback for refresh: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Token refresh service temporarily unavailable"));
    }
}

