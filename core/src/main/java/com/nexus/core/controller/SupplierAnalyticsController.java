package com.nexus.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.SupplierAnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/core/analytics/supplier")
@RequiredArgsConstructor
public class SupplierAnalyticsController {

    private final SupplierAnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @LogActivity("Get Supplier Dashboard")
    public ResponseEntity<?> getDashboard() {
        return analyticsService.getDashboard();
    }

    @GetMapping("/orders")
    @LogActivity("Get Supplier Order Analytics")
    public ResponseEntity<?> getOrderAnalytics() {
        return analyticsService.getOrderAnalytics();
    }

    @GetMapping("/capacity")
    @LogActivity("Get Supplier Capacity Analytics")
    public ResponseEntity<?> getCapacityAnalytics() {
        return analyticsService.getCapacityAnalytics();
    }

    @GetMapping("/customers")
    @LogActivity("Get Supplier Customer Analytics")
    public ResponseEntity<?> getCustomerAnalytics() {
        return analyticsService.getCustomerAnalytics();
    }
}
