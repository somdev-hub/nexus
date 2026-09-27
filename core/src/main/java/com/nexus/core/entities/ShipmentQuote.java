package com.nexus.core.entities;

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

import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_shipment_quotes", schema = "core")
public class ShipmentQuote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quote_id")
    private Long quoteId;

    @Column(name = "quote_number", unique = true, nullable = false)
    private String quoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Enumerated(EnumType.STRING)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(name = "base_rate", precision = 19, scale = 4)
    private BigDecimal baseRate = BigDecimal.ZERO;

    @Column(name = "fuel_surcharge", precision = 19, scale = 4)
    private BigDecimal fuelSurcharge = BigDecimal.ZERO;

    @Column(name = "accessorial_charges", precision = 19, scale = 4)
    private BigDecimal accessorialCharges = BigDecimal.ZERO;

    @Column(name = "accessorial_details", columnDefinition = "TEXT")
    private String accessorialDetails;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String currency = "USD";

    @Column(name = "valid_until")
    private Date validUntil;

    private String notes;

    @Version
    private Long version = 0L;
}
