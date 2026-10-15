package com.nexus.pms.payload;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SuiteIntentResponse {

    private Long intentId;
    /** Encrypted opaque reference — use as ?ref= param. Raw intentRef never leaves server. */
    private String ref;
    private String idempotencyKey;
    private String status;

    private Long invoiceId;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String orgId;
    private String payerRole;

    private String quoteRef;
    private Long partnershipId;
    private String intentKind;

    private Double amount;
    private String currency;
    private String description;

    private Long mockPaymentId;
    private MockPaymentResponse mockPayment;
    private MockReceiptResponse receipt;
    private String failureReason;
    private Boolean isIdempotentRetry;

    private Timestamp expiresAt;
    private Timestamp createdAt;
    private Timestamp completedAt;
}
