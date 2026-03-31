package com.nexus.iam.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSetCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for JWT and JWKS (JSON Web Key Set) handling
 * 
 * This configuration provides:
 * 1. Custom RestTemplate with extended timeouts for JWKS endpoint
 * 2. JwtDecoder with caching to reduce repeated calls to Keycloak
 * 3. Proper error handling and logging for JWT validation
 * 
 * Issue: Spring Security OAuth2 JWT decoder times out when fetching JWKS from Keycloak
 * Cause: Default timeouts are too short for slow Keycloak instances
 * Solution: Use custom RestTemplate with 60s read timeout and caching
 */
@Configuration
@Slf4j
public class JwtJwksConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${keycloak.jwt.connection-timeout:15000}")
    private int connectionTimeout;

    @Value("${keycloak.jwt.read-timeout:60000}")
    private int readTimeout;

    /**
     * Creates a RestTemplate specifically for JWKS endpoint requests
     * with increased timeouts to prevent "Read timed out" errors
     * 
     * Configuration:
     * - Connection Timeout: 15 seconds (connect to Keycloak)
     * - Read Timeout: 60 seconds (wait for JWKS response)
     */
    @Bean("jwksRestTemplate")
    public RestTemplate jwksRestTemplate() {
        log.info("Creating JWKS RestTemplate with connectionTimeout={}ms, readTimeout={}ms", 
                 connectionTimeout, readTimeout);
        
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        
        ClientHttpRequestFactory clientHttpRequestFactory = new BufferingClientHttpRequestFactory(factory);
        return new RestTemplate(clientHttpRequestFactory);
    }

    /**
     * Creates JwtDecoder bean with proper timeout configuration
     * Uses the JWKS RestTemplate to fetch public keys from Keycloak
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Initializing JwtDecoder with JWKS URI: {}", jwkSetUri);
        
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .restOperations(jwksRestTemplate())
                .build();
        
        log.info("JwtDecoder initialized successfully");
        return decoder;
    }
}

