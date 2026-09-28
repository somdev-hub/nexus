package com.nexus.core.model.entities;

import com.nexus.core.model.enums.DriverStatus;
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

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_drivers", schema = "core")
public class Driver extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "driver_code", unique = true, nullable = false)
    private String driverCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;
    private String email;

    @Column(name = "license_number", nullable = false)
    private String licenseNumber;

    @Column(name = "license_class")
    private String licenseClass;

    @Column(name = "license_expiry")
    private Date licenseExpiry;

    @Enumerated(EnumType.STRING)
    private DriverStatus status = DriverStatus.AVAILABLE;

    @Column(name = "hr_employee_id")
    private String hrEmployeeId;

    @Column(name = "on_time_rate")
    private Double onTimeRate = 0.0;

    @Column(name = "safety_score")
    private Double safetyScore = 0.0;

    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    private String notes;

    @Version
    private Long version = 0L;
}
