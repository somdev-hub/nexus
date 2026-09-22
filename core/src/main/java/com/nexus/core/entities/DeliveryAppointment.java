package com.nexus.core.entities;

import java.sql.Timestamp;

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

/**
 * Delivery Appointment Scheduling (FR-RET-034).
 * Retailers schedule delivery appointments against receiving warehouses.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_delivery_appointments", schema = "core")
public class DeliveryAppointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "appointment_number", unique = true)
    private String appointmentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", referencedColumnName = "warehouse_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account retailerOrg;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeliveryAppointmentStatus status = DeliveryAppointmentStatus.SCHEDULED;

    @Column(name = "scheduled_start")
    private Timestamp scheduledStart;

    @Column(name = "scheduled_end")
    private Timestamp scheduledEnd;

    @Column(name = "actual_arrival")
    private Timestamp actualArrival;

    @Column(name = "actual_departure")
    private Timestamp actualDeparture;

    @Column(name = "dock_number")
    private String dockNumber;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Column(name = "notes")
    private String notes;

    @Version
    private Long version = 0L;
}
