package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

public interface RetailerAnalyticsService {

    ResponseEntity<?> getExecutiveDashboard();

    ResponseEntity<?> getSpendAnalytics(String groupBy, String period);

    ResponseEntity<?> getSupplyChainVisibility(Long purchaseOrderId);

    ResponseEntity<?> getSpendBySupplier();

    ResponseEntity<?> getSpendByCategory();
}
