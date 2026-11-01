package com.nexus.core.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.sql.Timestamp;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_vmi_replenishments", schema = "core")
public class VmiReplenishment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "replenishment_id")
    private Long replenishmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vmi_id", referencedColumnName = "vmi_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VmiConfig vmi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account retailerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", referencedColumnName = "material_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Material material;

    private Double quantity;

    private Double unitPrice;

    private Double totalAmount;

    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "received_at")
    private Timestamp receivedAt;
}