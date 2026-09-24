package com.nexus.core.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface SupplierCustomerPortalService {

    ResponseEntity<?> getCustomerOrders(Long buyerOrgId, String status, Pageable pageable);

    ResponseEntity<?> getCustomerInvoices(Long buyerOrgId, String status, Pageable pageable);

    ResponseEntity<?> getCustomerShipments(Long buyerOrgId, String status, Pageable pageable);

    ResponseEntity<?> getCustomerReturns(Pageable pageable);

    ResponseEntity<?> getCustomerSummary(Long buyerOrgId);
}
