package com.nexus.iam.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for validating and extracting data from Keycloak JWT tokens
 * 
 * This can be used across all microservices for stateless JWT validation
 * without requiring database lookups or network calls to Keycloak.
 * 
 * Usage in microservices:
 * - Extract database user ID: keycloakTokenUtil.extractUserIdFromDatabase(token)
 * - Extract Keycloak ID (hexadecimal UUID): keycloakTokenUtil.extractKeycloakId(token)
 * - Extract roles: keycloakTokenUtil.extractRoles(token)
 * - Validate token: keycloakTokenUtil.validateToken(token)
 *
 * IMPORTANT: extractUserId() is deprecated - use extractUserIdFromDatabase() instead
 */
@Slf4j
@Component
public class KeycloakTokenUtil {

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private WebConstants webConstants;

    @Autowired(required = false)
    private com.nexus.iam.repository.UserRepository userRepository;

    /**
     * Validate JWT token against Keycloak JWKS
     * Checks signature, expiration, and issuer
     */
    public boolean validateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token.startsWith("Bearer ") ? token.substring(7) : token);
            log.debug("Token validation successful for user: {}", jwt.getClaimAsString("sub"));
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract database user ID from JWT token
     * Attempts multiple strategies:
     * 1. First checks for custom 'userId' claim (if added by Keycloak mapper)
     * 2. Falls back to extracting keycloakId (sub claim) and looking up in database
     *
     * @param token JWT token
     * @return Database user ID (Long), or null if not found
     */
    public Long extractUserIdFromDatabase(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token.startsWith("Bearer ") ? token.substring(7) : token);

            // Strategy 1: Check for custom userId claim (if Keycloak mapper is configured)
            Object userIdClaim = jwt.getClaims().get("userId");
            if (userIdClaim != null) {
                try {
                    return Long.valueOf(userIdClaim.toString());
                } catch (NumberFormatException e) {
                    log.debug("Custom userId claim found but not a valid Long: {}", userIdClaim);
                }
            }

            // Strategy 2: Extract keycloakId and look up in database
            String keycloakId = jwt.getClaimAsString("sub");
            if (keycloakId != null && userRepository != null) {
                var user = userRepository.findByKeycloakId(keycloakId);
                if (user.isPresent()) {
                    return user.get().getId();
                } else {
                    log.warn("User not found in database for keycloakId: {}", keycloakId);
                }
            }

            log.error("Could not extract database userId from token");
            return null;
        } catch (Exception e) {
            log.error("Error extracting database user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID (Keycloak subject - hexadecimal UUID) from JWT
     * WARNING: This returns the Keycloak ID, not the database userId
     * Use extractUserIdFromDatabase() to get the actual database userId instead
     */
    public String extractKeycloakId(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token.startsWith("Bearer ") ? token.substring(7) : token);
            return jwt.getClaimAsString("sub");
        } catch (Exception e) {
            log.error("Error extracting Keycloak ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID (Keycloak subject) from JWT
     * DEPRECATED: Use extractUserIdFromDatabase() instead to get the actual database userId
     */
    @Deprecated(forRemoval = true, since = "2.0")
    public String extractUserId(String token) {
        log.warn("extractUserId() is deprecated and returns Keycloak ID. Use extractUserIdFromDatabase() to get database userId");
        return extractKeycloakId(token);
    }

    /**
     * Extract email from JWT
     */
    public String extractEmail(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaimAsString("email");
        } catch (Exception e) {
            log.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract preferred username from JWT
     */
    public String extractUsername(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaimAsString("preferred_username");
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract roles from JWT token
     * Reads from resource_access.<client-id>.roles claim
     */
    public Set<String> extractRoles(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Set<String> roles = new HashSet<>();

            // Extract from resource_access.<client-id>.roles
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                String clientId = webConstants.getKeycloakRoleClientId();
                Map<String, Object> clientRoles = (Map<String, Object>) resourceAccess.get(clientId);

                if (clientRoles != null) {
                    Collection<String> roleList = (Collection<String>) clientRoles.get("roles");
                    if (roleList != null) {
                        roles.addAll(roleList);
                    }
                }
            }

            // Fallback to realm_access.roles
            if (roles.isEmpty()) {
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null) {
                    Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
                    if (realmRoles != null) {
                        roles.addAll(realmRoles);
                    }
                }
            }

            log.debug("Extracted roles from token: {}", roles);
            return roles;

        } catch (Exception e) {
            log.error("Error extracting roles from token: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    /**
     * Check if token has specific role
     */
    public boolean hasRole(String token, String roleName) {
        Set<String> roles = extractRoles(token);
        return roles.contains(roleName);
    }

    /**
     * Check if token has any of the specified roles
     */
    public boolean hasAnyRole(String token, String... roleNames) {
        Set<String> roles = extractRoles(token);
        for (String roleName : roleNames) {
            if (roles.contains(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract organization ID from custom user attributes in JWT
     * Returns null if not present
     */
    public Long extractOrganizationId(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Map<String, Object> attributes = jwt.getClaimAsMap("attributes");
            if (attributes != null && attributes.containsKey("organizationId")) {
                return Long.valueOf(attributes.get("organizationId").toString());
            }
        } catch (Exception e) {
            log.debug("Organization ID not found in token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get token expiration time (Unix timestamp in seconds)
     */
    public Long getExpirationTime(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getExpiresAt().getEpochSecond();
        } catch (Exception e) {
            log.error("Error extracting expiration from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Long expirationTime = getExpirationTime(token);
            if (expirationTime == null) {
                return true;
            }
            return System.currentTimeMillis() / 1000 > expirationTime;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
}

