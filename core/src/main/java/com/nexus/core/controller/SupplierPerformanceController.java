package com.nexus.core.controller;

import java.sql.Date;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import com.nexus.core.payload.SupplierPerformanceDto;
import com.nexus.core.service.SupplierPerformanceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier-performance")
@RequiredArgsConstructor
public class SupplierPerformanceController {

	private final SupplierPerformanceService performanceService;

	@PostMapping("/create")
	@LogActivity("Create Supplier Performance Record")
	public ResponseEntity<?> createPerformanceRecord(@Valid @RequestBody SupplierPerformanceDto performanceDto) {
		return performanceService.createPerformanceRecord(performanceDto);
	}

	@GetMapping("/{id}")
	@LogActivity("Get Supplier Performance by ID")
	public ResponseEntity<?> getPerformanceById(@PathVariable Long id) {
		return performanceService.getPerformanceById(id);
	}

	@GetMapping("/supplier/{supplierId}")
	@LogActivity("Get Supplier Performance by Supplier")
	public ResponseEntity<?> getPerformanceBySupplier(@PathVariable Long supplierId,
			@PageableDefault(size = 20) Pageable pageable) {
		return performanceService.getPerformanceBySupplier(supplierId, pageable);
	}

	@GetMapping("/account")
	@LogActivity("Get Supplier Performance by Account")
	public ResponseEntity<?> getPerformanceByAccount(@PageableDefault(size = 20) Pageable pageable) {
		return performanceService.getPerformanceByAccount(
				com.nexus.core.security.OrganizationContextHolder.requireOrganizationId(), pageable);
	}

	@GetMapping("/account/period")
	@LogActivity("Get Supplier Performance by Account and Period")
	public ResponseEntity<?> getPerformanceByAccountAndPeriod(
			@RequestParam Date startDate,
			@RequestParam Date endDate,
			@PageableDefault(size = 20) Pageable pageable) {
		return performanceService.getPerformanceByAccountAndPeriod(
				com.nexus.core.security.OrganizationContextHolder.requireOrganizationId(), startDate, endDate,
				pageable);
	}

	@GetMapping("/supplier/{supplierId}/period")
	@LogActivity("Get Supplier Performance by Supplier and Period")
	public ResponseEntity<?> getPerformanceBySupplierAndPeriod(
			@PathVariable Long supplierId,
			@RequestParam Date startDate,
			@RequestParam Date endDate,
			@PageableDefault(size = 20) Pageable pageable) {
		return performanceService.getPerformanceBySupplierAndPeriod(supplierId, startDate, endDate, pageable);
	}

	@GetMapping("/account/tier/{tier}")
	@LogActivity("Get Supplier Performance by Account and Tier")
	public ResponseEntity<?> getPerformanceByAccountAndTier(
			@PathVariable String tier,
			@PageableDefault(size = 20) Pageable pageable) {
		return performanceService.getPerformanceByAccountAndTier(
				com.nexus.core.security.OrganizationContextHolder.requireOrganizationId(), tier, pageable);
	}

	@GetMapping("/supplier/{supplierId}/latest")
	@LogActivity("Get Latest Supplier Performance")
	public ResponseEntity<?> getLatestPerformanceBySupplier(@PathVariable Long supplierId) {
		return performanceService.getLatestPerformanceBySupplier(supplierId);
	}

	@GetMapping("/account/summary")
	@LogActivity("Get Supplier Performance Summary by Account")
	public ResponseEntity<?> getPerformanceSummaryByAccount() {
		return performanceService.getPerformanceSummaryByAccount(
				com.nexus.core.security.OrganizationContextHolder.requireOrganizationId());
	}

	@GetMapping("/supplier/{supplierId}/summary")
	@LogActivity("Get Supplier Performance Summary by Supplier")
	public ResponseEntity<?> getPerformanceSummaryBySupplier(@PathVariable Long supplierId) {
		return performanceService.getPerformanceSummaryBySupplier(supplierId);
	}

	@PostMapping("/calculate")
	@LogActivity("Calculate and Save Supplier Performance")
	public ResponseEntity<?> calculateAndSavePerformance(
			@RequestParam Long supplierId,
			@RequestParam Date startDate,
			@RequestParam Date endDate,
			@RequestParam String calculatedBy) {
		return performanceService.calculateAndSavePerformance(supplierId, startDate, endDate, calculatedBy);
	}

	@PutMapping("/{id}")
	@LogActivity("Update Supplier Performance Record")
	public ResponseEntity<?> updatePerformanceRecord(@PathVariable Long id,
			@Valid @RequestBody SupplierPerformanceDto performanceDto) {
		return performanceService.updatePerformanceRecord(id, performanceDto);
	}

	@DeleteMapping("/{id}")
	@LogActivity("Delete Supplier Performance Record")
	public ResponseEntity<?> deletePerformanceRecord(@PathVariable Long id) {
		return performanceService.deletePerformanceRecord(id);
	}
}