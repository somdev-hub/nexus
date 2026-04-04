package com.nexus.pms.model.entities;

import java.sql.Timestamp;

import com.nexus.pms.model.enums.PaymentMethod;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_payment_methods", schema = "pms")
public class PaymentMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentMethodId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    // card details
    private String cardBrand;
    private String cardLast4;
    private String cardExpMonth;
    private String cardExpYear;

    // bank details
    private String bankName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankAccountHolderName;

    // upi details
    private String upiId;
    private String upiVpa;

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
