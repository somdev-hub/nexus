package com.nexus.iam.service;

import com.nexus.iam.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keycloak Authentication Service Interface
 * 
 * Handles all Keycloak-specific authentication operations:
 * - Direct Access Grant login (email/password)
 * - User registration
 * - Token refresh
 * - Token validation and user sync
 */
public interface KeycloakAuthenticationService {

    /**
     * Authenticate user with Keycloak using Direct Access Grant
     * 
     * @param email    User email (username in Keycloak)
     * @param password User password
     * @return LoginResponse with tokens and user data
     */
    ResponseEntity<LoginResponse> login(String email, String password);

    /**
     * Register new user in Keycloak (without profile photo)
     * 
     * @param userRegisterDto User registration data
     * @return LoginResponse or success message
     */
    ResponseEntity<LoginResponse> register(UserRegisterDto userRegisterDto);

    /**
     * Register new user in Keycloak (with optional profile photo)
     * 
     * @param userRegisterDto User registration data
     * @param profilePhoto    Optional profile picture file
     * @return LoginResponse with authenticated tokens and user data
     */
    ResponseEntity<LoginResponse> register(UserRegisterDto userRegisterDto, MultipartFile profilePhoto);

    /**
     * Refresh access token using refresh token
     * 
     * @param refreshToken Keycloak refresh token
     * @return New tokens
     */
    ResponseEntity<LoginResponse> refreshToken(String refreshToken);

    /**
     * Authenticate with Keycloak using Direct Access Grant
     * Internal method to exchange credentials for tokens
     * 
     * @param username Email (Keycloak username)
     * @param password User password
     * @return Token response from Keycloak or null if failed
     */
    java.util.Map<String, Object> authenticateWithKeycloak(String username, String password);

    /**
     * Validate JWT token and sync user to database
     * 
     * @param accessToken  Keycloak access token
     * @param refreshToken Keycloak refresh token
     * @param expiresIn    Token expiration in seconds
     * @return LoginResponse with synced user data
     */
    ResponseEntity<?> validateAndSyncUser(String accessToken, String refreshToken, Long expiresIn);

    /**
     * Verify Keycloak JWT token validity and extract claims
     * 
     * @param token Keycloak JWT token
     * @return Token claims and validity information
     */
    java.util.Map<String, String> verifyToken(String token);

    /**
     * Decrypt/Extract claims from Keycloak JWT token
     * 
     * @param token Keycloak JWT token
     * @return All token claims
     */
    java.util.Map<String, Object> decryptToken(String token);

    ResponseEntity<?> registerApplicant(ApplicantRegisterDto userRegisterDto, MultipartFile profilePicture);

    ResponseEntity<?> loginApplicant(LoginRequest.ApplicantLoginRequest request);

    ResponseEntity<?> refreshApplicantToken(RefreshTokenRequest request);
}
