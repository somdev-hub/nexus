package com.nexus.pms.model.enums;

import java.sql.Timestamp;

import com.nexus.pms.model.entities.Payment;
import com.nexus.pms.model.entities.ProcessingStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "t_payment_webhooks", schema = "pms")
public class PaymentWebhooks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long webhookId;

    private String webhookEventId;

    private String webhookEventType;

    private String webhookPayload;

    private String webhookSignature;

    private Boolean isSignatureValid;
    private String signatureValidationError;

    private ProcessingStatus processingStatus;

    private String processingError;

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

    private Payment payment;

}
