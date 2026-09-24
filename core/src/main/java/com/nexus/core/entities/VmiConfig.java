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

import java.sql.Timestamp;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_vmi_config", schema = "core")
public class VmiConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vmi_id")
    private Long vmiId;

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
    @JoinColumn(name = "material_id", referencedColumnName = "material_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Material material;

    @Column(name = "min_level")
    private Double minLevel;

    @Column(name = "max_level")
    private Double maxLevel;

    @Column(name = "reorder_point")
    private Double reorderPoint;

    @Column(name = "reorder_quantity")
    private Double reorderQuantity;

    @Column(name = "auto_replenish")
    private Boolean autoReplenish = false;

    @Column(name = "last_replenished_at")
    private Timestamp lastReplenishedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
