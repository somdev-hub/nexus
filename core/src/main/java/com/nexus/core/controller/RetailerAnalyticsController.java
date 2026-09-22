package com.nexus.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.RetailerAnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/analytics/retailer")
@RequiredArgsConstructor
public class RetailerAnalyticsController {

    private final RetailerAnalyticsService retailerAnalyticsService;

    @GetMapping("/dashboard")
    @LogActivity("Get Retailer Executive Dashboard")
    public ResponseEntity<?> getExecutiveDashboard() {
        return retailerAnalyticsService.getExecutiveDashboard();
    }

    /**
     * Consolidated spend analytics – single endpoint with grouping.
     * Supports FR-AN-002.
     */
    @GetMapping("/spend")
    @LogActivity("Get Retailer Spend Analytics")
    public ResponseEntity<?> getSpendAnalytics(
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String period) {
        return retailerAnalyticsService.getSpendAnalytics(groupBy, period);
    }

    @GetMapping("/visibility/{purchaseOrderId}")
    @LogActivity("Get Supply Chain Visibility")
    public ResponseEntity<?> getSupplyChainVisibility(@PathVariable Long purchaseOrderId) {
        return retailerAnalyticsService.getSupplyChainVisibility(purchaseOrderId);
    }
}
