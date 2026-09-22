package com.nexus.core.payload;

import com.nexus.core.entities.FreightInvoiceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * DTO for FreightInvoice (FR-FIN-005/006/007, FR-RET-033).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreightInvoiceDto {

    private Long freightInvoiceId;

    private String invoiceNumber;

    @NotNull(message = "Shipment ID is required")
    private Long shipmentId;

    private String shipmentNumber;

    private Long retailerOrgId;

    private Long logisticsOrgId;

    private FreightInvoiceStatus status;

    private String shipmentMode;

    @DecimalMin(value = "0.0", message = "Freight cost must be non-negative")
    private BigDecimal freightCost;

    private BigDecimal accessorialCharges;

    private BigDecimal fuelSurcharge;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String currency;

    private Date issuedDate;

    private Date dueDate;

    private Timestamp paidDate;

    private String pmsReferenceId;

    private String pmsStatus;

    private String discrepancyReason;

    private BigDecimal discrepancyAmount;

    private String notes;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
