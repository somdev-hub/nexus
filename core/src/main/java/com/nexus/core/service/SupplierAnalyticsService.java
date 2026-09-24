package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

public interface SupplierAnalyticsService {

    ResponseEntity<?> getDashboard();

    ResponseEntity<?> getOrderAnalytics();

    ResponseEntity<?> getCapacityAnalytics();

    ResponseEntity<?> getCustomerAnalytics();
}
