package com.nexus.core.controller;

import jakarta.validation.Valid;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.exception.InvalidCredentialsException;
import com.nexus.core.payload.PurchaseOrderDto;
import com.nexus.core.security.OrganizationContextFilter;
import com.nexus.core.service.PurchaseOrderService;
import com.nexus.core.utils.CommonUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

	private final PurchaseOrderService purchaseOrderService;
	private final CommonUtils commonUtils;

	@PostMapping("/create")
	@LogActivity("Create Purchase Order")
	public ResponseEntity<?> createPurchaseOrder(@Valid @RequestBody PurchaseOrderDto poDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		// Set buyer organization from context
		poDto.setBuyerOrgId(orgId);

		return purchaseOrderService.createPurchaseOrder(poDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Purchase Order")
	public ResponseEntity<?> getPurchaseOrder(@PathVariable Long id, @RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return purchaseOrderService.getPurchaseOrderByIdAndOrg(id, orgId);
	}

	@GetMapping("/all")
	@LogActivity("Get All Purchase Orders")
	public ResponseEntity<?> getAllPurchaseOrders(@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return purchaseOrderService.getAllPurchaseOrdersByOrgId(orgId, pageable);
	}

	@GetMapping("/status/{status}")
	@LogActivity("Get Purchase Orders by Status")
	public ResponseEntity<?> getPurchaseOrdersByStatus(@PathVariable String status,
			@RequestHeader("Authorization") String token,
			@PageableDefault(size = 20) Pageable pageable) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		try {
			com.nexus.core.entities.PurchaseOrderStatus poStatus = com.nexus.core.entities.PurchaseOrderStatus
					.valueOf(status.toUpperCase());
			return purchaseOrderService.getPurchaseOrdersByOrgIdAndStatus(orgId, poStatus, pageable);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + status);
		}
	}

	@PutMapping("/{id}/update")
	@LogActivity("Update Purchase Order")
	public ResponseEntity<?> updatePurchaseOrder(@PathVariable Long id,
			@RequestBody PurchaseOrderDto poDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return purchaseOrderService.updatePurchaseOrder(id, poDto, orgId);
	}

	@PutMapping("/{id}/transition")
	@LogActivity("Transition Purchase Order Status")
	public ResponseEntity<?> transitionStatus(@PathVariable Long id,
			@RequestParam String newStatus,
			@RequestBody(required = false) Map<String, Object> params,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		try {
			com.nexus.core.entities.PurchaseOrderStatus targetStatus = com.nexus.core.entities.PurchaseOrderStatus
					.valueOf(newStatus.toUpperCase());
			return purchaseOrderService.transitionStatus(id, targetStatus, params, orgId);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid status: " + newStatus);
		}
	}

	@PostMapping("/{parentPoId}/amend")
	@LogActivity("Create Purchase Order Amendment")
	public ResponseEntity<?> createAmendment(@PathVariable Long parentPoId,
			@RequestBody PurchaseOrderDto amendmentDto,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return purchaseOrderService.createAmendment(parentPoId, amendmentDto, orgId);
	}

	@GetMapping("/{parentPoId}/amendments")
	@LogActivity("Get Purchase Order Amendments")
	public ResponseEntity<?> getAmendments(@PathVariable Long parentPoId,
			@RequestHeader("Authorization") String token) {
		if (!commonUtils.validateToken(token)) {
			throw new InvalidCredentialsException();
		}

		Long orgId = getOrganizationIdFromContext();
		if (orgId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organization context not found");
		}

		return purchaseOrderService.getAmendmentsByParentPoId(parentPoId, orgId);
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