package com.nexus.iam.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.nexus.iam.dto.KeycloakRoleDto;
import com.nexus.iam.dto.KeycloakUserDto;
import com.nexus.iam.service.KeycloakAdminService;
import com.nexus.iam.utils.WebConstants;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of Keycloak Admin Service using Keycloak Admin Client
 * Includes circuit breaker and retry logic for resilience
 */
@Slf4j
@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    @Autowired
    private WebConstants webConstants;

    @Autowired
    private RestClient restClient;

    private Keycloak keycloakClient;
    private String adminToken;
    private long tokenExpireTime;

    /**
     * Get or refresh admin token from Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "getAdminTokenFallback")
    @Retry(name = "keycloak-admin")
    public String getAdminToken() {
        try {
            // Check if token is still valid (with 1-minute buffer)
            if (adminToken != null && System.currentTimeMillis() < tokenExpireTime - 60000) {
                return adminToken;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", webConstants.getKeycloakAdminGrantType());
            body.add("client_id", webConstants.getKeycloakAdminClientId());
            body.add("client_secret", webConstants.getKeycloakAdminClientSecret());

            ResponseEntity<Map> response = restClient.post()
                    .uri(webConstants.getKeycloakTokenUrl())
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getBody() != null) {
                adminToken = (String) response.getBody().get("access_token");
                Integer expiresIn = (Integer) response.getBody().get("expires_in");
                tokenExpireTime = System.currentTimeMillis() + (expiresIn * 1000L);
                log.info("Admin token refreshed successfully");
                return adminToken;
            }

            throw new RuntimeException("Failed to obtain admin token from Keycloak");
        } catch (Exception e) {
            log.error("Error obtaining admin token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to obtain admin token", e);
        }
    }

    public String getAdminTokenFallback(Exception e) {
        log.warn("Circuit breaker fallback: getAdminToken failed, returning cached token if available");
        if (adminToken != null) {
            return adminToken;
        }
        throw new RuntimeException("Keycloak admin service unavailable and no cached token available", e);
    }

    /**
     * Create a new role in Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "createRoleInKeycloakFallback")
    @Retry(name = "keycloak-admin")
    public String createRoleInKeycloak(String roleName) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName(roleName);
            roleRepresentation.setClientRole(false);
            roleRepresentation.setComposite(false);

            ResponseEntity<Void> response = restClient.post()
                    .uri(webConstants.getKeycloakAdminUrl() + "/roles")
                    .headers(h -> h.addAll(headers))
                    .body(roleRepresentation)
                    .retrieve()
                    .toEntity(Void.class);

            log.info("Role created in Keycloak: {}", roleName);

            // Get the created role to return its ID
            KeycloakRoleDto createdRole = getRoleByName(roleName);
            return createdRole != null ? createdRole.getId() : roleName;

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("PERMISSION DENIED: Service account lacks admin permissions to create role '{}'. " +
                    "Assign 'manage-roles' permission to iam-client. Error: {}", roleName, e.getMessage());
            throw new RuntimeException("Keycloak service account lacks required 'manage-roles' permission", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("AUTHENTICATION FAILED: Cannot create role '{}' - authentication failed. Error: {}", roleName,
                    e.getMessage());
            throw new RuntimeException("Keycloak authentication failed. Check credentials.", e);
        } catch (Exception e) {
            log.error("Error creating role in Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create role in Keycloak: " + roleName, e);
        }
    }

    public String createRoleInKeycloakFallback(String roleName, Exception e) {
        log.warn("Circuit breaker fallback: createRoleInKeycloak failed for role: {}", roleName);
        throw new RuntimeException("Keycloak service unavailable, cannot create role: " + roleName, e);
    }

    /**
     * Delete a role from Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "deleteRoleFromKeycloakFallback")
    @Retry(name = "keycloak-admin")
    public void deleteRoleFromKeycloak(String keycloakRoleId) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            restClient.delete()
                    .uri(webConstants.getKeycloakAdminUrl() + "/roles-by-id/" + keycloakRoleId)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Role deleted from Keycloak: {}", keycloakRoleId);

        } catch (Exception e) {
            log.error("Error deleting role from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete role from Keycloak: " + keycloakRoleId, e);
        }
    }

    public void deleteRoleFromKeycloakFallback(String keycloakRoleId, Exception e) {
        log.warn("Circuit breaker fallback: deleteRoleFromKeycloak failed for role ID: {}", keycloakRoleId);
        throw new RuntimeException("Keycloak service unavailable, cannot delete role: " + keycloakRoleId, e);
    }

    /**
     * Get a role by name from Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "getRoleByNameFallback")
    @Retry(name = "keycloak-admin")
    public KeycloakRoleDto getRoleByName(String roleName) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<RoleRepresentation> response = restClient.get()
                    .uri(webConstants.getKeycloakAdminUrl() + "/roles/" + roleName)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(RoleRepresentation.class);

            if (response.getBody() != null) {
                RoleRepresentation roleRep = response.getBody();
                return KeycloakRoleDto.builder()
                        .id(roleRep.getId())
                        .name(roleRep.getName())
                        .description(roleRep.getDescription())
                        .composite(Boolean.TRUE.equals(roleRep.isComposite()))
                        .build();
            }

            return null;

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("PERMISSION DENIED: Service account lacks admin permissions to retrieve role '{}'. Error: {}",
                    roleName, e.getMessage());
            return null;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("AUTHENTICATION FAILED: Cannot retrieve role '{}' - authentication failed. Error: {}", roleName,
                    e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error retrieving role from Keycloak: {}", e.getMessage(), e);
            return null;
        }
    }

    public KeycloakRoleDto getRoleByNameFallback(String roleName, Exception e) {
        log.warn("Circuit breaker fallback: getRoleByName failed for role: {}", roleName);
        return null;
    }

    /**
     * List all roles in Keycloak realm
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "listAllRolesFallback")
    @Retry(name = "keycloak-admin")
    public List<KeycloakRoleDto> listAllRoles() {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<RoleRepresentation[]> response = restClient.get()
                    .uri(webConstants.getKeycloakAdminUrl() + "/roles")
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(RoleRepresentation[].class);

            if (response.getBody() != null) {
                return Arrays.stream(response.getBody())
                        .map(roleRep -> KeycloakRoleDto.builder()
                                .id(roleRep.getId())
                                .name(roleRep.getName())
                                .description(roleRep.getDescription())
                                .composite(Boolean.TRUE.equals(roleRep.isComposite()))
                                .build())
                        .collect(Collectors.toList());
            }

            return List.of();

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("PERMISSION DENIED: Service account lacks admin permissions on Keycloak. " +
                    "The iam-client service account needs to be assigned admin roles in Keycloak. " +
                    "See KEYCLOAK_ADMIN_PERMISSIONS_FIX.md for configuration steps. Error: {}", e.getMessage());
            throw new RuntimeException(
                    "Keycloak service account lacks required admin permissions. " +
                            "See KEYCLOAK_ADMIN_PERMISSIONS_FIX.md for configuration steps.",
                    e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("AUTHENTICATION FAILED: Invalid credentials or token for Keycloak admin API. Error: {}",
                    e.getMessage());
            throw new RuntimeException("Keycloak authentication failed. Check credentials in configuration.", e);
        } catch (Exception e) {
            log.error("Error listing roles from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list roles from Keycloak", e);
        }
    }

    public List<KeycloakRoleDto> listAllRolesFallback(Exception e) {
        log.warn("Circuit breaker fallback: listAllRoles failed");
        throw new RuntimeException("Keycloak service unavailable, cannot list roles", e);
    }

    /**
     * Create a new user in Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "createUserInKeycloakFallback")
    @Retry(name = "keycloak-admin")
    public String createUserInKeycloak(KeycloakUserDto userDto) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setUsername(userDto.getUsername() != null ? userDto.getUsername() : userDto.getEmail());
            userRepresentation.setEmail(userDto.getEmail());
            userRepresentation.setFirstName(userDto.getFirstName());
            userRepresentation.setLastName(userDto.getLastName());
            userRepresentation.setEnabled(userDto.getEnabled() != null ? userDto.getEnabled() : true);
            userRepresentation
                    .setEmailVerified(userDto.getEmailVerified() != null ? userDto.getEmailVerified() : false);
            if (userDto.getAttributes() != null && !userDto.getAttributes().isEmpty()) {
                // Convert Map<String, Object> to Map<String, List<String>>
                Map<String, List<String>> attrs = new java.util.HashMap<>();
                for (Map.Entry<String, Object> entry : userDto.getAttributes().entrySet()) {
                    Object value = entry.getValue();
                    List<String> valueList;
                    if (value instanceof List) {
                        valueList = (List<String>) value;
                    } else {
                        valueList = java.util.Arrays.asList(value.toString());
                    }
                    attrs.put(entry.getKey(), valueList);
                }
                userRepresentation.setAttributes(attrs);
            }

            restClient.post()
                    .uri(webConstants.getKeycloakAdminUrl() + "/users")
                    .headers(h -> h.addAll(headers))
                    .body(userRepresentation)
                    .retrieve()
                    .toBodilessEntity();

            // Set password if provided
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                String userId = getUserByEmail(userDto.getEmail()).getId();
                setUserPassword(userId, userDto.getPassword());
            }

            log.info("User created in Keycloak: {}", userDto.getEmail());

            // Get the created user to return its ID
            KeycloakUserDto createdUser = getUserByEmail(userDto.getEmail());
            return createdUser != null ? createdUser.getId() : userDto.getEmail();

        } catch (org.springframework.web.client.HttpClientErrorException.Conflict e) {
            log.warn("User already exists in Keycloak: {}", userDto.getEmail());
            throw new RuntimeException("User with email " + userDto.getEmail() + " already exists in Keycloak", e);
        } catch (Exception e) {
            log.error("Error creating user in Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user in Keycloak: " + userDto.getEmail(), e);
        }
    }

    public String createUserInKeycloakFallback(KeycloakUserDto userDto, Exception e) {
        log.warn("Circuit breaker fallback: createUserInKeycloak failed for user: {}", userDto.getEmail());
        // Check if this is a 409 Conflict (user already exists)
        if (e.getCause() instanceof org.springframework.web.client.HttpClientErrorException.Conflict) {
            throw new RuntimeException("User already exists in Keycloak: " + userDto.getEmail(), e);
        }
        throw new RuntimeException("Keycloak service unavailable, cannot create user: " + userDto.getEmail(), e);
    }

    /**
     * Get a user by email from Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "getUserByEmailFallback")
    @Retry(name = "keycloak-admin")
    public KeycloakUserDto getUserByEmail(String email) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<UserRepresentation[]> response = restClient.get()
                    .uri(webConstants.getKeycloakAdminUrl() + "/users?email=" + email)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .toEntity(UserRepresentation[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                UserRepresentation userRep = response.getBody()[0];

                // Convert Map<String, List<String>> to Map<String, Object>
                Map<String, Object> attrs = null;
                if (userRep.getAttributes() != null && !userRep.getAttributes().isEmpty()) {
                    attrs = new java.util.HashMap<>();
                    for (Map.Entry<String, List<String>> entry : userRep.getAttributes().entrySet()) {
                        List<String> values = entry.getValue();
                        attrs.put(entry.getKey(), values.size() == 1 ? values.get(0) : values);
                    }
                }

                return KeycloakUserDto.builder()
                        .id(userRep.getId())
                        .email(userRep.getEmail())
                        .username(userRep.getUsername())
                        .firstName(userRep.getFirstName())
                        .lastName(userRep.getLastName())
                        .enabled(userRep.isEnabled())
                        .emailVerified(userRep.isEmailVerified())
                        .attributes(attrs)
                        .build();
            }

            return null;

        } catch (Exception e) {
            log.error("Error retrieving user from Keycloak: {}", e.getMessage(), e);
            return null;
        }
    }

    public KeycloakUserDto getUserByEmailFallback(String email, Exception e) {
        log.warn("Circuit breaker fallback: getUserByEmail failed for email: {}", email);
        return null;
    }

    /**
     * Assign a role to a user in Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "assignRoleToUserFallback")
    @Retry(name = "keycloak-admin")
    public void assignRoleToUser(String keycloakUserId, String keycloakRoleId, String roleName) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setId(keycloakRoleId);
            roleRepresentation.setName(roleName); // Include role name for Keycloak to find the role

            restClient.post()
                    .uri(webConstants.getKeycloakAdminUrl() + "/users/" + keycloakUserId + "/role-mappings/realm")
                    .headers(h -> h.addAll(headers))
                    .body(new RoleRepresentation[] { roleRepresentation })
                    .retrieve()
                    .toBodilessEntity();

            log.info("Role assigned to user in Keycloak: userId={}, roleId={}, roleName={}", keycloakUserId,
                    keycloakRoleId, roleName);

        } catch (Exception e) {
            log.error("Error assigning role to user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign role to user", e);
        }
    }

    public void assignRoleToUserFallback(String keycloakUserId, String keycloakRoleId, String roleName, Exception e) {
        log.warn("Circuit breaker fallback: assignRoleToUser failed for userId: {}, roleId: {}, roleName: {}",
                keycloakUserId, keycloakRoleId, roleName);
        throw new RuntimeException("Keycloak service unavailable, cannot assign role to user", e);
    }

    /**
     * Remove a role from a user in Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "removeRoleFromUserFallback")
    @Retry(name = "keycloak-admin")
    public void removeRoleFromUser(String keycloakUserId, String keycloakRoleId) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setId(keycloakRoleId);

            restClient.method(HttpMethod.DELETE)
                    .uri(webConstants.getKeycloakAdminUrl() + "/users/" + keycloakUserId + "/role-mappings/realm")
                    .headers(h -> h.addAll(headers))
                    .body(new RoleRepresentation[] { roleRepresentation })
                    .retrieve()
                    .toBodilessEntity();

            log.info("Role removed from user in Keycloak: userId={}, roleId={}", keycloakUserId, keycloakRoleId);

        } catch (Exception e) {
            log.error("Error removing role from user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to remove role from user", e);
        }
    }

    public void removeRoleFromUserFallback(String keycloakUserId, String keycloakRoleId, Exception e) {
        log.warn("Circuit breaker fallback: removeRoleFromUser failed for userId: {}, roleId: {}",
                keycloakUserId, keycloakRoleId);
        throw new RuntimeException("Keycloak service unavailable, cannot remove role from user", e);
    }

    /**
     * Set user password in Keycloak
     */
    @Override
    @CircuitBreaker(name = "keycloak-admin", fallbackMethod = "setUserPasswordFallback")
    @Retry(name = "keycloak-admin")
    public void setUserPassword(String keycloakUserId, String password) {
        try {
            String token = getAdminToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create credential object for password reset
            // Keycloak reset-password endpoint expects a single credential object (not an
            // array)
            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false);

            restClient.put()
                    .uri(webConstants.getKeycloakAdminUrl() + "/users/" + keycloakUserId + "/reset-password")
                    .headers(h -> h.addAll(headers))
                    .body(credential)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password set for user in Keycloak: {}", keycloakUserId);

        } catch (Exception e) {
            log.error("Error setting user password: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to set user password", e);
        }
    }

    public void setUserPasswordFallback(String keycloakUserId, String password, Exception e) {
        log.warn("Circuit breaker fallback: setUserPassword failed for userId: {}", keycloakUserId);
        throw new RuntimeException("Keycloak service unavailable, cannot set user password", e);
    }

    /**
     * Check if a role exists in Keycloak
     */
    @Override
    public boolean roleExists(String roleName) {
        try {
            KeycloakRoleDto role = getRoleByName(roleName);
            return role != null;
        } catch (Exception e) {
            log.warn("Error checking role existence: {}", e.getMessage());
            return false;
        }
    }
}
