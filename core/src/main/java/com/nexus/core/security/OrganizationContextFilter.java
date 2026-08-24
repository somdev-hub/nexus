package com.nexus.core.security;

import com.nexus.core.exception.OrganizationContextException;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.WebConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter to validate and establish organization context from X-Organization-ID
 * header.
 * <p>
 * This filter implements FR-FOUND-001: Organization Context
 * - Extracts X-Organization-ID header from request
 * - Validates that the authenticated user has access to the requested
 * organization via IAM service
 * - Sets organization context in request attributes for downstream use
 */
@Component
public class OrganizationContextFilter extends OncePerRequestFilter {

	public static final String ORGANIZATION_ID_HEADER = "X-Organization-ID";
	public static final String ORGANIZATION_CONTEXT_ATTRIBUTE = "suite.organizationContext";
	public static final String ORGANIZATION_TYPE_ATTRIBUTE = "suite.organizationType";

	@Autowired
	private CommonUtils commonUtils;

	@Autowired
	private WebConstants webConstants;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String requestPath = request.getRequestURI();

		// Skip organization context validation for public endpoints
		if (isPublicEndpoint(requestPath)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extract organization ID from header
		String orgIdHeader = request.getHeader(ORGANIZATION_ID_HEADER);

		if (!StringUtils.hasText(orgIdHeader)) {
			// For endpoints that require organization context, reject if missing
			if (requiresOrganizationContext(requestPath)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Missing required header: " + ORGANIZATION_ID_HEADER);
				return;
			}
			filterChain.doFilter(request, response);
			return;
		}

		try {
			Long organizationId = Long.parseLong(orgIdHeader);

			// Get authenticated user
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			String authToken = extractAuthToken(request);

			// Validate organization exists and is active via IAM service
			if (authToken != null) {
				var orgResponse = commonUtils.getOrganizationFromIam(organizationId, authToken);
				if (!orgResponse.getStatusCode().is2xxSuccessful()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND,
							"Organization not found: " + organizationId);
					return;
				}

				// Parse organization details from IAM response
				JsonNode orgNode = objectMapper.readTree(orgResponse.getBody());
				boolean isActive = orgNode.path("isActive").asBoolean(true);
				String orgType = orgNode.path("orgType").asText();

				// Validate organization is active
				if (!isActive) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN,
							"Organization is not active: " + organizationId);
					return;
				}

				// Validate user has access to this organization
				if (authentication != null && authentication.isAuthenticated()
						&& !"anonymousUser".equals(authentication.getPrincipal())) {

					Long userId = extractUserId(authentication);
					if (userId != null
							&& !commonUtils.validateUserOrganizationAccess(userId, organizationId, authToken)) {
						response.sendError(HttpServletResponse.SC_FORBIDDEN,
								"User does not have access to organization: " + organizationId);
						return;
					}
				}

				// Set organization context in request attributes
				request.setAttribute(ORGANIZATION_CONTEXT_ATTRIBUTE, orgNode);
				request.setAttribute(ORGANIZATION_TYPE_ATTRIBUTE, orgType);
			}

		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Invalid organization ID format: " + orgIdHeader);
			return;
		} catch (OrganizationContextException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
			return;
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Extract Authorization token from request.
	 */
	private String extractAuthToken(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader;
		}
		return null;
	}

	/**
	 * Extract user ID from authentication.
	 */
	private Long extractUserId(Authentication authentication) {
		// Try to get user ID from JWT claims or principal
		if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
			// Try to get user ID from various claims
			Long userId = jwt.getClaim("user_id");
			if (userId != null)
				return userId;

			userId = jwt.getClaim("sub");
			if (userId != null)
				return userId;

			// Try to parse from subject
			String sub = jwt.getSubject();
			if (sub != null) {
				try {
					return Long.parseLong(sub);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return null;
	}

	/**
	 * Check if the endpoint requires organization context.
	 * Suite API endpoints that operate on organization-scoped data require context.
	 */
	private boolean requiresOrganizationContext(String requestPath) {
		return requestPath.startsWith("/core/") && !requestPath.startsWith("/core/public/");
	}

	/**
	 * Check if endpoint is public (doesn't require authentication or org context).
	 */
	private boolean isPublicEndpoint(String requestPath) {
		return requestPath.startsWith("/actuator/")
				|| requestPath.startsWith("/swagger-ui/")
				|| requestPath.startsWith("/v3/api-docs/")
				|| requestPath.startsWith("/core/public/");
	}
}