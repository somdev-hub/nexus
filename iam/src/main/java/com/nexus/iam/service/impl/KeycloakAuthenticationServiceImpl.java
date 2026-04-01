package com.nexus.iam.service.impl;

import com.nexus.iam.dto.KeycloakRoleDto;
import com.nexus.iam.dto.KeycloakUserDto;
import com.nexus.iam.dto.LoginResponse;
import com.nexus.iam.dto.UserRegisterDto;
import com.nexus.iam.entities.Department;
import com.nexus.iam.entities.Organization;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.exception.UnauthorizedException;
import com.nexus.iam.repository.DepartmentRepository;
import com.nexus.iam.repository.OrganizationRepository;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.KeycloakAdminService;
import com.nexus.iam.service.KeycloakAuthenticationService;
import com.nexus.iam.service.KeycloakUserSyncService;
import com.nexus.iam.utils.CommonConstants;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak Authentication Service Implementation
 * 
 * Handles all Keycloak-specific authentication operations including:
 * - Direct Access Grant (Resource Owner Password Credentials) flow
 * - User registration in Keycloak
 * - Token refresh
 * - User data extraction and sync to IAM database
 * 
 * Features:
 * - Circuit breaker for fault tolerance
 * - Retry logic for transient failures
 * - Comprehensive logging
 * - User lazy-load synchronization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAuthenticationServiceImpl implements KeycloakAuthenticationService {

    private final KeycloakUserSyncService keycloakUserSyncService;
    private final KeycloakAdminService keycloakAdminService;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JwtDecoder jwtDecoder;
    private final WebConstants webConstants;
    private final RestService restService;
    private final ModelMapper modelMapper;
    private final RestClient restClient;

    /**
     * Authenticate user with Keycloak using Direct Access Grant
     * 
     * This endpoint allows your frontend to authenticate directly with your API
     * without redirecting to Keycloak UI.
     * 
     * @param email    User email (username in Keycloak)
     * @param password User password
     * @return LoginResponse with Keycloak tokens and user data
     */
    @Override
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "loginFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<LoginResponse> login(String email, String password) {
        try {
            // Validate input
            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                log.warn("Login attempt with missing email or password");
                throw new UnauthorizedException("Unauthorized", "Email and password are required");
            }

            log.info("Keycloak login attempt for user: {}", email);

            // Step 1: Authenticate with Keycloak using Direct Access Grant
            Map<String, Object> tokenResponse = authenticateWithKeycloak(email, password);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.warn("Keycloak authentication failed for user: {}", email);
                throw new UnauthorizedException("Unauthorized", "Invalid credentials or user not found in Keycloak");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Long expiresIn = ((Number) tokenResponse.getOrDefault("expires_in", 300L)).longValue();

            log.debug("Keycloak authentication successful for user: {}", email);

            // Step 2: Validate token and sync user to database
            return validateAndSyncUser(accessToken, refreshToken, expiresIn);

        } catch (Exception e) {
            log.error("Error during Keycloak login: {}", e.getMessage(), e);
            throw new UnauthorizedException("Unauthorized", "Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Register new user in Keycloak and sync to IAM database
     * 
     * This comprehensive registration process:
     * 1. Validates user registration data
     * 2. Creates user in Keycloak using Keycloak Admin API
     * 3. Creates organization and user in IAM database
     * 4. Handles roles (creates if doesn't exist, assigns to user in both Keycloak
     * and DB)
     * 5. Creates department and links to user
     * 6. Uploads profile photo to DMS microservice
     * 7. Initializes HR employee record in HR microservice
     * 8. Automatically logs in the user and returns Keycloak tokens
     * 
     * @param userRegisterDto User registration data including email, password,
     *                        name, organization, etc.
     * @param profilePhoto    Optional profile picture MultipartFile
     * @return LoginResponse with authenticated tokens and user data
     */
    @Override
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "registerFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<LoginResponse> register(UserRegisterDto userRegisterDto, MultipartFile profilePhoto) {
        try {
            // Step 1: VALIDATION
            if (ObjectUtils.isEmpty(userRegisterDto)) {
                throw new UnauthorizedException("Unauthorized", "User registration data cannot be null or empty");
            }

            if (ObjectUtils.isEmpty(userRegisterDto.getName()) ||
                    ObjectUtils.isEmpty(userRegisterDto.getEmail()) ||
                    ObjectUtils.isEmpty(userRegisterDto.getPassword())) {
                throw new UnauthorizedException("Unauthorized", "Name, email, and password are required");
            }

            if (ObjectUtils.isEmpty(userRegisterDto.getOrgType()) ||
                    ObjectUtils.isEmpty(userRegisterDto.getOrgName())) {
                throw new UnauthorizedException("Unauthorized", "Organization Type and Name are required");
            }

            log.info("Starting registration process for user: {}", userRegisterDto.getEmail());

            // Check if email already exists in database
            if (userRepository.existsByEmail(userRegisterDto.getEmail())) {
                log.warn("Registration attempt with existing email in IAM database: {}", userRegisterDto.getEmail());
                throw new UnauthorizedException("Unauthorized", "Email already exists");
            }

            // Step 2: CREATE USER IN KEYCLOAK (Outside transaction to prevent rollback)
            log.debug("Creating user in Keycloak: {}", userRegisterDto.getEmail());
            KeycloakUserDto keycloakUserDto = KeycloakUserDto.builder()
                    .username(userRegisterDto.getEmail())
                    .email(userRegisterDto.getEmail())
                    .firstName(userRegisterDto.getName().split(" ").length > 0 ? userRegisterDto.getName().split(" ")[0]
                            : userRegisterDto.getName())
                    .lastName(userRegisterDto.getName().split(" ").length > 1 ? String.join(" ",
                            java.util.Arrays.copyOfRange(userRegisterDto.getName().split(" "), 1,
                                    userRegisterDto.getName().split(" ").length))
                            : "")
                    .password(userRegisterDto.getPassword())
                    .enabled(true)
                    .emailVerified(false)
                    .build();

            String keycloakUserId;
            try {
                keycloakUserId = keycloakAdminService.createUserInKeycloak(keycloakUserDto);
                log.info("User created in Keycloak with ID: {}", keycloakUserId);
            } catch (RuntimeException runtimeEx) {
                // Check if it's a 409 Conflict (user already exists)
                if (runtimeEx.getCause() instanceof org.springframework.web.client.HttpClientErrorException.Conflict) {
                    log.warn("User already exists in Keycloak: {}", userRegisterDto.getEmail());
                    throw new UnauthorizedException("Unauthorized", "User with this email already exists in Keycloak");
                }
                throw runtimeEx;
            }

            // Step 2b: CREATE DATABASE USER IN TRANSACTION
            return createUserInDatabase(userRegisterDto, profilePhoto, keycloakUserId);

        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw new ServiceLevelException("KeycloakAuthenticationService", e.getLocalizedMessage(), "register",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());
        }
    }

    /**
     * .body(Map.of("error", "Registration failed: " + e.getMessage()));
     * }
     * }
     * 
     * /**
     * Create user in database and related entities (in separate transaction)
     * This allows Keycloak user to persist even if database operations fail
     */
    @Transactional
    private ResponseEntity<LoginResponse> createUserInDatabase(UserRegisterDto userRegisterDto,
            MultipartFile profilePhoto,
            String keycloakUserId) {
        try {
            // Step 3: CREATE ORGANIZATION
            log.debug("Creating organization for user: {}", userRegisterDto.getEmail());
            Organization org = new Organization();
            org.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            org.setOrgName(userRegisterDto.getOrgName());
            org.setOrgType(userRegisterDto.getOrgType());
            org.setTrustScore(0d);
            org = organizationRepository.save(org);
            log.debug("Organization created with ID: {}", org.getId());

            // Step 4: CREATE USER IN DATABASE
            log.debug("Creating user in IAM database");
            User user = modelMapper.map(userRegisterDto, User.class);
            user.setCreatedAt(Timestamp.valueOf(java.time.LocalDateTime.now()));
            user.setOrganization(org);
            user.setKeycloakId(keycloakUserId); // Store Keycloak user ID for sync
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);
            // Note: Password is managed by Keycloak, not stored in database
            user.setPassword("keycloak-managed");

            // Step 5: HANDLE ROLES
            log.debug("Handling user roles");
            String roleName = !ObjectUtils.isEmpty(userRegisterDto.getRole()) ? userRegisterDto.getRole() : "USER";

            // Create role in Keycloak if it doesn't exist
            KeycloakRoleDto keycloakRole = keycloakAdminService.getRoleByName(roleName);
            String keycloakRoleId;
            if (keycloakRole == null) {
                log.debug("Creating role in Keycloak: {}", roleName);
                keycloakRoleId = keycloakAdminService.createRoleInKeycloak(roleName);
            } else {
                keycloakRoleId = keycloakRole.getId();
            }

            // Assign role to user in Keycloak
            log.debug("Assigning role to user in Keycloak");
            keycloakAdminService.assignRoleToUser(keycloakUserId, keycloakRoleId, roleName);

            // Create or get role in database
            Role dbRole;
            if (roleRepository.existsByName(roleName)) {
                dbRole = roleRepository.findByName(roleName).get();
            } else {
                Role newRole = new Role();
                newRole.setName(roleName);
                dbRole = roleRepository.save(newRole);
            }
            user.getRoles().add(dbRole);

            // Save user to database
            user = userRepository.save(user);
            log.info("User created in IAM database with ID: {}", user.getId());

            // Step 6: CREATE DEPARTMENT
            log.debug("Creating department for user");
            Department department = new Department();
            department.setDepartmentName(
                    !ObjectUtils.isEmpty(userRegisterDto.getDepartment()) ? userRegisterDto.getDepartment()
                            : "General");
            department.setDepartmentHead(user);
            department.setOrganization(org);
            department.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            department.getRoles().add(dbRole);
            departmentRepository.save(department);
            log.debug("Department created for user");

            // Step 7: UPLOAD PROFILE PHOTO TO DMS (non-blocking)
            if (!ObjectUtils.isEmpty(profilePhoto)) {
                handleProfilePhotoUpload(user, org, profilePhoto);
            }

            // Step 8: INITIALIZE HR EMPLOYEE RECORD (non-blocking)
            handleHRInitialization(user, org, userRegisterDto);

            // Step 9: AUTO-LOGIN - Authenticate user with Keycloak
            log.debug("Auto-logging in user via Keycloak");
            return login(userRegisterDto.getEmail(), userRegisterDto.getPassword());

        } catch (Exception e) {
            log.error("Error creating user in database: {}", e.getMessage(), e);
            throw new ServiceLevelException("KeycloakAuthenticationService", e.getLocalizedMessage(), "register",
                    new Timestamp(System.currentTimeMillis()), e.getCause().toString(), e.getMessage());
        }
    }

    /**
     * Handle profile photo upload (non-blocking - exceptions don't fail
     * registration)
     */
    private void handleProfilePhotoUpload(User user, Organization org, MultipartFile profilePhoto) {
        log.debug("Uploading profile photo to DMS");
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(webConstants.getCommonDmsUrl());
            Map<String, Object> dmsDto = new HashMap<>();
            dmsDto.put("userId", user.getId());
            dmsDto.put("fileName", user.getId() + "_profile_pic");
            dmsDto.put("remarks", "Profile Photo Upload");
            dmsDto.put("documentType", "PROFILE_IMAGE");
            dmsDto.put("orgId", org.getId());
            dmsDto.put("orgType", org.getOrgType().toString());

            Map<String, Object> docPayload = new HashMap<>();
            docPayload.put("dto", dmsDto);
            docPayload.put("file", profilePhoto);

            Map<String, String> dmsHeaders = new HashMap<>();
            dmsHeaders.put(CommonConstants.CONTENT_TYPE, "multipart/form-data");
            Map<String, Object> authenticateWithKeycloak = authenticateWithKeycloak(webConstants.getGenericUserId(),
                    webConstants.getGenericPassword());
            if (authenticateWithKeycloak == null || !authenticateWithKeycloak.containsKey("access_token")) {
                log.warn("Unable to authenticate generic user for DMS upload");
                return;
            }
            String genericAccessToken = (String) authenticateWithKeycloak.get("access_token");
            dmsHeaders.put(CommonConstants.AUTHORIZATION, "Bearer " + genericAccessToken);

            ResponseEntity<?> dmsResponse = restService.iamRestCall(
                    builder.toUriString(),
                    docPayload,
                    dmsHeaders,
                    HttpMethod.POST,
                    user.getId());

            if (dmsResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, String> respBody = (Map<String, String>) dmsResponse.getBody();
                if (respBody != null && respBody.containsKey("documentUrl")) {
                    user.setProfilePhoto(respBody.get("documentUrl"));
                    userRepository.save(user);
                    log.debug("Profile photo set for user");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to upload profile photo: {}", e.getMessage());
            // Non-blocking - registration continues even if photo upload fails
        }
    }

    /**
     * Handle HR employee initialization (non-blocking - exceptions don't fail
     * registration)
     */
    private void handleHRInitialization(User user, Organization org, UserRegisterDto userRegisterDto) {
        log.debug("Initializing HR employee record");
        try {
            Map<String, Object> hrPayload = new HashMap<>();
            hrPayload.put("employeeId", user.getId());
            hrPayload.put("fullName", user.getName());
            hrPayload.put("email", user.getEmail());
            hrPayload.put("orgId", org.getId());
            hrPayload.put("department",
                    !ObjectUtils.isEmpty(userRegisterDto.getDepartment()) ? userRegisterDto.getDepartment()
                            : "General");
            hrPayload.put("title",
                    !ObjectUtils.isEmpty(userRegisterDto.getTitle()) ? userRegisterDto.getTitle() : "Employee");
            hrPayload.put("remarks", "New User Registration via Keycloak");
            hrPayload.put("timestamp", new Timestamp(System.currentTimeMillis()));
            hrPayload.put("personalEmail",
                    !ObjectUtils.isEmpty(userRegisterDto.getPersonalEmail()) ? userRegisterDto.getPersonalEmail()
                            : userRegisterDto.getEmail());
            hrPayload.put("compensation",
                    !ObjectUtils.isEmpty(userRegisterDto.getCompensation()) ? userRegisterDto.getCompensation() : 0);

            Map<String, String> hrHeaders = new HashMap<>();
            hrHeaders.put(CommonConstants.CONTENT_TYPE, CommonConstants.APPLICATION_JSON);
            Map<String, Object> authenticateWithKeycloak = authenticateWithKeycloak(webConstants.getGenericUserId(),
                    webConstants.getGenericPassword());
            if (authenticateWithKeycloak == null || !authenticateWithKeycloak.containsKey("access_token")) {
                log.warn("Unable to authenticate generic user for HR initialization");
                return;
            }
            String genericAccessToken = (String) authenticateWithKeycloak.get("access_token");
            hrHeaders.put(CommonConstants.AUTHORIZATION, "Bearer " + genericAccessToken);

            UriComponentsBuilder hrBuilder = UriComponentsBuilder.fromUriString(webConstants.getHrInitUrl());
            ResponseEntity<?> hrResponse = restService.iamRestCall(
                    hrBuilder.toUriString(),
                    hrPayload,
                    hrHeaders,
                    HttpMethod.POST,
                    user.getId());

            if (hrResponse.getStatusCode().is2xxSuccessful()) {
                log.debug("HR employee record initialized");
            } else {
                log.warn("HR initialization returned status: {}", hrResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Failed to initialize HR record: {}", e.getMessage());
            // Non-blocking - registration continues even if HR init fails
        }
    }

    /**
     * Register endpoint for JSON requests (without file upload)
     * Routes to the main register method with null profile photo
     * 
     * @param userRegisterDto User registration data
     * @return LoginResponse or error
     */
    @Override
    public ResponseEntity<LoginResponse> register(UserRegisterDto userRegisterDto) {
        return register(userRegisterDto, null);
    }

    /**
     * Refresh access token using refresh token from Keycloak
     * 
     * Returns the same full LoginResponse as the login endpoint with user data,
     * org, and role information.
     * 
     * @param refreshToken Keycloak refresh token
     * @return LoginResponse with new tokens, user data, org, and role
     */
    @Override
    @CircuitBreaker(name = "keycloak-auth", fallbackMethod = "refreshTokenFallback")
    @Retry(name = "keycloak-auth")
    public ResponseEntity<LoginResponse> refreshToken(String refreshToken) {
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("Token refresh called without refresh token");
                throw new UnauthorizedException("Unauthorized", "Refresh token is required");
            }

            log.debug("Refreshing access token with Keycloak");

            String tokenUrl = webConstants.getKeycloakTokenUrl();

            // Build the request body for form-encoded data using MultiValueMap
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);
            body.add("client_id", webConstants.getKeycloakClientId());
            body.add("client_secret", webConstants.getKeycloakClientSecret());

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> tokens = response.getBody();
                String newAccessToken = (String) tokens.get("access_token");
                String newRefreshToken = (String) tokens.getOrDefault("refresh_token", refreshToken);
                Long expiresIn = ((Number) tokens.getOrDefault("expires_in", 300L)).longValue();

                log.info("Token refreshed successfully");

                // Return the same full LoginResponse as login endpoint
                // This includes user data, org, and role information
                return validateAndSyncUser(newAccessToken, newRefreshToken, expiresIn);
            }

            log.error("Token refresh failed with status: {}", response.getStatusCode());
            throw new UnauthorizedException("Unauthorized", "Failed to refresh token");
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new UnauthorizedException("Unauthorized", "Token refresh failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate with Keycloak using Direct Access Grant (Resource Owner Password
     * Credentials)
     * 
     * This is the key method that exchanges email/password for Keycloak tokens
     * without requiring the user to visit Keycloak UI.
     * 
     * Request to Keycloak:
     * - grant_type=password
     * - client_id, client_secret
     * - username (email), password
     * - scope (openid profile email)
     * 
     * @param username Email (Keycloak username)
     * @param password Password
     * @return Token response from Keycloak or null if failed
     */
    @Override
    public Map<String, Object> authenticateWithKeycloak(String username, String password) {
        try {
            log.debug("Authenticating with Keycloak Direct Access Grant for user: {}", username);

            String tokenUrl = webConstants.getKeycloakTokenUrl();

            // Build the request body for Direct Access Grant using MultiValueMap for form
            // encoding
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password"); // Direct Access Grant (Resource Owner Password Credentials)
            body.add("client_id", webConstants.getKeycloakClientId());
            body.add("client_secret", webConstants.getKeycloakClientSecret());
            body.add("username", username); // Email as username
            body.add("password", password);
            body.add("scope", "openid profile email"); // Requested scopes

            log.debug("Sending Direct Access Grant request to Keycloak token endpoint");

            ResponseEntity<Map> response = restClient.post()
                    .uri(tokenUrl)
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Keycloak Direct Access Grant successful for user: {}", username);
                return response.getBody();
            }

            // Check for error response from Keycloak
            if (response.getStatusCode().is4xxClientError()) {
                Map<String, Object> errorBody = response.getBody();
                String error = errorBody != null ? (String) errorBody.get("error_description") : "Unknown error";
                log.error("Keycloak authentication failed for user {}: {}", username, error);
                return null;
            }

            log.error("Keycloak Direct Access Grant failed with status: {} for user: {}",
                    response.getStatusCode(), username);
            return null;

        } catch (Exception e) {
            log.error("Error during Keycloak Direct Access Grant: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validate JWT token and sync user to IAM database (lazy-load pattern)
     * 
     * Process:
     * 1. Decode and validate JWT signature using Keycloak's public key
     * 2. Extract user data from JWT claims
     * 3. Extract roles from JWT
     * 4. Sync user to IAM database (create if new, update if exists)
     * 5. Return LoginResponse with synced user data
     * 
     * @param accessToken  Keycloak access token
     * @param refreshToken Keycloak refresh token
     * @param expiresIn    Token expiration in seconds
     * @return LoginResponse with user data and tokens
     */
    @Override
    public ResponseEntity<LoginResponse> validateAndSyncUser(String accessToken, String refreshToken, Long expiresIn) {
        try {
            log.debug("Validating and syncing user from Keycloak token");

            // log.debug("JWT signature validated successfully");

            // Step 2: Extract user data from JWT claims
            var userDto = keycloakUserSyncService.extractUserFromToken(accessToken);
            if (userDto == null) {
                log.error("Failed to extract user data from Keycloak token");
                // return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                // .body(Map.of("error", "Invalid token: missing user data"));
                throw new UnauthorizedException("Unauthorized", "Invalid token: missing user data");
            }

            // Step 3: Extract roles from JWT
            Set<String> roles = keycloakUserSyncService.extractRolesFromToken(accessToken);
            log.debug("Extracted roles from Keycloak token: {}", roles);

            // Step 4: Sync user to IAM database (lazy-load on first login)
            Long userId = keycloakUserSyncService.syncUserFromKeycloak(userDto, roles);
            log.info("User synced to IAM database: userId={}, email={}", userId, userDto.getEmail());

            // Step 5: Load user and organization from database
            User syncedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found after sync"));
            Long orgId = syncedUser.getOrganization() != null ? syncedUser.getOrganization().getId() : null;

            // Get the primary application role (first non-system role, or ADMIN/USER based
            // on what's in DB)
            String primaryRole = "ROLE_USER";
            if (!syncedUser.getRoles().isEmpty()) {
                primaryRole = syncedUser.getRoles().stream()
                        .map(role -> "ROLE_" + role.getName())
                        .filter(role -> !role.contains("offline") && !role.contains("default-roles")
                                && !role.contains("uma_"))
                        .findFirst()
                        .orElse("ROLE_USER");
            }
            log.debug("User org association: userId={}, orgId={}, primaryRole={}", userId, orgId, primaryRole);

            // Step 6: Build response
            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken != null ? refreshToken : accessToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .email(userDto.getEmail())
                    .name(userDto.getFirstName() + " " + userDto.getLastName())
                    .userId(userId)
                    .orgId(orgId)
                    .role(primaryRole)
                    .build();

            log.info("Keycloak authentication completed successfully for user: {}", userDto.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating and syncing user: {}", e.getMessage(), e);
            throw new UnauthorizedException("Unauthorized", "Token validation failed: " + e.getMessage());
        }
    }

    // ===== Fallback Methods =====

    public ResponseEntity<?> loginFallback(String email, String password, Throwable e) {
        log.error("Circuit breaker fallback for login: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Keycloak authentication service temporarily unavailable"));
    }

    public ResponseEntity<?> registerFallback(UserRegisterDto userRegisterDto, MultipartFile profilePhoto,
            Throwable e) {
        log.error("Circuit breaker fallback for register: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Keycloak registration service temporarily unavailable"));
    }

    public ResponseEntity<?> refreshTokenFallback(String refreshToken, Throwable e) {
        log.error("Circuit breaker fallback for refresh token: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Keycloak token refresh service temporarily unavailable"));
    }

    /**
     * Verify Keycloak JWT token validity and extract claims
     * 
     * @param token Keycloak JWT token
     * @return Token claims and validity information with "isValid" key
     */
    @Override
    public Map<String, String> verifyToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.warn("Verify token called with null/empty token");
                return Map.of("isValid", "false", "error", "Token is required");
            }

            log.debug("Verifying Keycloak token");

            // Decode and validate JWT signature using Keycloak's public key
            Jwt jwt = jwtDecoder.decode(token);
            log.info("Keycloak token verified successfully");

            // Extract key claims
            Map<String, String> result = new HashMap<>();
            result.put("isValid", "true");
            result.put("subject", jwt.getSubject());
            result.put("issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toString() : "N/A");
            result.put("expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : "N/A");
            result.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : "N/A");

            // Add email if available
            String email = jwt.getClaimAsString("email");
            if (email != null) {
                result.put("email", email);
            }

            // Add name if available
            String name = jwt.getClaimAsString("name");
            if (name != null) {
                result.put("name", name);
            }

            log.debug("Token verification successful, claims extracted");
            return result;

        } catch (Exception e) {
            log.error("Error verifying Keycloak token: {}", e.getMessage(), e);
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("isValid", "false");
            errorResult.put("error", "Token verification failed: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Decrypt/Extract all claims from Keycloak JWT token
     * 
     * @param token Keycloak JWT token
     * @return All token claims
     */
    @Override
    public Map<String, Object> decryptToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.warn("Decrypt token called with null/empty token");
                return Map.of("error", "Token is required");
            }

            log.debug("Decrypting Keycloak token");

            // Decode and validate JWT signature
            Jwt jwt = jwtDecoder.decode(token);
            log.info("Keycloak token decrypted successfully");

            // Extract all claims
            Map<String, Object> claims = new HashMap<>();

            // Standard JWT claims
            claims.put("subject", jwt.getSubject());
            claims.put("issuedAt", jwt.getIssuedAt() != null ? jwt.getIssuedAt().toEpochMilli() : null);
            claims.put("expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toEpochMilli() : null);
            claims.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
            claims.put("audience", jwt.getAudience());
            claims.put("notBefore", jwt.getNotBefore() != null ? jwt.getNotBefore().toEpochMilli() : null);

            // Add all additional claims
            jwt.getClaims().forEach((key, value) -> {
                if (!claims.containsKey(key)) {
                    claims.put(key, value);
                }
            });

            log.debug("All token claims extracted successfully");
            return claims;

        } catch (Exception e) {
            log.error("Error decrypting Keycloak token: {}", e.getMessage(), e);
            return Map.of("error", "Token decryption failed: " + e.getMessage());
        }
    }
}
