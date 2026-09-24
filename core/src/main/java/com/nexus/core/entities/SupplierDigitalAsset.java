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
@Table(name = "t_supplier_digital_assets", schema = "core")
public class SupplierDigitalAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type")
    private AssetType assetType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "dms_document_id")
    private String dmsDocumentId;

    @Column(name = "dms_document_url")
    private String dmsDocumentUrl;

    @Column(name = "version")
    private Integer version = 1;

    public enum AssetType {
        DATASHEET,
        CERTIFICATION,
        COMPLIANCE,
        MODEL_3D,
        COA,
        COC,
        TEST_REPORT
    }
}
