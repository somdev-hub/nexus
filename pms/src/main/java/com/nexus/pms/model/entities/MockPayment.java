package com.nexus.pms.model.entities;

import java.sql.Timestamp;

import com.nexus.pms.model.enums.MockPaymentMethod;
import com.nexus.pms.model.enums.MockPaymentStatus;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_mock_payments", schema = "pms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mock_payment_idempotency", columnNames = "idempotencyKey"),
        @UniqueConstraint(name = "uk_mock_payment_txn_ref", columnNames = "transactionReference"),
        @UniqueConstraint(name = "uk_mock_payment_receipt", columnNames = "receiptNo")
})
@Data
public class MockPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(unique = true)
    private String receiptNo;

    // Business linkage (no FK — PMS stays decoupled from core)
    private Long invoiceId;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String orgId;
    private String payerRole;

    // Money
    @Column(nullable = false)
    private Double amount;
    private String currency = "INR";
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MockPaymentMethod paymentMethod;

    // Masked method detail only (never store full PAN/CVV)
    private String methodDetailMasked;
    private String payerName;
    private String payerVpa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MockPaymentStatus status = MockPaymentStatus.PENDING;

    private String failureCode;
    private String failureReason;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp completedAt;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        createdAt = now;
        updatedAt = now;
        if (currency == null)
            currency = "INR";
        if (status == null)
            status = MockPaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
