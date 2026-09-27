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
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.sql.Date;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_freight_rates", schema = "core")
public class FreightRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id")
    private Long rateId;

    @Column(name = "rate_code", unique = true, nullable = false)
    private String rateCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    private RateType rateType;

    @Column(name = "origin_lane")
    private String originLane;

    @Column(name = "destination_lane")
    private String destinationLane;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type")
    private FleetAssetType equipmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_mode")
    private ShipmentMode shipmentMode;

    @Column(name = "base_rate", precision = 19, scale = 4)
    private BigDecimal baseRate = BigDecimal.ZERO;

    @Column(name = "fuel_surcharge_formula")
    private String fuelSurchargeFormula;

    @Column(name = "fuel_surcharge_pct", precision = 5, scale = 2)
    private BigDecimal fuelSurchargePct = BigDecimal.ZERO;

    @Column(name = "accessorial_table", columnDefinition = "TEXT")
    private String accessorialTable;

    private String currency = "USD";

    @Column(name = "effective_from")
    private Date effectiveFrom;

    @Column(name = "effective_to")
    private Date effectiveTo;

    @Column(name = "is_active_rate")
    private Boolean isActiveRate = true;

    private String notes;

    @Version
    private Long version = 0L;
}
