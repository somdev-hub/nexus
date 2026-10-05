package com.nexus.core.model.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
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
@Table(name = "t_quotation_line_items", schema = "core")
public class QuotationLineItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", referencedColumnName = "quotation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierQuotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    // Optional supporting document for the line (e.g. the variant's
    // datasheet). Must belong to the line's catalog when both are set.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "digital_asset_id", referencedColumnName = "asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierDigitalAsset digitalAsset;

    // Additional supporting documents for the line (multi-select).
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "t_quotation_line_digital_assets", schema = "core", joinColumns = @JoinColumn(name = "line_id"))
    @Column(name = "digital_asset_id")
    private java.util.List<Long> digitalAssetIds = new java.util.ArrayList<>();

    // Price tier the line's unit price was taken from (optional selection
    // record; must belong to the line's catalog when both are set).
    @Column(name = "price_tier_id")
    private Long priceTierId;

    // Product variant the line was configured with (optional selection
    // record; must belong to the line's catalog when both are set).
    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "description")
    private String description;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "notes")
    private String notes;
}
