package com.nexus.core.service.interfaces;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nexus.core.payload.PartnershipInvitationDto;

public interface SupplierLogisticsMarketplaceService {

    // All logistics routing capacities (lane + period + availability) for the
    // supplier marketplace screen.
    ResponseEntity<?> getAvailableLogistics(String search, Pageable pageable);

    // Supplier's own supplier-logistics partnerships (either side).
    ResponseEntity<?> getMyLogisticsPartnerships(Pageable pageable);

    // Supplier sends a SHORT_TERM or LONG_TERM partnership proposal to a
    // logistics org, optionally anchored to a routing-capacity period.
    ResponseEntity<?> createProposal(PartnershipInvitationDto invitationDto);

    // Supplier hands a prepared (DRAFT) shipment to a partnered logistics org.
    // Shipment moves DRAFT -> BOOKED with logisticsOrg + partnership recorded.
    ResponseEntity<?> handoverShipment(Long shipmentId, Map<String, Object> body);

    // Supplier's shipments (handover tracking).
    ResponseEntity<?> getMyShipments(String status, Pageable pageable);
}
