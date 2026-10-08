package com.nexus.core.service.interfaces;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.LogisticsPartnershipQuotationDto;

public interface LogisticsPartnershipQuotationService {

    // Logistics: raise a counter-quotation against a long-term proposal.
    ResponseEntity<?> createQuotation(LogisticsPartnershipQuotationDto dto);

    // Either side: own quotations / one quotation.
    ResponseEntity<?> getMyQuotations(String status, Pageable pageable);
    ResponseEntity<?> getQuotation(Long id);

    // Supplier: accept / reject a submitted quotation.
    ResponseEntity<?> respondToQuotation(Long id, Map<String, Object> body);

    // Logistics: activate a long-term partnership once every agreed route
    // has a private capacity. Only then does it come into effect.
    ResponseEntity<?> activatePartnership(Long partnershipId);

    // Supplier: private route capacities under own long-term partnerships.
    ResponseEntity<?> getMyPrivateRoutes(Pageable pageable);

    // Logistics: private route capacities under one partnership.
    ResponseEntity<?> getPartnershipRoutes(Long partnershipId, Pageable pageable);

    // Terminate from either side, with shipment side-effects.
    ResponseEntity<?> terminateAsSupplier(Long partnershipId, Map<String, Object> body);
    ResponseEntity<?> terminateAsLogistics(Long partnershipId, Map<String, Object> body);
}
