package com.nexus.core.model.entities;

import java.sql.Timestamp;

import com.nexus.core.model.enums.PartnershipQuotationStatus;

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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_logistics_partnership_quotations", schema = "core")
public class LogisticsPartnershipQuotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quotation_id")
    private Long quotationId;

    @Column(name = "quotation_number", unique = true)
    private String quotationNumber;

    // The supplier's long-term proposal this quotation answers.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", referencedColumnName = "invitation_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PartnershipInvitation invitation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    // Created (DRAFT) when the first quotation is submitted.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id", referencedColumnName = "partnership_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Partnership partnership;

    @Enumerated(EnumType.STRING)
    private PartnershipQuotationStatus status = PartnershipQuotationStatus.DRAFT;

    // Agreed route lines: JSON array of
    // {fromLane, toLane, capacity, capacityUnit, unitPrice, currency,
    //  periodStart, periodEnd}. Subset of the requested routes allowed.
    @Column(name = "route_lines_json", columnDefinition = "TEXT")
    private String routeLinesJson;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "currency")
    private String currency = "USD";

    @Column(name = "validity_start")
    private Timestamp validityStart;

    @Column(name = "validity_end")
    private Timestamp validityEnd;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "responded_at")
    private Timestamp respondedAt;
}
