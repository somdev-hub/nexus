package com.nexus.core.config;

import com.nexus.core.security.OrganizationContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import lombok.RequiredArgsConstructor;

/**
 * Security Configuration for Nexus Core Service with Suite Foundation
 * <p>
 * Implements FR-FOUND-001 through FR-FOUND-004:
 * - Organization context validation via X-Organization-ID header
 * - Suite role mapping (SUITE_RETAILER, SUITE_SUPPLIER, SUITE_LOGISTICS,
 * SUITE_ADMIN)
 * - Organization-scoped data access enforcement
 * - IAM user context consumption
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final OrganizationContextFilter organizationContextFilter;

	/**
	 * CORS Configuration Source
	 * Allows requests from frontend applications
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001",
				"http://localhost:3002", "http://localhost:8080"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
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
	 * Security Chain:
	 * 1. CORS enabled
	 * 2. CSRF disabled (stateless API, no session)
	 * 3. Stateless session management
	 * 4. Exception handling (401/403 errors)
	 * 5. OAuth2 Resource Server (for Keycloak JWT validation)
	 * 6. Organization context filter (validates X-Organization-ID header)
	 * 7. Public endpoints: actuator, swagger
	 * 8. All other requests require authentication
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// Enable CORS
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

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

				// OAuth2 Resource Server support (Keycloak JWT validation)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(new SuiteJwtAuthenticationConverter())))

				// Authorization rules
				.authorizeHttpRequests((authz) -> authz
						// Public endpoints
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/swagger-ui/**").permitAll()
						.requestMatchers("/v3/api-docs/**").permitAll()
						.requestMatchers("/core/public/**").permitAll()
						// Suite role-based access control
						.requestMatchers("/core/products/**")
						.hasAnyRole("SUITE_RETAILER", "SUITE_SUPPLIER", "SUITE_LOGISTICS", "SUITE_ADMIN")
						.requestMatchers("/core/materials/**")
						.hasAnyRole("SUITE_RETAILER", "SUITE_SUPPLIER", "SUITE_LOGISTICS", "SUITE_ADMIN")
						.requestMatchers("/core/warehouses/**")
						.hasAnyRole("SUITE_RETAILER", "SUITE_SUPPLIER", "SUITE_LOGISTICS", "SUITE_ADMIN")
						.requestMatchers("/core/orders/**")
						.hasAnyRole("SUITE_RETAILER", "SUITE_SUPPLIER", "SUITE_LOGISTICS", "SUITE_ADMIN")
						.requestMatchers("/core/partnerships/**")
						.hasAnyRole("SUITE_RETAILER", "SUITE_SUPPLIER", "SUITE_LOGISTICS", "SUITE_ADMIN")
						// Admin-only endpoints
						.requestMatchers("/core/admin/**").hasRole("SUITE_ADMIN")
						// All other requests require authentication
						.anyRequest().authenticated())

				// Add organization context filter before default filter
				.addFilterBefore(organizationContextFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}