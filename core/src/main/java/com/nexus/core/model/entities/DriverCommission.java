package com.nexus.core.model.entities;

import com.nexus.core.model.enums.DriverCommissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Per-trip commission owed to a contract driver, derived from the resolved
 * trip fare at the driver's commission rate. One record per delivered
 * shipment and driver; PENDING lines re-price on every read (same self-heal
 * pattern as the logistics payable lines), PAID history is frozen.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Table(name = "t_driver_commissions", schema = "core")
public class DriverCommission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commission_id")
    private Long commissionId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "logistics_org_id")
    private Long logisticsOrgId;

    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "shipment_number")
    private String shipmentNumber;

    /** City lane label of the billed trip (origin → destination). */
    private String lane;

    private BigDecimal amount;

    private String currency;

    /** Final shipment status at commission generation time. */
    @Column(name = "shipment_status")
    private String shipmentStatus;

    @Enumerated(EnumType.STRING)
    private DriverCommissionStatus status = DriverCommissionStatus.PENDING;

    @Column(name = "delivered_at")
    private Date deliveredAt;

    @Column(name = "paid_at")
    private Timestamp paidAt;

    @Column(name = "payment_ref")
    private String paymentRef;

    private String notes;

    @Version
    private Long version = 0L;
}
