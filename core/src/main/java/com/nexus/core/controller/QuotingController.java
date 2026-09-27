package com.nexus.core.controller;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.payload.FreightRateDto;
import com.nexus.core.payload.ShipmentQuoteDto;
import com.nexus.core.service.QuotingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.Map;

@RestController
@RequestMapping("/core/logistics/quoting")
@RequiredArgsConstructor
public class QuotingController {

    private final QuotingService quotingService;

    @GetMapping("/load-board")
    @LogActivity("Get Load Board")
    public ResponseEntity<?> getLoadBoard(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotingService.getLoadBoard(mode, search, pageable);
    }

    @PostMapping("/quotes/create")
    @LogActivity("Create Shipment Quote")
    public ResponseEntity<?> createQuote(@Valid @RequestBody ShipmentQuoteDto dto) {
        return quotingService.createQuote(dto);
    }

    @GetMapping("/quotes/{id}")
    @LogActivity("Get Shipment Quote")
    public ResponseEntity<?> getQuote(@PathVariable Long id) {
        return quotingService.getQuote(id);
    }

    @GetMapping("/quotes/all")
    @LogActivity("Get All Shipment Quotes")
    public ResponseEntity<?> getAllQuotes(
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotingService.getAllQuotes(shipmentId, status, search, pageable);
    }

    @PutMapping("/quotes/{id}/update")
    @LogActivity("Update Shipment Quote")
    public ResponseEntity<?> updateQuote(@PathVariable Long id, @RequestBody ShipmentQuoteDto dto) {
        return quotingService.updateQuote(id, dto);
    }

    @PutMapping("/quotes/{id}/status")
    @LogActivity("Transition Shipment Quote Status")
    public ResponseEntity<?> transitionQuoteStatus(@PathVariable Long id,
            @RequestParam String newStatus,
            @RequestBody(required = false) Map<String, Object> params) {
        return quotingService.transitionQuoteStatus(id, newStatus, params);
    }

    @DeleteMapping("/quotes/{id}")
    @LogActivity("Delete Shipment Quote")
    public ResponseEntity<?> deleteQuote(@PathVariable Long id) {
        return quotingService.deleteQuote(id);
    }

    @PostMapping("/rates/create")
    @LogActivity("Create Freight Rate")
    public ResponseEntity<?> createRate(@Valid @RequestBody FreightRateDto dto) {
        return quotingService.createRate(dto);
    }

    @GetMapping("/rates/{id}")
    @LogActivity("Get Freight Rate")
    public ResponseEntity<?> getRate(@PathVariable Long id) {
        return quotingService.getRate(id);
    }

    @GetMapping("/rates/all")
    @LogActivity("Get All Freight Rates")
    public ResponseEntity<?> getAllRates(
            @RequestParam(required = false) String rateType,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return quotingService.getAllRates(rateType, equipmentType, search, pageable);
    }

    @PutMapping("/rates/{id}/update")
    @LogActivity("Update Freight Rate")
    public ResponseEntity<?> updateRate(@PathVariable Long id, @RequestBody FreightRateDto dto) {
        return quotingService.updateRate(id, dto);
    }

    @DeleteMapping("/rates/{id}")
    @LogActivity("Delete Freight Rate")
    public ResponseEntity<?> deleteRate(@PathVariable Long id) {
        return quotingService.deleteRate(id);
    }
}
