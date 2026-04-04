package com.nexus.pms.model.entities;

import java.sql.Timestamp;

import com.nexus.pms.model.enums.PaymentStatus;
import com.nexus.pms.model.enums.PaymentType;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "t_payments", schema = "pms")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private Long merchantMemberId;

    private String idempotencyKey;
    private String transactionReference;
    private String razorpayPaymentId;

    private Double amount;
    private String currency;
    private String description;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType; // SALARY, REFUND, INVOICE, SUPPLY, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodEntity paymentMethodEntity;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private Double grossAmount;
    private Double feeAmount;
    private Double netAmount;
    private Double taxAmount;

    private Integer retryCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Boolean isIdempotentRetry;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        createdAt = new Timestamp(System.currentTimeMillis());
        isActive = true;
        updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
