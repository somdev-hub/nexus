package com.nexus.core.entities;

import java.sql.Date;

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
@Table(name = "t_supplier_quality_certificates", schema = "core")
public class SupplierQualityCertificate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Long certificateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", referencedColumnName = "purchase_order_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type")
    private CertificateType certificateType;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "dms_document_id")
    private String dmsDocumentId;

    @Column(name = "dms_document_url")
    private String dmsDocumentUrl;

    @Column(name = "issued_date")
    private Date issuedDate;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public enum CertificateType {
        COA,
        COC,
        TEST_REPORT
    }
}
