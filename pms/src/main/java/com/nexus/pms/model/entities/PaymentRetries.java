package com.nexus.pms.model.entities;

import java.sql.Timestamp;

import com.nexus.pms.model.enums.RetryStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "t_payment_retries", schema = "pms")
@Data
public class PaymentRetries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long retryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    private Integer retryCount;
    @Enumerated(EnumType.STRING)
    private RetryStatus retryStatus;

    private String retryError;
    private String retryResponse;

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
