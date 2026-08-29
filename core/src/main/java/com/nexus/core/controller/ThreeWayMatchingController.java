package com.nexus.core.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.entities.PurchaseOrder;
import com.nexus.core.entities.Invoice;
import com.nexus.core.service.ThreeWayMatchingService;
import com.nexus.core.service.ThreeWayMatchingService.MatchingResult;
import com.nexus.core.repository.PurchaseOrderRepo;
import com.nexus.core.repository.InvoiceRepo;
import com.nexus.core.security.OrganizationContextHolder;
import com.nexus.core.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/three-way-match")
@RequiredArgsConstructor
public class ThreeWayMatchingController {

	private final ThreeWayMatchingService threeWayMatchingService;
	private final PurchaseOrderRepo purchaseOrderRepo;
	private final InvoiceRepo invoiceRepo;

	@GetMapping("/match/{purchaseOrderId}")
	@LogActivity("Perform Three-Way Match")
	public ResponseEntity<?> performThreeWayMatch(@PathVariable Long purchaseOrderId) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		PurchaseOrder po = purchaseOrderRepo.findByPurchaseOrderIdAndBuyerOrgId(purchaseOrderId, orgId)
				.orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "purchaseOrderId", purchaseOrderId));
		MatchingResult result = threeWayMatchingService.performThreeWayMatch(po);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/can-invoice/{purchaseOrderId}")
	@LogActivity("Check Can Invoice")
	public ResponseEntity<?> canInvoice(@PathVariable Long purchaseOrderId) {
		boolean canInvoice = threeWayMatchingService.canInvoice(purchaseOrderId);
		return ResponseEntity.ok(canInvoice);
	}

	@GetMapping("/summary/{purchaseOrderId}")
	@LogActivity("Get Matching Summary")
	public ResponseEntity<?> getMatchingSummary(@PathVariable Long purchaseOrderId) {
		Map<String, Object> summary = threeWayMatchingService.getMatchingSummary(purchaseOrderId);
		return ResponseEntity.ok(summary);
	}

	@PostMapping("/validate-invoice")
	@LogActivity("Validate Invoice Match")
	public ResponseEntity<?> validateInvoiceMatch(@RequestBody com.nexus.core.payload.InvoiceDto invoiceDto) {
		Long orgId = OrganizationContextHolder.requireOrganizationId();
		Invoice invoice = invoiceRepo.findByInvoiceIdAndPurchaseOrderBuyerOrgId(invoiceDto.getInvoiceId(), orgId);
		if (invoice == null) {
			throw new ResourceNotFoundException("Invoice", "invoiceId", invoiceDto.getInvoiceId());
		}
		MatchingResult result = threeWayMatchingService.validateInvoiceMatch(invoice);
		return ResponseEntity.ok(result);
	}
}