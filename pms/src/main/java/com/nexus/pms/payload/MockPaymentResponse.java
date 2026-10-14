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
public class MockPaymentResponse {

    private Long paymentId;
    private String idempotencyKey;
    private String transactionReference;
    private String receiptNo;
    private String status;
    private Double amount;
    private String currency;
    private String description;
    private String paymentMethod;
    private String methodDetailMasked;
    private Long invoiceId;
    private String invoiceNumber;
    private String orgId;
    private String payerRole;
    private String failureCode;
    private String failureReason;
    private Boolean isIdempotentRetry;
    private Boolean success;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp completedAt;
}
