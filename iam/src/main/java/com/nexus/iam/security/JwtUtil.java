package com.nexus.iam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import com.nexus.iam.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationWithMinimum256BitsForHS256AlgorithmSecurityPurpose}")
    private String jwtSecret;

    @Value("${jwt.access.expiration:3000000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshTokenExpiration;

    @Value("${keycloak.oauth2.enabled:false}")
    private boolean keycloakEnabled;

    @Autowired(required = false)
    private JwtDecoder jwtDecoder;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put("type", "access");
        extraClaims.put("roles", userDetails.getAuthorities());
        extraClaims.put("issuedAt", new Date());
        extraClaims.put("expiration", new Date(System.currentTimeMillis() + accessTokenExpiration));
        if (userDetails instanceof User) {
            extraClaims.put("userId", ((User) userDetails).getId());
        }
        return createToken(extraClaims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            if (keycloakEnabled) {
                // For Keycloak tokens (RS256)
                if (jwtDecoder == null) {
                    log.warn("Keycloak is enabled but JwtDecoder not available");
                    return false;
                }
                Jwt jwt = jwtDecoder.decode(token); // This validates signature and expiration
                String username = jwt.getClaimAsString("preferred_username");
                return username != null && username.equals(userDetails.getUsername());
            } else {
                // For traditional JWT (HMAC-SHA256)
                final String username = extractUsername(token);
                return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            }
        } catch (Exception e) {
            log.error("Error validating token with user details: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        if (keycloakEnabled) {
            return validateKeycloakToken(token);
        } else {
            return validateTraditionalJwt(token);
        }
    }

    public String extractUsernameFromToken(String token) {
        try {
            if (keycloakEnabled) {
                // For Keycloak tokens (RS256), extract from JwtDecoder
                if (jwtDecoder == null) {
                    log.warn("Keycloak is enabled but JwtDecoder not available");
                    return null;
                }
                Jwt jwt = jwtDecoder.decode(token);
                return jwt.getClaimAsString("preferred_username");
            } else {
                // For traditional JWT (HMAC-SHA256), use standard extraction
                return extractUsername(token);
            }
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            return null;
        }
    }

    public Long extractUserIdFromToken(String token) {
        try {
            if (keycloakEnabled) {
                // For Keycloak tokens (RS256), extract from JwtDecoder
                if (jwtDecoder == null) {
                    log.warn("Keycloak is enabled but JwtDecoder not available");
                    return null;
                }
                Jwt jwt = jwtDecoder.decode(token);
                // Try to get userId from token claims
                Object userIdObj = jwt.getClaims().get("userId");
                if (userIdObj instanceof Integer) {
                    return ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    return (Long) userIdObj;
                }
                return null;
            } else {
                // For traditional JWT (HMAC-SHA256), use standard extraction
                Claims claims = extractAllClaims(token);
                Object userIdObj = claims.get("userId");
                if (userIdObj instanceof Integer) {
                    return ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    return (Long) userIdObj;
                }
            }
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Validate token using either Keycloak or Traditional JWT based on
     * configuration
     * 
     * Strips "Bearer " prefix and delegates to appropriate validator:
     * - If keycloak.oauth2.enabled=true: Uses Keycloak token validation (via
     * JwtDecoder)
     * - If keycloak.oauth2.enabled=false: Uses traditional JWT validation
     * (HMAC-SHA256)
     * 
     * @param token Token string (may include "Bearer " prefix)
     * @return true if token is valid, false otherwise
     */
    public boolean isValidToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Token validation attempted with null/empty token");
            return false;
        }

        // Strip Bearer prefix if present
        String cleanToken = token;
        if (token.startsWith("Bearer ")) {
            cleanToken = token.substring(7);
        }

        // Route to appropriate validator based on configuration
        if (keycloakEnabled) {
            return validateKeycloakToken(cleanToken);
        } else {
            return validateTraditionalJwt(cleanToken);
        }
    }

    /**
     * Validate Keycloak token using JwtDecoder (Spring Security component)
     * This avoids circular dependency by using the core decoder instead of the
     * service layer.
     * 
     * @param token Keycloak JWT token
     * @return true if token is valid, false otherwise
     */
    private boolean validateKeycloakToken(String token) {
        try {
            if (jwtDecoder == null) {
                log.error("Keycloak is enabled but JwtDecoder is not available");
                return false;
            }

            // Decode and validate JWT signature using Keycloak's public key (via JWKS
            // endpoint)
            Jwt jwt = jwtDecoder.decode(token);

            // Check if token is expired
            if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(java.time.Instant.now())) {
                log.warn("Keycloak token has expired");
                return false;
            }

            log.debug("Keycloak token validated successfully");
            return true;

        } catch (Exception e) {
            log.error("Error validating Keycloak token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate traditional JWT token using HMAC-SHA256
     * 
     * @param token JWT token
     * @return true if token is valid and not expired, false otherwise
     */
    private boolean validateTraditionalJwt(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            if (isTokenExpired(token)) {
                log.warn("JWT token has expired");
                return false;
            }

            log.debug("Traditional JWT token validated successfully");
            return true;

        } catch (Exception e) {
            log.error("Error validating traditional JWT: {}", e.getMessage());
            return false;
        }
    }
}
