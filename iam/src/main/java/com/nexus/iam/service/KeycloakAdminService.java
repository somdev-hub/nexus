package com.nexus.iam.service;

import com.nexus.iam.dto.KeycloakRoleDto;
import com.nexus.iam.dto.KeycloakUserDto;

import java.util.List;

/**
 * Service interface for Keycloak Admin API operations.
 * Handles all role and user management operations in Keycloak.
 */
public interface KeycloakAdminService {

    /**
     * Create a new role in Keycloak
     *
     * @param roleName the name of the role to create
     * @return the Keycloak role ID
     */
    String createRoleInKeycloak(String roleName);

    /**
     * Delete a role from Keycloak
     *
     * @param keycloakRoleId the Keycloak role UUID
     */
    void deleteRoleFromKeycloak(String keycloakRoleId);

    /**
     * Get a role by name from Keycloak
     *
     * @param roleName the name of the role
     * @return KeycloakRoleDto containing role details
     */
    KeycloakRoleDto getRoleByName(String roleName);

    /**
     * List all roles in Keycloak realm
     *
     * @return List of all roles in the realm
     */
    List<KeycloakRoleDto> listAllRoles();

    /**
     * Create a new user in Keycloak
     *
     * @param userDto the user data
     * @return the Keycloak user ID
     */
    String createUserInKeycloak(KeycloakUserDto userDto);

    /**
     * Get a user by email from Keycloak
     *
     * @param email the user email
     * @return KeycloakUserDto if found
     */
    KeycloakUserDto getUserByEmail(String email);

    /**
     * Assign a role to a user in Keycloak
     *
     * @param keycloakUserId the Keycloak user UUID
     * @param keycloakRoleId the Keycloak role UUID
     */
    void assignRoleToUser(String keycloakUserId, String keycloakRoleId, String roleName);

    /**
     * Remove a role from a user in Keycloak
     *
     * @param keycloakUserId the Keycloak user UUID
     * @param keycloakRoleId the Keycloak role UUID
     */
    void removeRoleFromUser(String keycloakUserId, String keycloakRoleId);

    /**
     * Set user password in Keycloak
     *
     * @param keycloakUserId the Keycloak user UUID
     * @param password       the new password
     */
    void setUserPassword(String keycloakUserId, String password);

    /**
     * Get admin access token from Keycloak using client credentials
     *
     * @return the access token
     */
    String getAdminToken();

    /**
     * Check if a role exists in Keycloak
     *
     * @param roleName the role name
     * @return true if role exists, false otherwise
     */
    boolean roleExists(String roleName);
}
