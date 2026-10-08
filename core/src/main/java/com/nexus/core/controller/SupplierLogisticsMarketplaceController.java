package com.nexus.core.controller;

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
import com.nexus.core.payload.PartnershipInvitationDto;
import com.nexus.core.service.interfaces.LogisticsPartnershipQuotationService;
import com.nexus.core.service.interfaces.SupplierLogisticsMarketplaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/supplier/logistics-marketplace")
@RequiredArgsConstructor
public class SupplierLogisticsMarketplaceController {

    private final SupplierLogisticsMarketplaceService marketplaceService;
    private final LogisticsPartnershipQuotationService quotationService;

    @GetMapping("/available")
    @LogActivity("Browse Logistics Marketplace")
    public ResponseEntity<?> getAvailableLogistics(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return marketplaceService.getAvailableLogistics(search, pageable);
    }

    @GetMapping("/partnerships")
    @LogActivity("Get Supplier Logistics Partnerships")
    public ResponseEntity<?> getMyLogisticsPartnerships(
            @PageableDefault(size = 20) Pageable pageable) {
        return marketplaceService.getMyLogisticsPartnerships(pageable);
    }

    @PostMapping("/proposals")
    @LogActivity("Propose Supplier Logistics Partnership")
    public ResponseEntity<?> createProposal(@Valid @RequestBody PartnershipInvitationDto invitationDto) {
        return marketplaceService.createProposal(invitationDto);
    }

    @GetMapping("/shipments")
    @LogActivity("Get Supplier Shipments")
    public ResponseEntity<?> getMyShipments(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return marketplaceService.getMyShipments(status, pageable);
    }

    @PostMapping("/shipments/{shipmentId}/handover")
    @LogActivity("Handover Shipment to Logistics")
    public ResponseEntity<?> handoverShipment(@PathVariable Long shipmentId,
            @RequestBody Map<String, Object> body) {
        return marketplaceService.handoverShipment(shipmentId, body);
    }

    @GetMapping("/quotations")
    @LogActivity("Get Supplier Logistics Quotations")
    public ResponseEntity<?> getMyQuotations(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotationService.getMyQuotations(status, pageable);
    }

    @GetMapping("/quotations/{id}")
    @LogActivity("Get Supplier Logistics Quotation")
    public ResponseEntity<?> getQuotation(@PathVariable Long id) {
        return quotationService.getQuotation(id);
    }

    @PostMapping("/quotations/{id}/respond")
    @LogActivity("Respond to Logistics Quotation")
    public ResponseEntity<?> respondToQuotation(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return quotationService.respondToQuotation(id, body);
    }

    @GetMapping("/my-routes")
    @LogActivity("Get Supplier Private Routes")
    public ResponseEntity<?> getMyPrivateRoutes(
            @PageableDefault(size = 50) Pageable pageable) {
        return quotationService.getMyPrivateRoutes(pageable);
    }

    @PostMapping("/partnerships/{id}/terminate")
    @LogActivity("Terminate Supplier Logistics Partnership")
    public ResponseEntity<?> terminatePartnership(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        return quotationService.terminateAsSupplier(id, body);
    }
}
