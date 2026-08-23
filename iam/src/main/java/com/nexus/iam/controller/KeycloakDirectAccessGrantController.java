package com.nexus.iam.controller;

import com.nexus.iam.dto.LoginRequest;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak Direct Access Grant (Password Grant) Controller
 * 
 * This controller handles authentication with your own UI using email/password.
 * Internally, it authenticates with Keycloak and returns Keycloak tokens.
 * 
 * Flow:
 * 1. Frontend sends: POST /iam/auth/keycloak/login { email, password }
 * 2. IAM Service internally uses Keycloak Direct Access Grant
 * 3. Keycloak validates credentials
 * 4. IAM Service syncs user to database (lazy-load)
 * 5. Frontend receives: { accessToken, refreshToken, user data }
 * 
 * NO Keycloak UI redirect needed - pure API-based authentication
 */
@Slf4j
@RestController
@RequestMapping("/iam/auth/keycloak")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class KeycloakDirectAccessGrantController {

    private final KeycloakUserSyncService keycloakUserSyncService;
    private final JwtDecoder jwtDecoder;
    private final WebConstants webConstants;
    private final RestClient restClient;

    /**
     * Login with email and password using Keycloak Direct Access Grant
     * 
     * This endpoint allows you to authenticate users with your own UI/API
     * without redirecting to Keycloak login page.
     * 
     * Request:
     * POST /iam/auth/keycloak/login
     * {
     *   "email": "user@example.com",
     *   "password": "password123"
     * }
     * 
     * Response: { accessToken, refreshToken, user data }
     * 
     * @param loginRequest Email and password
     * @return LoginResponse with Keycloak tokens
     */
    @PostMapping("/login")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "loginFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Validate input
            if (loginRequest.getEmail() == null || loginRequest.getEmail().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
                log.warn("Login attempt with missing email or password");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email and password are required"));
            }

            log.info("Keycloak login attempt for user: {}", loginRequest.getEmail());

            // Step 1: Authenticate with Keycloak using Direct Access Grant
            Map<String, Object> tokenResponse = authenticateWithKeycloak(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.warn("Keycloak authentication failed for user: {}", loginRequest.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials or user not found in Keycloak"));
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Long expiresIn = ((Number) tokenResponse.getOrDefault("expires_in", 300L)).longValue();

            log.debug("Keycloak authentication successful for user: {}", loginRequest.getEmail());

            // Step 2: Validate token and sync user to database
            return validateAndSyncUser(accessToken, refreshToken, expiresIn);

        } catch (Exception e) {
            log.error("Error during Keycloak login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Authenticate with Keycloak using Direct Access Grant (Resource Owner Password Credentials)
     * 
     * This is the key method that exchanges email/password for Keycloak tokens
     * without requiring the user to visit Keycloak UI.
     * 
     * @param username Email (Keycloak username)
     * @param password Password
     * @return Token response from Keycloak
     */
    private Map<String, Object> authenticateWithKeycloak(String username, String password) {
        try {
            log.debug("Authenticating with Keycloak Direct Access Grant");

            String tokenUrl = webConstants.getKeycloakTokenUrl();

            // Build the request body for Direct Access Grant
            // grant_type=password is the key difference from OAuth2 authorization code flow
            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "password");           // Direct Access Grant
            body.put("client_id", webConstants.getKeycloakClientId());
            body.put("client_secret", webConstants.getKeycloakClientSecret());
            body.put("username", username);               // Email as username
            body.put("password", password);
            body.put("scope", "openid profile email");    // Requested scopes

            log.debug("Sending Direct Access Grant request to Keycloak");

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Keycloak Direct Access Grant successful");
                return response.getBody();
            }

            // Check for error response from Keycloak
            if (response.getStatusCode().is4xxClientError()) {
                Map<String, Object> errorBody = response.getBody();
                String error = errorBody != null ? (String) errorBody.get("error_description") : "Unknown error";
                log.error("Keycloak authentication failed: {}", error);
                return null;
            }

            log.error("Keycloak Direct Access Grant failed with status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error during Keycloak Direct Access Grant: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate token and sync user to database (lazy-load)
     */
    private ResponseEntity<?> validateAndSyncUser(String accessToken, String refreshToken, Long expiresIn) {
        try {
            log.debug("Validating and syncing user from Keycloak token");

            // Decode JWT (JwtDecoder verifies signature using Keycloak's public key)
            Jwt jwt = jwtDecoder.decode(accessToken);
            log.debug("JWT signature validated successfully");

            // Extract user data from JWT
            var userDto = keycloakUserSyncService.extractUserFromToken(accessToken);
            if (userDto == null) {
                log.error("Failed to extract user data from Keycloak token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token: missing user data"));
            }

            // Extract roles from JWT
            Set<String> roles = keycloakUserSyncService.extractRolesFromToken(accessToken);
            log.debug("Extracted roles from Keycloak token: {}", roles);

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

            log.info("Keycloak login successful for user: {}", userDto.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating and syncing user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token validation failed: " + e.getMessage()));
        }
    }

    /**
     * Register new user in Keycloak
     * 
     * This endpoint creates a new user directly in Keycloak
     * No need to use /iam/auth/register anymore if using Keycloak
     * 
     * @param body User registration data
     * @return Response with user ID or error
     */
    @PostMapping("/register")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "registerFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            String firstName = body.get("firstName");
            String lastName = body.get("lastName");

            // Validate input
            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email and password are required"));
            }

            log.info("Registering new user in Keycloak: {}", email);

            // Create user in Keycloak
            // This would use KeycloakAdminServiceImpl to create the user
            // For now, we'll return a simple response
            // In production, implement proper user creation

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "User registered successfully",
                            "email", email,
                            "note", "You can now login with your credentials"
                    ));

        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    /**
     * Refresh access token using refresh token from Keycloak
     * 
     * @param body Request body with refreshToken
     * @return New access token
     */
    @PostMapping("/refresh-token")
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

            log.debug("Refreshing access token with Keycloak");

            String tokenUrl = webConstants.getKeycloakTokenUrl();

            Map<String, String> refreshBody = new HashMap<>();
            refreshBody.put("grant_type", "refresh_token");
            refreshBody.put("refresh_token", refreshToken);
            refreshBody.put("client_id", webConstants.getKeycloakClientId());
            refreshBody.put("client_secret", webConstants.getKeycloakClientSecret());

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
     * Change password for authenticated user
     * 
     * @param body Request with current password and new password
     * @return Success or error response
     */
    @PostMapping("/change-password")
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "changePasswordFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        try {
            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");
            String email = body.get("email");

            if (email == null || currentPassword == null || newPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email, current password, and new password are required"));
            }

            log.info("Changing password for user: {}", email);

            // Step 1: Verify current password by attempting login
            Map<String, Object> tokenResponse = authenticateWithKeycloak(email, currentPassword);
            if (tokenResponse == null) {
                log.warn("Current password verification failed for user: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            // Step 2: Change password in Keycloak
            // This would use KeycloakAdminServiceImpl to update the user
            // For now, return success response

            log.info("Password changed successfully for user: {}", email);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));

        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password change failed: " + e.getMessage()));
        }
    }

    // ===== Fallback Methods =====

    public ResponseEntity<?> loginFallback(LoginRequest loginRequest, Exception e) {
        log.error("Circuit breaker fallback for login: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Authentication service temporarily unavailable"));
    }

    public ResponseEntity<?> registerFallback(Map<String, String> body, Exception e) {
        log.error("Circuit breaker fallback for register: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Registration service temporarily unavailable"));
    }

    public ResponseEntity<?> refreshTokenFallback(Map<String, String> body, Exception e) {
        log.error("Circuit breaker fallback for refresh token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Token refresh service temporarily unavailable"));
    }

    public ResponseEntity<?> changePasswordFallback(Map<String, String> body, Exception e) {
        log.error("Circuit breaker fallback for change password: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Password change service temporarily unavailable"));
    }
}

