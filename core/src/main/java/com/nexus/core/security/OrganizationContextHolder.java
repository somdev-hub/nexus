package com.nexus.core.security;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility class to access organization context from the current request.
 * This allows services to extract organization ID without controllers passing
 * it explicitly.
 */
public final class OrganizationContextHolder {

	private OrganizationContextHolder() {
		// Utility class
	}

	/**
	 * Get the current organization ID from the request context.
	 * 
	 * @return organization ID, or null if not available
	 */
	public static Long getCurrentOrganizationId() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
					.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				Object orgContext = request.getAttribute(OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE);
				if (orgContext != null) {
					JsonNode orgNode = (JsonNode) orgContext;
					return orgNode.path("id").asLong();
				}
			}
		} catch (Exception e) {
			// Log error if needed
		}
		return null;
	}

	/**
	 * Get the current organization type from the request context.
	 * 
	 * @return organization type, or null if not available
	 */
	public static String getCurrentOrganizationType() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
					.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				Object orgType = request.getAttribute(OrganizationContextFilter.ORGANIZATION_TYPE_ATTRIBUTE);
				if (orgType != null) {
					return orgType.toString();
				}
			}
		} catch (Exception e) {
			// Log error if needed
		}
		return null;
	}

	/**
	 * Get the full organization context node from the request context.
	 * 
	 * @return organization context as JsonNode, or null if not available
	 */
	public static JsonNode getCurrentOrganizationContext() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
					.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				Object orgContext = request.getAttribute(OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE);
				if (orgContext != null) {
					return (JsonNode) orgContext;
				}
			}
		} catch (Exception e) {
			// Log error if needed
		}
		return null;
	}

	/**
	 * Check if organization context is available in the current request.
	 * 
	 * @return true if organization context is available
	 */
	public static boolean hasOrganizationContext() {
		return getCurrentOrganizationId() != null;
	}

	/**
	 * Require organization context, throwing an exception if not available.
	 * 
	 * @return organization ID
	 * @throws IllegalStateException if organization context is not available
	 */
	public static Long requireOrganizationId() {
		Long orgId = getCurrentOrganizationId();
		if (orgId == null) {
			throw new IllegalStateException(
					"Organization context not found. Ensure X-Organization-ID header is provided and OrganizationContextFilter is configured.");
		}
		return orgId;
	}
}