package com.nexus.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.dto.SupplierRiskMonitoringCreateRequest;
import com.nexus.core.dto.SupplierRiskMonitoringUpdateRequest;
import com.nexus.core.service.interfaces.SupplierRiskMonitoringService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Supplier Risk Monitoring.
 * FR-RET-024: Supplier Risk Monitoring
 */
@RestController
@RequestMapping("/core/supplier-risk-monitoring")
@RequiredArgsConstructor
public class SupplierRiskMonitoringController {

	private final SupplierRiskMonitoringService riskMonitoringService;

	@PostMapping("/create")
	@LogActivity("Create Supplier Risk Monitoring")
	public ResponseEntity<?> createRiskMonitoring(@Valid @RequestBody SupplierRiskMonitoringCreateRequest request) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.createRiskMonitoring(request, orgId));
	}

	@GetMapping("/{id}")
	@LogActivity("Get Supplier Risk Monitoring")
	public ResponseEntity<?> getRiskMonitoringById(@PathVariable Long id) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getRiskMonitoringById(id, orgId));
	}

	@GetMapping("/supplier/{supplierId}")
	@LogActivity("Get Supplier Risk Monitoring by Supplier")
	public ResponseEntity<?> getRiskMonitoringBySupplierId(@PathVariable Long supplierId) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getRiskMonitoringBySupplierId(supplierId, orgId));
	}

	@GetMapping("/partnership/{partnershipId}")
	@LogActivity("Get Supplier Risk Monitoring by Partnership")
	public ResponseEntity<?> getRiskMonitoringByPartnershipId(@PathVariable Long partnershipId) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getRiskMonitoringByPartnershipId(partnershipId, orgId));
	}

	@GetMapping("/risk-level/{riskLevel}")
	@LogActivity("Get Supplier Risk Monitoring by Risk Level")
	public ResponseEntity<?> getRiskMonitoringByRiskLevel(@PathVariable String riskLevel) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getRiskMonitoringByRiskLevel(riskLevel, orgId));
	}

	@GetMapping("/due-for-review")
	@LogActivity("Get Supplier Risk Monitoring Due for Review")
	public ResponseEntity<?> getRiskMonitoringDueForReview() {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getRiskMonitoringDueForReview(orgId));
	}

	@PutMapping("/{id}/update")
	@LogActivity("Update Supplier Risk Monitoring")
	public ResponseEntity<?> updateRiskMonitoring(@PathVariable Long id,
			@Valid @RequestBody SupplierRiskMonitoringUpdateRequest request) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.updateRiskMonitoring(id, request, orgId));
	}

	@DeleteMapping("/{id}")
	@LogActivity("Delete Supplier Risk Monitoring")
	public ResponseEntity<?> deleteRiskMonitoring(@PathVariable Long id) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		riskMonitoringService.deleteRiskMonitoring(id, orgId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/supplier/{supplierId}/summary")
	@LogActivity("Get Supplier Risk Summary")
	public ResponseEntity<?> getSupplierRiskSummary(@PathVariable Long supplierId) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getSupplierRiskSummary(supplierId, orgId));
	}

	@GetMapping("/supplier/{supplierId}/category/{riskCategory}")
	@LogActivity("Get Supplier Risk Monitoring by Category")
	public ResponseEntity<?> getRiskMonitoringByCategory(@PathVariable Long supplierId,
			@PathVariable String riskCategory) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getRiskMonitoringByCategory(supplierId, riskCategory, orgId));
	}

	@GetMapping("/all")
	@LogActivity("Get All Supplier Risk Monitoring")
	public ResponseEntity<?> getAllRiskMonitoring(
			@RequestParam(defaultValue = "0") int pageNo,
			@RequestParam(defaultValue = "20") int pageSize) {
		Long orgId = com.nexus.core.security.OrganizationContextHolder.getCurrentOrganizationId();
		return ResponseEntity.ok(riskMonitoringService.getAllRiskMonitoring(orgId, pageNo, pageSize));
	}
}