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
import com.nexus.core.payload.LogisticsPartnershipQuotationDto;
import com.nexus.core.service.interfaces.LogisticsPartnershipQuotationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/logistics/partnerships")
@RequiredArgsConstructor
public class LogisticsPartnershipController {

    private final LogisticsPartnershipQuotationService quotationService;

    @PostMapping("/quotations")
    @LogActivity("Raise Logistics Partnership Quotation")
    public ResponseEntity<?> createQuotation(@Valid @RequestBody LogisticsPartnershipQuotationDto dto) {
        return quotationService.createQuotation(dto);
    }

    @GetMapping("/quotations")
    @LogActivity("Get Logistics Partnership Quotations")
    public ResponseEntity<?> getMyQuotations(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotationService.getMyQuotations(status, pageable);
    }

    @GetMapping("/quotations/{id}")
    @LogActivity("Get Logistics Partnership Quotation")
    public ResponseEntity<?> getQuotation(@PathVariable Long id) {
        return quotationService.getQuotation(id);
    }

    @GetMapping("/{id}/routes")
    @LogActivity("Get Partnership Private Routes")
    public ResponseEntity<?> getPartnershipRoutes(@PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable) {
        return quotationService.getPartnershipRoutes(id, pageable);
    }

    @PostMapping("/{id}/activate")
    @LogActivity("Activate Logistics Partnership")
    public ResponseEntity<?> activatePartnership(@PathVariable Long id) {
        return quotationService.activatePartnership(id);
    }

    @PostMapping("/{id}/terminate")
    @LogActivity("Terminate Logistics Partnership")
    public ResponseEntity<?> terminatePartnership(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        return quotationService.terminateAsLogistics(id, body);
    }
}
