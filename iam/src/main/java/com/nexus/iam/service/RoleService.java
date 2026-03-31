package com.nexus.iam.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service interface for Role management with Keycloak integration
 */
@Service
public interface RoleService {
    
    /**
     * Initialize roles from Keycloak on startup
     */
    void initializeRoles();

    /**
     * Create a role if it doesn't exist (with department assignment)
     * @param roleName the name of the role
     * @param deptId the department ID to assign the role to
     * @param authHeader the authorization header for validation
     */
    ResponseEntity<?> createRoleIfNotFound(String roleName, Long deptId, String authHeader);

    /**
     * Delete a role by name
     * @param roleName the name of the role
     */
    void deleteRoleByName(String roleName);

    /**
     * Sync keycloak_id for all roles in IAM database
     * Fetches keycloak_id from Keycloak for roles that have null keycloak_id
     */
    void syncKeycloakIds();
}





