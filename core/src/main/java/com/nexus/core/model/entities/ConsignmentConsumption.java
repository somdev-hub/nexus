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
@Table(name = "t_consignment_consumptions", schema = "core")
public class ConsignmentConsumption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consumption_id")
    private Long consumptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consignment_id", referencedColumnName = "consignment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ConsignmentStock consignment;

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

    @Column(name = "consumed_at")
    private Timestamp consumedAt;

    @Column(name = "consumed_by")
    private String consumedBy;
}