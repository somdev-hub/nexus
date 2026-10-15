package com.nexus.core.controller;

import java.sql.Date;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.FinanceService;
import com.nexus.core.service.interfaces.LogisticsBillingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/finance")
@RequiredArgsConstructor
public class FinanceController {

	private final FinanceService financeService;
	private final LogisticsBillingService billingService;

	@LogActivity("Retailer Finance Summary")
	@GetMapping("/retailer/summary")
	public ResponseEntity<?> retailerSummary(@RequestParam(required = false) String period) {
		return financeService.retailerSummary(period);
	}

	@LogActivity("Retailer Finance Transactions")
	@GetMapping("/retailer/transactions")
	public ResponseEntity<?> retailerTransactions(
			@RequestParam(required = false) String kind,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Date from,
			@RequestParam(required = false) Date to,
			@PageableDefault(size = 20) Pageable pageable) {
		return financeService.retailerTransactions(kind, status, search, from, to, pageable);
	}

	@LogActivity("Supplier Finance Summary")
	@GetMapping("/supplier/summary")
	public ResponseEntity<?> supplierSummary(@RequestParam(required = false) String period) {
		return financeService.supplierSummary(period);
	}

	@LogActivity("Supplier Finance Transactions")
	@GetMapping("/supplier/transactions")
	public ResponseEntity<?> supplierTransactions(
			@RequestParam(required = false) String kind,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Date from,
			@RequestParam(required = false) Date to,
			@PageableDefault(size = 20) Pageable pageable) {
		return financeService.supplierTransactions(kind, status, search, from, to, pageable);
	}

	@LogActivity("Logistics Finance Summary")
	@GetMapping("/logistics/summary")
	public ResponseEntity<?> logisticsSummary(@RequestParam(required = false) String period) {
		return financeService.logisticsSummary(period);
	}

	@LogActivity("Logistics Finance Transactions")
	@GetMapping("/logistics/transactions")
	public ResponseEntity<?> logisticsTransactions(
			@RequestParam(required = false) String kind,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Date from,
			@RequestParam(required = false) Date to,
			@PageableDefault(size = 20) Pageable pageable) {
		return financeService.logisticsTransactions(kind, status, search, from, to, pageable);
	}

	@LogActivity("Open Logistics Trips")
	@GetMapping("/billing/open-trips")
	public ResponseEntity<?> openTrips(@RequestParam Long partnershipId) {
		return billingService.getOpenTrips(partnershipId);
	}

	@LogActivity("Logistics Statements")
	@GetMapping("/billing/statements")
	public ResponseEntity<?> statements(@RequestParam Long partnershipId) {
		return billingService.getStatements(partnershipId);
	}

	@LogActivity("Create Logistics Pay Quote")
	@PostMapping("/billing/quotes")
	public ResponseEntity<?> createQuote(@RequestBody Map<String, Object> body) {
		return billingService.createBatchQuote(body);
	}

	@LogActivity("Get Logistics Pay Quote")
	@GetMapping("/billing/quotes/{quoteRef}")
	public ResponseEntity<?> getQuote(@PathVariable String quoteRef) {
		return billingService.getQuote(quoteRef);
	}

	@LogActivity("Mark Logistics Quote Paid")
	@PostMapping("/billing/mark-paid")
	public ResponseEntity<?> markPaid(@RequestBody Map<String, Object> body) {
		return billingService.markPaid(body);
	}
}
