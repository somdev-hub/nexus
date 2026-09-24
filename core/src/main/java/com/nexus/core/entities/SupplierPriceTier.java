package com.nexus.core.entities;

import java.math.BigDecimal;
import java.sql.Date;

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
@Table(name = "t_supplier_price_tiers", schema = "core")
public class SupplierPriceTier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Long tierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    @Column(name = "tier_name")
    private String tierName;

    @Column(name = "min_quantity")
    private Double minQuantity;

    @Column(name = "max_quantity")
    private Double maxQuantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "customer_segment")
    private String customerSegment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", referencedColumnName = "contract_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierContract contract;

    @Column(name = "valid_from")
    private Date validFrom;

    @Column(name = "valid_to")
    private Date validTo;

    @Column(name = "currency", length = 3)
    private String currency = "USD";
}
