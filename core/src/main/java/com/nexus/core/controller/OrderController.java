package com.nexus.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.OrderDto;
import com.nexus.core.security.OrganizationContextFilter;
import com.nexus.core.service.OrderService;
import com.nexus.core.utils.CommonUtils;
import com.nexus.core.utils.Logger;

@RestController
@RequestMapping("/core/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonUtils commonUtils;

	@Autowired
	private Logger logger;

	@PostMapping("/add")
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

		ResponseEntity<?> response = null;
		try {
			response = orderService.addOrder(orderDto);
		} catch (Exception e) {
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		} finally {
			logger.log("/core/orders/add", HttpMethod.POST,
					response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, orderDto,
					response != null ? response.getBody() : null, orgId);
		}

		return response;
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getOrder(@PathVariable Long id, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		ResponseEntity<?> response = null;
		try {
			response = orderService.getOrderByIdAndOrg(id, orgId);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		} finally {
			logger.log("/core/orders/{id}", HttpMethod.GET,
					response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, id,
					response != null ? response.getBody() : null, orgId);
		}

		return response;
	}

	@GetMapping("/all")
	public ResponseEntity<?> getAllOrders(@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		// Get organization ID from request context
		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		ResponseEntity<?> response = null;
		try {
			response = orderService.getAllOrdersByOrgId(orgId);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		} finally {
			logger.log("/core/orders/all", HttpMethod.GET,
					response != null ? response.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR, orgId,
					response != null ? response.getBody() : null, orgId);
		}
		return response;
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