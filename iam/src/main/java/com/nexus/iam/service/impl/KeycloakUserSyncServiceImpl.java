package com.nexus.iam.service.impl;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.iam.dto.KeycloakUserDto;
import com.nexus.iam.entities.Role;
import com.nexus.iam.entities.User;
import com.nexus.iam.exception.ResourceNotFoundException;
import com.nexus.iam.exception.ServiceLevelException;
import com.nexus.iam.repository.RoleRepository;
import com.nexus.iam.repository.UserRepository;
import com.nexus.iam.service.KeycloakUserSyncService;
import com.nexus.iam.utils.WebConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of Keycloak User Sync Service
 * 
 * Handles lazy-load of user data from Keycloak JWT claims on first login.
 * Also supports extracting user info and roles from JWT tokens for stateless
 * operation.
 */
@Slf4j
@Service
public class KeycloakUserSyncServiceImpl implements KeycloakUserSyncService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private WebConstants webConstants;

    /**
     * Sync/create user in IAM database from Keycloak JWT claims
     * Called on first authenticated API request (lazy-load pattern)
     * 
     * NOTE: For existing users, only updates keycloak_id if missing.
     * Role assignments are NOT updated on subsequent syncs (to avoid duplicate key
     * violations).
     * Roles are only assigned during initial user creation in the registration
     * flow.
     * 
     * @param keycloakUserDto user data extracted from JWT
     * @param roles           set of role names from JWT claims
     * @return IAM database user ID
     */
    @Override
    @Transactional
    public Long syncUserFromKeycloak(KeycloakUserDto keycloakUserDto, Set<String> roles) {
        try {
            // Step 1: Check if user exists in IAM database by email
            var existingUser = userRepository.findByEmail(keycloakUserDto.getEmail());

            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                // Step 2a: User exists - only update keycloak_id if missing (no role
                // reassignment)
                user = existingUser.get();
                if (user.getKeycloakId() == null || user.getKeycloakId().isEmpty()) {
                    user.setKeycloakId(keycloakUserDto.getId());
                    log.debug("Updated existing user with Keycloak ID: {}", keycloakUserDto.getEmail());
                }
                log.debug("User already exists in database, skipping role re-assignment: {}",
                        keycloakUserDto.getEmail());
            } else {
                // Step 2b: User doesn't exist - create new user record
                user = new User();
                user.setKeycloakId(keycloakUserDto.getId());
                user.setEmail(keycloakUserDto.getEmail());
                user.setName(buildUserName(keycloakUserDto));
                user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
                user.setEnabled(keycloakUserDto.getEnabled() != null ? keycloakUserDto.getEnabled() : true);
                user.setAccountNonExpired(true);
                user.setAccountNonLocked(true);
                user.setCredentialsNonExpired(true);

                // Extract organization ID from attributes if present
                if (keycloakUserDto.getAttributes() != null) {
                    Map<String, Object> attrs = keycloakUserDto.getAttributes();
                    if (attrs.containsKey("organizationId")) {
                        Long orgId = Long.valueOf(attrs.get("organizationId").toString());
                        // Organization will be lazy-loaded if needed
                    }
                }

                isNewUser = true;
                log.info("Creating new user from Keycloak: {}", keycloakUserDto.getEmail());
            }

            // Step 3: Only assign roles for NEW users (avoid duplicate key violations on
            // subsequent syncs)
            if (isNewUser) {
                assignRolesToUser(user, roles);
                log.debug("Assigned roles to new user: {}", keycloakUserDto.getEmail());
            }

            // Step 4: Save user
            User savedUser = userRepository.save(user);
            log.info("User synced from Keycloak: {} (ID: {})", savedUser.getEmail(), savedUser.getId());

            return savedUser.getId();

        } catch (Exception e) {
            log.error("Error syncing user from Keycloak: {}", e.getMessage(), e);
            throw new ServiceLevelException(
                    "KeycloakUserSyncServiceImpl",
                    "Failed to sync user from Keycloak",
                    "syncUserFromKeycloak",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    /**
     * Assign roles to user based on Keycloak JWT claims
     */
    private void assignRolesToUser(User user, Set<String> roles) {
        Set<Role> userRoles = new HashSet<>();

        for (String roleName : roles) {
            try {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                userRoles.add(role);
            } catch (ResourceNotFoundException e) {
                log.warn("Role not found in IAM database: {}", roleName);
                // Optionally create role if not exists
                try {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    roleRepository.save(newRole);
                    userRoles.add(newRole);
                    log.info("Created missing role from Keycloak: {}", roleName);
                } catch (Exception ex) {
                    log.error("Error creating role {}: {}", roleName, ex.getMessage());
                }
            }
        }

        user.setRoles(userRoles);
    }

    /**
     * Build user name from first and last name
     */
    private String buildUserName(KeycloakUserDto userDto) {
        StringBuilder name = new StringBuilder();
        if (userDto.getFirstName() != null && !userDto.getFirstName().isEmpty()) {
            name.append(userDto.getFirstName());
        }
        if (userDto.getLastName() != null && !userDto.getLastName().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(userDto.getLastName());
        }
        return name.length() > 0 ? name.toString() : userDto.getEmail();
    }

    /**
     * Extract user data from JWT token
     * Parses token claims to build KeycloakUserDto
     */
    @Override
    public KeycloakUserDto extractUserFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            KeycloakUserDto userDto = KeycloakUserDto.builder()
                    .id(jwt.getClaimAsString("sub"))
                    .email(jwt.getClaimAsString("email"))
                    .username(jwt.getClaimAsString("preferred_username"))
                    .firstName(jwt.getClaimAsString("given_name"))
                    .lastName(jwt.getClaimAsString("family_name"))
                    .emailVerified(jwt.getClaimAsBoolean("email_verified"))
                    .enabled(true)
                    .build();

            log.debug("Extracted user from JWT: {}", userDto.getEmail());
            return userDto;

        } catch (Exception e) {
            log.error("Error extracting user from JWT: {}", e.getMessage(), e);
            throw new ServiceLevelException(
                    "KeycloakUserSyncServiceImpl",
                    "Failed to extract user from JWT",
                    "extractUserFromToken",
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    /**
     * Extract roles from JWT token claims
     * Reads from resource_access.<client-id>.roles claim
     * Filters out Keycloak system roles (offline_access, default-roles-*, uma_*)
     */
    @Override
    public Set<String> extractRolesFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            // Extract roles from resource_access.<client-id>.roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Set<String> roles = new HashSet<>();

            if (resourceAccess != null) {
                String clientId = webConstants.getKeycloakRoleClientId();
                Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get(clientId);

                if (clientRoles != null) {
                    Collection<String> clientRoleList = (Collection<String>) clientRoles.get("roles");
                    if (clientRoleList != null) {
                        // Filter out Keycloak system roles
                        roles.addAll(clientRoleList.stream()
                                .filter(this::isApplicationRole)
                                .collect(Collectors.toSet()));
                    }
                }
            }

            // Also check realm_access.roles as fallback
            if (roles.isEmpty()) {
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null) {
                    Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
                    if (realmRoles != null) {
                        // Filter out Keycloak system roles
                        roles.addAll(realmRoles.stream()
                                .filter(this::isApplicationRole)
                                .collect(Collectors.toSet()));
                    }
                }
            }

            log.debug("Extracted application roles from JWT: {}", roles);
            return roles;

        } catch (Exception e) {
            log.error("Error extracting roles from JWT: {}", e.getMessage(), e);
            return new HashSet<>();
        }
    }

    /**
     * Check if role is an application role (not a Keycloak system role)
     * Filters out:
     * - offline_access (for refresh tokens)
     * - default-roles-* (Keycloak default roles)
     * - uma_* (CIBA authorization roles)
     * - realm-management (realm admin roles)
     * - account (account management roles)
     */
    private boolean isApplicationRole(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }

        String lowerRole = role.toLowerCase();
        return !lowerRole.equals("offline_access") &&
                !lowerRole.startsWith("default-roles-") &&
                !lowerRole.startsWith("uma_") &&
                !lowerRole.equals("realm-management") &&
                !lowerRole.equals("account") &&
                !lowerRole.equals("account-console") &&
                !lowerRole.equals("account-manage-account") &&
                !lowerRole.equals("account-manage-account-links") &&
                !lowerRole.equals("account-view-profile");
    }
}
