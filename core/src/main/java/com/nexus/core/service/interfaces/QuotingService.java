package com.nexus.core.service.interfaces;

import com.nexus.core.payload.FreightRateDto;
import com.nexus.core.payload.ShipmentQuoteDto;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface QuotingService {

    // Load board: shipments visible to logistics for bidding (uses Shipment entity, org-scoped)
    ResponseEntity<?> getLoadBoard(String mode, String search, Pageable pageable);
    // Own in-motion loads: picked up and heading to delivery.
    ResponseEntity<?> getPickedUpLoads(Pageable pageable);
    // Own finished loads: delivered (and closed) history.
    ResponseEntity<?> getDeliveredLoads(Pageable pageable);
    // Own shipments across the full lifecycle (execution tracking).
    ResponseEntity<?> getMyShipments(String search, Pageable pageable);

    // Quotes (FR-LOG-001/002 consolidated: bid + accept flow in single status API)
    ResponseEntity<?> createQuote(ShipmentQuoteDto dto);
    ResponseEntity<?> getQuote(Long id);
    ResponseEntity<?> getAllQuotes(Long shipmentId, String status, String search, Pageable pageable);
    ResponseEntity<?> updateQuote(Long id, ShipmentQuoteDto dto);
    ResponseEntity<?> transitionQuoteStatus(Long id, String newStatus, Map<String, Object> params);
    ResponseEntity<?> deleteQuote(Long id);

    // Rates (FR-LOG-031 consolidated)
    ResponseEntity<?> createRate(FreightRateDto dto);
    ResponseEntity<?> getRate(Long id);
    ResponseEntity<?> getAllRates(String rateType, String equipmentType, String search, Pageable pageable);
    ResponseEntity<?> updateRate(Long id, FreightRateDto dto);
    ResponseEntity<?> deleteRate(Long id);
}
