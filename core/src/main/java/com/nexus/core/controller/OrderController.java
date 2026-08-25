package com.nexus.core.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.OrderDto;
import com.nexus.core.security.OrganizationContextFilter;
import com.nexus.core.service.OrderService;
import com.nexus.core.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;
	private final CommonUtils commonUtils;

	@PostMapping("/add")
	@LogActivity("Create Order")
	public ResponseEntity<?> addOrder(@RequestBody OrderDto orderDto, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context (set by OrganizationContextFilter)
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		// Set buyer organization ID on the order DTO
		orderDto.setBuyerOrgId(orgId);

		return orderService.addOrder(orderDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Order")
	public ResponseEntity<?> getOrder(@PathVariable Long id, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return orderService.getOrderByIdAndOrg(id, orgId);
	}

	@GetMapping("/all")
	@LogActivity("Get All Orders")
	public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return orderService.getAllOrdersByOrgId(orgId, pageable);
	}

	/**
	 * Extract organization ID from request attributes set by
	 * OrganizationContextFilter.
	 */
	private Long getOrganizationIdFromContext() {
		try {
			jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
					.getRequestAttributes())
					.getRequest();
			if (request != null) {
				Object orgContext = request.getAttribute(OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE);
				if (orgContext != null) {
					com.fasterxml.jackson.databind.JsonNode orgNode = (com.fasterxml.jackson.databind.JsonNode) orgContext;
					return orgNode.path("id").asLong();
				}
			}
		} catch (Exception e) {
			// Ignore and return null
		}
		return null;
	}

}