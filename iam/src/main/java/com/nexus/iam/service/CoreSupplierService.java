package com.nexus.iam.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface CoreSupplierService {

    // Catalog
    ResponseEntity<?> createCatalog(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getCatalog(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllCatalogs(String authToken, String orgId, Pageable pageable, String status, String category, String family, String accessLevel, Boolean isPublished, String search);
    ResponseEntity<?> updateCatalog(Long id, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> transitionCatalogStatus(Long id, String newStatus, Map<String, Object> params, String authToken, String orgId);
    ResponseEntity<?> deleteCatalog(Long id, String authToken, String orgId);
    ResponseEntity<?> getCatalogSummary(String authToken, String orgId);

    // Variant
    ResponseEntity<?> createVariant(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getVariant(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllVariants(String authToken, String orgId, Pageable pageable, Long catalogId, String variantType, Long bomMaterialId);
    ResponseEntity<?> updateVariant(Long id, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> deleteVariant(Long id, String authToken, String orgId);

    // Price Tier
    ResponseEntity<?> createPriceTier(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getPriceTier(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllPriceTiers(String authToken, String orgId, Pageable pageable, Long catalogId, String customerSegment, Long contractId);
    ResponseEntity<?> updatePriceTier(Long id, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> deletePriceTier(Long id, String authToken, String orgId);
    ResponseEntity<?> getPriceForQuantity(Long catalogId, Double quantity, String customerSegment, String authToken, String orgId);

    // Digital Asset
    ResponseEntity<?> createDigitalAsset(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getDigitalAsset(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllDigitalAssets(String authToken, String orgId, Pageable pageable, Long catalogId, String assetType);
    ResponseEntity<?> deleteDigitalAsset(Long id, String authToken, String orgId);

    // Capacity
    ResponseEntity<?> createCapacity(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getCapacity(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllCapacities(String authToken, String orgId, Pageable pageable, String productLine, String shift);
    ResponseEntity<?> updateCapacity(Long id, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> deleteCapacity(Long id, String authToken, String orgId);
    ResponseEntity<?> getCapacitySummary(String authToken, String orgId);

    // ATP
    ResponseEntity<?> getAtpForCatalog(Long catalogId, Double quantity, String authToken, String orgId);
    ResponseEntity<?> getAtpForProductLine(String productLine, String authToken, String orgId);

    // Orders
    ResponseEntity<?> getSupplierOrder(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllSupplierOrders(String authToken, String orgId, Pageable pageable, String status, String poNumber, Long buyerOrgId);
    ResponseEntity<?> acknowledgeOrder(Long id, String authToken, String orgId, String confirmedDeliveryDate, String notes);
    ResponseEntity<?> updateOrderFulfillment(Long id, Map<String, Object> updates, String authToken, String orgId);
    ResponseEntity<?> createPartialShipment(Long purchaseOrderId, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getOrderSummary(String authToken, String orgId);

    // Quality Certificate
    ResponseEntity<?> createQualityCert(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getQualityCert(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllQualityCerts(String authToken, String orgId, Pageable pageable, Long poId, Long catalogId, String certType);
    ResponseEntity<?> deleteQualityCert(Long id, String authToken, String orgId);

    // Quotation
    ResponseEntity<?> createQuotation(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getQuotation(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllQuotations(String authToken, String orgId, Pageable pageable, String status, Long buyerOrgId, String quotationNumber);
    ResponseEntity<?> transitionQuotation(Long id, String newStatus, Map<String, Object> params, String authToken, String orgId);
    ResponseEntity<?> convertQuotation(Long id, String authToken, String orgId);
    ResponseEntity<?> deleteQuotation(Long id, String authToken, String orgId);
    ResponseEntity<?> getQuotationSummary(String authToken, String orgId);

    // Forecast
    ResponseEntity<?> createForecast(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getForecast(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllForecasts(String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> updateForecast(Long id, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> transitionForecast(Long id, String newStatus, String authToken, String orgId);
    ResponseEntity<?> deleteForecast(Long id, String authToken, String orgId);

    // Customer Portal
    ResponseEntity<?> getCustomerOrders(Long buyerOrgId, String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> getCustomerSummary(Long buyerOrgId, String authToken, String orgId);

    // Account Health
    ResponseEntity<?> getAccountHealth(Long buyerOrgId, String authToken, String orgId);
    ResponseEntity<?> getAllAccountHealth(String authToken, String orgId);
    ResponseEntity<?> getAccountHealthSummary(String authToken, String orgId);

    // Consignment
    ResponseEntity<?> createConsignment(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getConsignment(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllConsignments(String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> adjustConsignment(Long id, Double quantity, String reason, String authToken, String orgId);
    ResponseEntity<?> deleteConsignment(Long id, String authToken, String orgId);
    ResponseEntity<?> getConsignmentSummary(String authToken, String orgId);

    // VMI
    ResponseEntity<?> createVmi(Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> getVmi(Long id, String authToken, String orgId);
    ResponseEntity<?> getAllVmi(String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> updateVmi(Long id, Map<String, Object> dto, String authToken, String orgId);
    ResponseEntity<?> deleteVmi(Long id, String authToken, String orgId);
    ResponseEntity<?> triggerVmiReplenish(Long id, String authToken, String orgId);
    ResponseEntity<?> getVmiSuggestions(String authToken, String orgId);

    // Analytics
    ResponseEntity<?> getSupplierDashboard(String authToken, String orgId);

    // Partnership Invitations (mirrors retailer invitation routes, same core URLs)
    ResponseEntity<?> createPartnershipInvitation(Map<String, Object> invitationDto, String authToken, String orgId);
    ResponseEntity<?> respondToPartnershipInvitation(Long id, Map<String, Object> responseDto, String authToken, String orgId);
    ResponseEntity<?> getPartnershipInvitation(Long id, String authToken, String orgId);
    ResponseEntity<?> getSentPartnershipInvitations(String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> getReceivedPartnershipInvitations(String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> getPendingPartnershipInvitations(String authToken, String orgId, Pageable pageable);
    ResponseEntity<?> withdrawPartnershipInvitation(Long id, String authToken, String orgId);

    // ASN / POD passthroughs (Core P1)
    ResponseEntity<?> getAsnByPurchaseOrder(Long poId, String authToken, String orgId);
    ResponseEntity<?> getShipmentPod(Long shipmentId, String authToken, String orgId);

    // Partnerships visible to this org (primary OR secondary side)
    ResponseEntity<?> getAllPartnerships(String authToken, String orgId, Pageable pageable);

    ResponseEntity<?> updatePartnership(Long id, Map<String, Object> partnershipDto, String authToken,
            String orgId);

    ResponseEntity<?> updatePartnershipStatus(Long id, String status, String authToken, String orgId);
}
