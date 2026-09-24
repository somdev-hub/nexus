package com.nexus.core.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_supplier_catalog", schema = "core", uniqueConstraints = {
        @UniqueConstraint(name = "uk_catalog_code_org", columnNames = {"code", "supplier_org_id"}),
        @UniqueConstraint(name = "uk_catalog_sku_org", columnNames = {"sku", "supplier_org_id"})
})
public class SupplierCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catalog_id")
    private Long catalogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Category -> Family -> SKU hierarchy (FR-SUP-001)
    @Column(name = "category", columnDefinition = "TEXT")
    private String category;

    @Column(name = "family", columnDefinition = "TEXT")
    private String family;

    @Column(name = "sku", columnDefinition = "TEXT")
    private String sku;

    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes; // JSON

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications; // JSON

    @Column(name = "base_price")
    private Double basePrice;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_status")
    private CatalogStatus status = CatalogStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    private CatalogAccessLevel accessLevel = CatalogAccessLevel.PRIVATE;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    // Partner visibility for PARTNER_ONLY
    @Column(name = "allowed_partner_org_ids", columnDefinition = "TEXT")
    private String allowedPartnerOrgIds; // comma-separated org ids

    @OneToMany(mappedBy = "catalog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "catalog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SupplierPriceTier> priceTiers = new ArrayList<>();

    @OneToMany(mappedBy = "catalog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SupplierDigitalAsset> digitalAssets = new ArrayList<>();
}
