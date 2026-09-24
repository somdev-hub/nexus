package com.nexus.core.entities;

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
@Table(name = "t_consignment_stock", schema = "core")
public class ConsignmentStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consignment_id")
    private Long consignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account retailerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", referencedColumnName = "material_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Material material;

    @Column(name = "quantity_on_hand")
    private Double quantityOnHand = 0.0;

    @Column(name = "quantity_reserved")
    private Double quantityReserved = 0.0;

    @Column(name = "quantity_available")
    private Double quantityAvailable = 0.0;

    @Column(name = "consignment_number", unique = true)
    private String consignmentNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public void recalculateAvailable() {
        this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        if (this.quantityAvailable < 0) this.quantityAvailable = 0.0;
    }
}
