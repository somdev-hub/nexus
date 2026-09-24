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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_product_variants", schema = "core")
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type")
    private VariantType variantType;

    @Column(name = "variant_value")
    private String variantValue;

    @Column(name = "sku_suffix")
    private String skuSuffix;

    @Column(name = "price_adjustment")
    private Double priceAdjustment = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_material_id", referencedColumnName = "material_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Material bomMaterial;

    @Column(name = "quantity_available")
    private Double quantityAvailable = 0.0;

    public enum VariantType {
        SIZE,
        COLOR,
        CONFIGURATION
    }
}
