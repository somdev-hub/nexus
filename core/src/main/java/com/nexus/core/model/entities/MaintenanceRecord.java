package com.nexus.core.model.entities;

import com.nexus.core.model.enums.MaintenanceStatus;
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
@Table(name = "t_maintenance_records", schema = "core")
public class MaintenanceRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Long maintenanceId;

    @Column(name = "maintenance_number", unique = true, nullable = false)
    private String maintenanceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", referencedColumnName = "asset_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FleetAsset asset;

    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    private String description;

    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status = MaintenanceStatus.SCHEDULED;

    @Column(name = "scheduled_date")
    private Date scheduledDate;

    @Column(name = "completed_date")
    private Date completedDate;

    @Column(name = "odometer_reading")
    private Double odometerReading;

    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "service_provider")
    private String serviceProvider;

    @Column(name = "is_breakdown")
    private Boolean isBreakdown = false;

    private String notes;

    @Version
    private Long version = 0L;
}
