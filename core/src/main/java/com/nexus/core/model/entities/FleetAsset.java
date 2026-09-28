package com.nexus.core.model.entities;

import com.nexus.core.model.enums.FleetAssetStatus;
import com.nexus.core.model.enums.FleetAssetType;
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

import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_fleet_assets", schema = "core")
public class FleetAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_number", unique = true, nullable = false)
    private String assetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private FleetAssetType assetType;

    private String make;
    private String model;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "license_plate", unique = true)
    private String licensePlate;

    @Column(name = "vin", unique = true)
    private String vin;

    @Column(name = "capacity_weight")
    private Double capacityWeight;

    @Column(name = "capacity_volume")
    private Double capacityVolume;

    @Enumerated(EnumType.STRING)
    private FleetAssetStatus status = FleetAssetStatus.AVAILABLE;

    @Column(name = "current_mileage")
    private Double currentMileage = 0.0;

    @Column(name = "current_hours")
    private Double currentHours = 0.0;

    @Column(name = "last_maintenance_date")
    private Date lastMaintenanceDate;

    @Column(name = "next_maintenance_due_mileage")
    private Double nextMaintenanceDueMileage;

    @Column(name = "next_maintenance_due_date")
    private Date nextMaintenanceDueDate;

    @Column(name = "insurance_expiry")
    private Date insuranceExpiry;

    @Column(name = "permit_expiry")
    private Date permitExpiry;

    @Column(name = "dms_document_id")
    private String dmsDocumentId;

    private String notes;

    // Live GPS position (simulated until telematics integration lands)
    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "last_position_at")
    private Timestamp lastPositionAt;

    @Version
    private Long version = 0L;
}
