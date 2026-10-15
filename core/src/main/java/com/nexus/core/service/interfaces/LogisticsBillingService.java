package com.nexus.core.service.interfaces;

import java.util.Map;

import org.springframework.http.ResponseEntity;

public interface LogisticsBillingService {

	ResponseEntity<?> getOpenTrips(Long partnershipId);

	ResponseEntity<?> getStatements(Long partnershipId);

	ResponseEntity<?> createBatchQuote(Map<String, Object> body);

	ResponseEntity<?> getQuote(String quoteRef);

	ResponseEntity<?> markPaid(Map<String, Object> body);
}
