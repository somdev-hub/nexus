package com.nexus.iam.config;

import com.nexus.iam.security.JwtAuthenticationFilter;
import com.nexus.iam.service.impl.CustomUserDetailsService;
import com.nexus.iam.utils.WebConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration for IAM Service
 * <p>
 * Architecture:
 * - Stateless session management (JWT-based)
 * - Custom JWT filter for legacy authentication endpoints
 * - CORS enabled for frontend access
 * - OAuth2 Resource Server support for Keycloak integration
 * - Method-level security with @PreAuthorize and @PostAuthorize
 * <p>
 * Authentication Flow:
 * 1. Login/Register endpoints use custom JWT (until migrated to Keycloak SSO)
 * 2. Other endpoints validated by JwtAuthenticationFilter
 * 3. RestClient handles all Keycloak API communications
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private WebConstants webConstants;

    /**
     * Password encoder bean using BCrypt
     * BCrypt includes salt generation and is recommended for production
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DAO Authentication Provider for legacy authentication
     * Used by AuthenticationManager for login/register endpoints
     * <p>
     * Note: Spring Security 7.x requires UserDetailsService in constructor
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager bean for legacy auth endpoints
     * Used by AuthenticationServiceImpl for login/register operations
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) {
        return authConfig.getAuthenticationManager();
    }


    /**
     * CORS Configuration Source
     * Allows requests from frontend applications
     * <p>
     * Allowed Origins:
     * - http://localhost:3000 (React dev server)
     * - http://localhost:8080 (Angular dev server)
     * <p>
     * Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
     * Credentials: Allowed (for cookie-based auth if needed)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Security Filter Chain Configuration
     * <p>
     * HYBRID MODE: Supports both Phase 1 (Legacy JWT) and Phase 2 (Keycloak OAuth2)
     * 
     * Security Chain:
     * 1. CORS enabled
     * 2. CSRF disabled (stateless API, no session)
     * 3. Stateless session management
     * 4. Exception handling (401/403 errors)
     * 5. OAuth2 Resource Server (for Keycloak JWT validation) - Phase 2
     * 6. Custom JWT filter (for legacy JWT validation) - Phase 1
     * 7. Public endpoints: login, register, auth, oauth2 endpoints
     * 8. All other endpoints require authentication (either Phase 1 or Phase 2)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS
                .cors(Customizer.withDefaults())

                // Disable CSRF for stateless API
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(403, "Forbidden");
                        }))

                // OAuth2 Resource Server support (Phase 2 - Keycloak)
                // This validates JWT tokens signed by Keycloak
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))

                // Authorization rules
                .authorizeHttpRequests((authz) -> authz
                        // Public authentication endpoints (Phase 1 & Phase 2 unified)
                        .requestMatchers("/iam/auth/register").permitAll()
                        .requestMatchers("/iam/auth/register/applicant").permitAll()
                        .requestMatchers("/iam/auth/login").permitAll()
                        .requestMatchers("/iam/auth/refresh").permitAll()
                        .requestMatchers("/iam/auth/verify").permitAll()
                        .requestMatchers("/iam/auth/decrypt").permitAll()
                        
                        // OAuth2 Redirect endpoints (Phase 2 - for UI redirect flow)
                        .requestMatchers("/iam/auth/oauth2/callback").permitAll()
                        .requestMatchers("/iam/auth/oauth2/auth-url").permitAll()
                        .requestMatchers("/iam/auth/oauth2/refresh").permitAll()
                        .requestMatchers("/iam/auth/oauth2/validate-and-sync").permitAll()
                        
                        // General public endpoints
                        .requestMatchers("/iam/public/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // Add custom JWT filter before default filter (Phase 1)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Set authentication provider (Phase 1)
                .authenticationProvider(daoAuthenticationProvider());

        return http.build();
    }
}