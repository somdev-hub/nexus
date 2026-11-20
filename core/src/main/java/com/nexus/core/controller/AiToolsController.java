package com.nexus.core.controller;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.core.annotation.LogActivity;
import com.nexus.core.service.interfaces.ProductService;
import com.nexus.core.service.interfaces.PurchaseOrderService;
import com.nexus.core.service.interfaces.ShipmentService;
import com.nexus.core.service.interfaces.StockService;
import com.nexus.core.service.interfaces.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI Tools Controller - Provides AI-friendly endpoints for NexusBuddy tool calling.
 * 
 * These endpoints are designed to be called by the NexusBuddy microservice when
 * executing tools. They accept orgId as a query parameter for org-scoping.
 * 
 * Base path: /core/ai-tools
 * 
 * Security: These endpoints are permitAll (no JWT required) but require orgId param.
 */
@RestController
@RequestMapping("/core/ai-tools")
@RequiredArgsConstructor
@Slf4j
public class AiToolsController {

    private final ProductService productService;
    private final StockService stockService;
    private final PurchaseOrderService purchaseOrderService;
    private final ShipmentService shipmentService;
    private final SupplierService supplierService;

    // ============================================
    // Product Tools
    // ============================================

    @GetMapping("/products")
    @LogActivity("AI Tool: List Products")
    public ResponseEntity<?> listProducts(
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "20") int limit) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
            return productService.getAllProductsByOrgId(orgId, pageable);
        } catch (Exception e) {
            log.error("AI Tool listProducts failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/products/{id}")
    @LogActivity("AI Tool: Get Product Details")
    public ResponseEntity<?> getProductDetails(
            @PathVariable Long id,
            @RequestParam(required = false) Long orgId) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            return productService.getProductByIdAndOrg(id, orgId);
        } catch (Exception e) {
            log.error("AI Tool getProductDetails failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // Stock Tools
    // ============================================

    @GetMapping("/stock")
    @LogActivity("AI Tool: Get Stock Levels")
    public ResponseEntity<?> getStockLevels(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Boolean belowReorderPoint,
            @RequestParam(defaultValue = "20") int limit) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
            return stockService.getAllStockByOrgId(warehouseId, null, belowReorderPoint, null, pageable);
        } catch (Exception e) {
            log.error("AI Tool getStockLevels failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // Purchase Order Tools
    // ============================================

    @GetMapping("/purchase-orders")
    @LogActivity("AI Tool: Get Purchase Orders")
    public ResponseEntity<?> getPurchaseOrders(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
            return purchaseOrderService.getAllPurchaseOrders(status, pageable);
        } catch (Exception e) {
            log.error("AI Tool getPurchaseOrders failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/purchase-orders/{id}")
    @LogActivity("AI Tool: Get Purchase Order Details")
    public ResponseEntity<?> getPurchaseOrderDetails(
            @PathVariable Long id,
            @RequestParam(required = false) Long orgId) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            return purchaseOrderService.getPurchaseOrderById(id);
        } catch (Exception e) {
            log.error("AI Tool getPurchaseOrderDetails failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // Supplier Tools
    // ============================================

    @GetMapping("/suppliers")
    @LogActivity("AI Tool: Get Suppliers")
    public ResponseEntity<?> getSuppliers(
            @RequestParam(required = false) Long orgId,
            @RequestParam(defaultValue = "20") int limit) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
            return supplierService.getAllSuppliers(null, null, null, null, null, pageable);
        } catch (Exception e) {
            log.error("AI Tool getSuppliers failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/suppliers/{id}")
    @LogActivity("AI Tool: Get Supplier Details")
    public ResponseEntity<?> getSupplierDetails(
            @PathVariable Long id,
            @RequestParam(required = false) Long orgId) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            // Set organization context attribute for service layer
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            return supplierService.getSupplierById(id);
        } catch (Exception e) {
            log.error("AI Tool getSupplierDetails failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // Shipment Tools
    // ============================================

    @GetMapping("/shipments")
    @LogActivity("AI Tool: Get Shipments")
    public ResponseEntity<?> getShipments(
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 50));
            org.springframework.data.domain.Page<com.nexus.core.dto.ShipmentDto> shipments;
            if (status != null && !status.isBlank()) {
                try {
                    com.nexus.core.model.enums.ShipmentStatus shipmentStatus =
                            com.nexus.core.model.enums.ShipmentStatus.valueOf(status.toUpperCase());
                    shipments = shipmentService.getShipmentsByStatus(shipmentStatus, pageable);
                } catch (IllegalArgumentException e) {
                    shipments = shipmentService.getAllShipments(pageable);
                }
            } else {
                shipments = shipmentService.getAllShipments(pageable);
            }
            return ResponseEntity.ok(shipments);
        } catch (Exception e) {
            log.error("AI Tool getShipments failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipments/{id}")
    @LogActivity("AI Tool: Get Shipment Details")
    public ResponseEntity<?> getShipmentDetails(
            @PathVariable Long id,
            @RequestParam(required = false) Long orgId) {
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "orgId is required"));
        }
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.getRequest().setAttribute(
                        com.nexus.core.security.OrganizationContextFilter.ORGANIZATION_CONTEXT_ATTRIBUTE,
                        new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(
                                java.util.Map.of("orgId", orgId, "id", orgId)));
            }
            java.util.Optional<com.nexus.core.dto.ShipmentDto> shipment = shipmentService.getShipmentById(id);
            if (shipment.isPresent()) {
                return ResponseEntity.ok(shipment.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Shipment not found"));
        } catch (Exception e) {
            log.error("AI Tool getShipmentDetails failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}