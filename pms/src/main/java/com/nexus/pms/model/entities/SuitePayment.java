package com.nexus.pms.model.entities;

import java.sql.Timestamp;

import com.nexus.pms.model.enums.SuitePaymentStatus;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_suite_payments", schema = "pms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_suite_pay_idempotency", columnNames = "idempotencyKey"),
        @UniqueConstraint(name = "uk_suite_pay_intent_ref", columnNames = "intentRef")
})
@Data
public class SuitePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long intentId;

    /** Raw opaque reference (UUID). Never expose raw — only encrypted form goes in URL. */
    @Column(nullable = false, unique = true)
    private String intentRef;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    // Captured invoice snapshot (immutable once created)
    private Long invoiceId;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String orgId;
    private String payerRole;

    // Logistics settlement linkage (core LQ-* quote ref for
    // LOGISTICS_SETTLEMENT intents; amount locked to frozen grandTotal).
    private String quoteRef;
    private Long partnershipId;
    private String intentKind;

    @Column(nullable = false)
    private Double amount;
    private String currency = "INR";
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuitePaymentStatus status = SuitePaymentStatus.INITIATED;

    /** Linked mock execution (set on pay). */
    private Long mockPaymentId;
    private String failureReason;

    private Timestamp expiresAt;
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
            status = SuitePaymentStatus.INITIATED;
        if (expiresAt == null)
            expiresAt = new Timestamp(System.currentTimeMillis() + 30 * 60 * 1000L);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
