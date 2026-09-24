package com.nexus.core.service;

import org.springframework.http.ResponseEntity;

public interface SupplierAccountHealthService {

    ResponseEntity<?> getAccountHealth(Long buyerOrgId);

    ResponseEntity<?> getAllAccountHealths();

    ResponseEntity<?> getHealthSummary();
}
