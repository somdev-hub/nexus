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
@Table(name = "t_collaborative_forecasts", schema = "core")
public class CollaborativeForecast extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_id")
    private Long forecastId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account retailerOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", referencedColumnName = "catalog_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupplierCatalog catalog;

    @Column(name = "period_start", nullable = false)
    private Date periodStart;

    @Column(name = "period_end", nullable = false)
    private Date periodEnd;

    @Column(name = "forecast_quantity")
    private Double forecastQuantity;

    @Column(name = "confidence_pct")
    private Double confidencePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ForecastStatus status = ForecastStatus.DRAFT;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private String createdBy;

    public enum ForecastStatus {
        DRAFT,
        SHARED,
        CONFIRMED,
        REJECTED
    }
}
