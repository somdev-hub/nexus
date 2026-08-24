package com.nexus.core.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JWT Authentication Converter for Suite Role Mapping.
 * <p>
 * Implements FR-FOUND-002: Suite Role Mapping
 * <p>
 * Maps Keycloak realm roles to Spring Security authorities with suite-specific
 * roles:
 * - SUITE_RETAILER: Retailer portal access
 * - SUITE_SUPPLIER: Supplier portal access
 * - SUITE_LOGISTICS: Logistics portal access
 * - SUITE_ADMIN: Suite administrative access
 * <p>
 * Also extracts organization IDs from JWT claims for organization context
 * validation.
 */
@Component
public class SuiteJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	// Suite-specific role constants
	public static final String ROLE_SUITE_RETAILER = "SUITE_RETAILER";
	public static final String ROLE_SUITE_SUPPLIER = "SUITE_SUPPLIER";
	public static final String ROLE_SUITE_LOGISTICS = "SUITE_LOGISTICS";
	public static final String ROLE_SUITE_ADMIN = "SUITE_ADMIN";

	// Keycloak claim names
	private static final String REALM_ACCESS_CLAIM = "realm_access";
	private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
	private static final String ROLES_CLAIM = "roles";
	private static final String ORGANIZATIONS_CLAIM = "organizations";
	private static final String ORG_ID_CLAIM = "org_id";
	private static final String ORG_TYPE_CLAIM = "org_type";

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
		return new JwtAuthenticationToken(jwt, authorities);
	}

	/**
	 * Extract authorities from JWT claims.
	 * <p>
	 * Sources of authorities:
	 * 1. Realm roles (realm_access.roles)
	 * 2. Client roles (resource_access.{client}.roles)
	 * 3. Suite-specific roles mapped from realm/client roles
	 * 4. Organization IDs as ORG_{id} authorities
	 */
	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		Set<GrantedAuthority> authorities = Stream.of(
				extractRealmRoles(jwt),
				extractClientRoles(jwt),
				extractSuiteRoles(jwt),
				extractOrganizationAuthorities(jwt)).flatMap(Collection::stream).collect(Collectors.toSet());

		return authorities;
	}

	/**
	 * Extract realm-level roles from JWT.
	 */
	private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
		if (realmAccess == null || !realmAccess.containsKey(ROLES_CLAIM)) {
			return List.of();
		}

		@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);
		return roles.stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
				.collect(Collectors.toSet());
	}

	/**
	 * Extract client-specific roles from JWT.
	 */
	private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
		Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
		if (resourceAccess == null) {
			return List.of();
		}

		return resourceAccess.values().stream()
				.filter(client -> client instanceof Map)
				.map(client -> (Map<String, Object>) client)
				.filter(client -> client.containsKey(ROLES_CLAIM))
				.flatMap(client -> {
					@SuppressWarnings("unchecked")
					List<String> roles = (List<String>) client.get(ROLES_CLAIM);
					return roles.stream();
				})
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
				.collect(Collectors.toSet());
	}

	/**
	 * Extract and map suite-specific roles.
	 * <p>
	 * Maps IAM roles to suite roles:
	 * - RETAILER_ADMIN, RETAILER_USER -> SUITE_RETAILER
	 * - SUPPLIER_ADMIN, SUPPLIER_USER -> SUITE_SUPPLIER
	 * - LOGISTICS_ADMIN, LOGISTICS_USER -> SUITE_LOGISTICS
	 * - PLATFORM_ADMIN -> SUITE_ADMIN
	 */
	private Collection<GrantedAuthority> extractSuiteRoles(Jwt jwt) {
		Set<String> allRoles = Stream.of(
				extractRealmRoles(jwt),
				extractClientRoles(jwt)).flatMap(Collection::stream)
				.map(GrantedAuthority::getAuthority)
				.map(auth -> auth.replace("ROLE_", ""))
				.collect(Collectors.toSet());

		return allRoles.stream()
				.flatMap(role -> mapToSuiteRoles(role).stream())
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.collect(Collectors.toSet());
	}

	/**
	 * Map IAM role to suite roles.
	 */
	private Set<String> mapToSuiteRoles(String role) {
		return switch (role.toUpperCase()) {
			case "RETAILER_ADMIN", "RETAILER_USER" -> Set.of(ROLE_SUITE_RETAILER);
			case "SUPPLIER_ADMIN", "SUPPLIER_USER" -> Set.of(ROLE_SUITE_SUPPLIER);
			case "LOGISTICS_ADMIN", "LOGISTICS_USER" -> Set.of(ROLE_SUITE_LOGISTICS);
			case "PLATFORM_ADMIN", "SUPER_ADMIN" -> Set.of(ROLE_SUITE_ADMIN);
			default -> Set.of();
		};
	}

	/**
	 * Extract organization IDs from JWT claims and create ORG_{id} authorities.
	 * <p>
	 * This enables OrganizationContextFilter to validate organization access.
	 */
	private Collection<GrantedAuthority> extractOrganizationAuthorities(Jwt jwt) {
		// Try to get organizations from custom claim
		Map<String, Object> organizationsClaim = jwt.getClaimAsMap(ORGANIZATIONS_CLAIM);
		if (organizationsClaim != null && organizationsClaim.containsKey(ORG_ID_CLAIM)) {
			@SuppressWarnings("unchecked")
			List<Long> orgIds = (List<Long>) organizationsClaim.get(ORG_ID_CLAIM);
			return orgIds.stream()
					.map(id -> new SimpleGrantedAuthority("ORG_" + id))
					.collect(Collectors.toSet());
		}

		// Fallback: try to get single org_id claim
		Long orgId = jwt.getClaim(ORG_ID_CLAIM);
		if (orgId != null) {
			return Set.of(new SimpleGrantedAuthority("ORG_" + orgId));
		}

		return List.of();
	}
}