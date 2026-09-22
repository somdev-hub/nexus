package com.nexus.core.entities;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * FreightInvoice entity for RET-P07 Financial Settlement.
 * Supports FR-FIN-005, FR-FIN-006, FR-FIN-007 and FR-RET-033.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_freight_invoices", schema = "core")
public class FreightInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "freight_invoice_id")
    private Long freightInvoiceId;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account retailerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FreightInvoiceStatus status = FreightInvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_mode")
    private ShipmentMode shipmentMode;

    @Column(name = "freight_cost", precision = 19, scale = 4)
    private BigDecimal freightCost;

    @Column(name = "accessorial_charges", precision = 19, scale = 4)
    private BigDecimal accessorialCharges = BigDecimal.ZERO;

    @Column(name = "fuel_surcharge", precision = 19, scale = 4)
    private BigDecimal fuelSurcharge = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency")
    private String currency = "USD";

    @Column(name = "issued_date")
    private Date issuedDate;

    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "paid_date")
    private Timestamp paidDate;

    @Column(name = "pms_reference_id")
    private String pmsReferenceId;

    @Column(name = "pms_status")
    private String pmsStatus;

    @Column(name = "discrepancy_reason")
    private String discrepancyReason;

    @Column(name = "discrepancy_amount", precision = 19, scale = 4)
    private BigDecimal discrepancyAmount;

    @Column(name = "notes")
    private String notes;

    @Version
    private Long version = 0L;
}
