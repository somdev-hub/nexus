package com.nexus.iam.service;

import java.util.Set;

import com.nexus.iam.dto.KeycloakUserDto;

/**
 * Service for synchronizing Keycloak user data to IAM database
 * Implements lazy-load strategy: syncs user data on first login/API call
 */
public interface KeycloakUserSyncService {

    /**
     * Sync or create user in IAM database from Keycloak data
     * Called on first login/API call to lazy-load user data
     * 
     * @param keycloakUserDto the user data from Keycloak token claims
     * @param roles the set of roles from JWT claims
     * @return the synced/created user ID in IAM database
     */
    Long syncUserFromKeycloak(KeycloakUserDto keycloakUserDto, Set<String> roles);

    /**
     * Extract user data from JWT token claims
     * 
     * @param token the JWT token
     * @return KeycloakUserDto with extracted claims
     */
    KeycloakUserDto extractUserFromToken(String token);

    /**
     * Extract roles from JWT token claims
     * 
     * @param token the JWT token
     * @return Set of role names
     */
    Set<String> extractRolesFromToken(String token);
}

