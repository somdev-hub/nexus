package com.nexus.iam.service;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/**
 * Core Logistics Service Interface
 * <p>
 * Defines logistics-specific Core module operations through IAM gateway.
 * All HTTP calls to Core module are handled here.
 */
public interface CoreLogisticsService {

    // Fleet assets
    ResponseEntity<?> createAsset(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getAsset(Long id, String auth, String org);
    ResponseEntity<?> getAllAssets(String auth, String org, Pageable p, String status, String assetType, String search);
    ResponseEntity<?> updateAsset(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> transitionAssetStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> deleteAsset(Long id, String auth, String org);
    ResponseEntity<?> getAssetSummary(String auth, String org);
    ResponseEntity<?> getAssetShipments(Long assetId, String auth, String org, Pageable p, String status, String from, String to);
    ResponseEntity<?> getAssetDrivers(Long assetId, String auth, String org);
    ResponseEntity<?> getAssetCurrentShipment(Long assetId, String auth, String org);

    // Drivers
    ResponseEntity<?> createDriver(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getDriver(Long id, String auth, String org);
    ResponseEntity<?> getAllDrivers(String auth, String org, Pageable p, String status, String search);
    ResponseEntity<?> updateDriver(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> transitionDriverStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> deleteDriver(Long id, String auth, String org);

    // Maintenance
    ResponseEntity<?> createMaintenance(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getMaintenance(Long id, String auth, String org);
    ResponseEntity<?> updateMaintenance(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getAllMaintenance(String auth, String org, Pageable p, Long assetId, String status, Boolean isBreakdown, String search);
    ResponseEntity<?> transitionMaintenanceStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> deleteMaintenance(Long id, String auth, String org);

    // Load board + quotes + rates
    ResponseEntity<?> getLoadBoard(String auth, String org, Pageable p, String mode, String search);
    ResponseEntity<?> createQuote(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getQuote(Long id, String auth, String org);
    ResponseEntity<?> getAllQuotes(String auth, String org, Pageable p, Long shipmentId, String status, String search);
    ResponseEntity<?> updateQuote(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> transitionQuoteStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> deleteQuote(Long id, String auth, String org);
    ResponseEntity<?> createRate(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getRate(Long id, String auth, String org);
    ResponseEntity<?> getAllRates(String auth, String org, Pageable p, String rateType, String equipmentType, String search);
    ResponseEntity<?> updateRate(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> deleteRate(Long id, String auth, String org);

    // Execution: booking assignment, ETA, POD, incidents
    ResponseEntity<?> assignBooking(Long shipmentId, Map<String, Object> body, String auth, String org);
    ResponseEntity<?> getShipmentPosition(Long shipmentId, String auth, String org);
    ResponseEntity<?> updateShipmentRoute(Long shipmentId, Map<String, Object> body, String auth, String org);
    ResponseEntity<?> transitionShipmentStatus(Long shipmentId, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> getShipmentEta(Long shipmentId, String auth, String org);
    ResponseEntity<?> capturePod(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> updatePod(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getPodByShipment(Long shipmentId, String auth, String org);
    ResponseEntity<?> getAllPods(String auth, String org, Pageable p, String search);
    ResponseEntity<?> reportIncident(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> updateIncident(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getIncident(Long id, String auth, String org);
    ResponseEntity<?> getAllIncidents(String auth, String org, Pageable p, Long shipmentId, String incidentType, String status, String search);
    ResponseEntity<?> transitionIncidentStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);

    // Operations: consolidation, capacity, payables, analytics
    ResponseEntity<?> createGroup(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getGroup(Long id, String auth, String org);
    ResponseEntity<?> getAllGroups(String auth, String org, Pageable p, String status, String search);
    ResponseEntity<?> updateGroup(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> transitionGroupStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> addShipmentsToGroup(Long id, Map<String, Object> body, String auth, String org);
    ResponseEntity<?> deleteGroup(Long id, String auth, String org);
    ResponseEntity<?> createCapacity(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getCapacity(Long id, String auth, String org);
    ResponseEntity<?> getAllCapacities(String auth, String org, Pageable p, String equipmentType, String search);
    ResponseEntity<?> updateCapacity(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> deleteCapacity(Long id, String auth, String org);
    ResponseEntity<?> createPayable(Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> updatePayable(Long id, Map<String, Object> dto, String auth, String org);
    ResponseEntity<?> getPayable(Long id, String auth, String org);
    ResponseEntity<?> getAllPayables(String auth, String org, Pageable p, Long shipmentId, String status, String search);
    ResponseEntity<?> transitionPayableStatus(Long id, String newStatus, Map<String, Object> params, String auth, String org);
    ResponseEntity<?> deletePayable(Long id, String auth, String org);
    ResponseEntity<?> getLogisticsDashboard(String auth, String org);

    // Partnership Invitations (mirrors retailer invitation routes, same core URLs)
    ResponseEntity<?> createPartnershipInvitation(Map<String, Object> invitationDto, String auth, String org);
    ResponseEntity<?> respondToPartnershipInvitation(Long id, Map<String, Object> responseDto, String auth, String org);
    ResponseEntity<?> getPartnershipInvitation(Long id, String auth, String org);
    ResponseEntity<?> getSentPartnershipInvitations(String auth, String org, Pageable p);
    ResponseEntity<?> getReceivedPartnershipInvitations(String auth, String org, Pageable p);
    ResponseEntity<?> getPendingPartnershipInvitations(String auth, String org, Pageable p);
    ResponseEntity<?> withdrawPartnershipInvitation(Long id, String auth, String org);

    // Logistics shipment list (Core GET /core/shipments/by-logistics-org)
    ResponseEntity<?> getMyShipments(String auth, String org, Pageable p);

    // Partnerships visible to this org (primary OR secondary side)
    ResponseEntity<?> getAllPartnerships(String auth, String org, Pageable p);

    ResponseEntity<?> updatePartnership(Long id, Map<String, Object> partnershipDto, String auth, String org);

    ResponseEntity<?> updatePartnershipStatus(Long id, String status, String auth, String org);

    // Freight invoice passthroughs (Core P1)
    ResponseEntity<?> getFreightInvoicesByLogisticsOrg(String auth, String org, Pageable p);
    ResponseEntity<?> getFreightInvoicesByShipment(Long shipmentId, String auth, String org);
}
