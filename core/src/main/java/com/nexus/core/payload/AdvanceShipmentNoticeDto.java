package com.nexus.core.payload;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceShipmentNoticeDto {

    private Long asnId;

    private String asnNumber;

    private Long purchaseOrderId;

    private String poNumber;

    private Long buyerOrgId;

    private Long supplierOrgId;

    private Long shipmentId;

    private String status;

    private String notes;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    // --- Dispatch detail (the ASN's own purpose: what is physically on
    // this truck). The PO carries the commercial order (what was asked
    // for, terms, approval); the ASN carries the dispatch snapshot (what
    // was actually sent, how, and when it lands). Sourced from the linked
    // shipment + PO totals at read time; nothing duplicated at rest. ---

    /** Carrier shipment reference, e.g. SHP-…. */
    private String shipmentNumber;

    private String shipmentStatus;

    private String trackingNumber;

    private String carrierName;

    private String logisticsOrgName;

    private String pickupLocation;

    private String deliveryLocation;

    private String pickupDate;

    private String deliveryDate;

    /** Total units ordered across the PO lines. */
    private Double orderedQuantity;

    /** Units loaded on this truck (ordered total minus backorder). */
    private Double shippedQuantity;

    /** Units still pending after this dispatch. */
    private Double backorderedQuantity;

    private Integer totalPackages;

    private Double totalWeight;

    private Double totalVolume;

    private String estimatedArrival;
}
