package com.nexus.core.entities;

import java.sql.Date;

import jakarta.persistence.Column;
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
@Table(name = "t_production_capacity_calendar", schema = "core")
public class ProductionCapacityCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capacity_id")
    private Long capacityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_org_id", referencedColumnName = "account_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account supplierOrg;

    @Column(name = "product_line")
    private String productLine;

    @Column(name = "period_start", nullable = false)
    private Date periodStart;

    @Column(name = "period_end", nullable = false)
    private Date periodEnd;

    @Column(name = "shift")
    private String shift;

    @Column(name = "available_capacity")
    private Double availableCapacity = 0.0;

    @Column(name = "allocated_capacity")
    private Double allocatedCapacity = 0.0;

    @Column(name = "unit")
    private String unit = "UNITS";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
