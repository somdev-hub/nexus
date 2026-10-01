package com.nexus.core.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "t_asn", schema = "core")
public class AdvanceShipmentNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asn_id")
    private Long asnId;

    @Column(name = "asn_number", unique = true)
    private String asnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", referencedColumnName = "purchase_order_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account buyerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "status")
    private String status = "SENT";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
