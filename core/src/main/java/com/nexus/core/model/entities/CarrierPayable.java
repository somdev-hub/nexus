package com.nexus.core.model.entities;

import com.nexus.core.model.enums.PayableStatus;
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
@Table(name = "t_carrier_payables", schema = "core")
public class CarrierPayable extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payable_id")
    private Long payableId;

    @Column(name = "payable_number", unique = true, nullable = false)
    private String payableNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", referencedColumnName = "shipment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logistics_org_id", referencedColumnName = "account_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account logisticsOrg;

    @Column(name = "carrier_name")
    private String carrierName;

    @Enumerated(EnumType.STRING)
    private PayableStatus status = PayableStatus.PENDING;

    @Column(name = "payable_amount", precision = 19, scale = 4)
    private BigDecimal payableAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    private String currency = "USD";

    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "pms_reference_id")
    private String pmsReferenceId;

    private String notes;

    @Version
    private Long version = 0L;
}
