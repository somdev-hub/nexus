package com.nexus.core.service.interfaces;

import org.springframework.http.ResponseEntity;

public interface SupplierAccountHealthService {

    ResponseEntity<?> getAccountHealth(Long buyerOrgId);

    ResponseEntity<?> getAllAccountHealths();

    ResponseEntity<?> getHealthSummary();
}
