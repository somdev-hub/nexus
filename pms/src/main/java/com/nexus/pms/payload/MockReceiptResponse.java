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
public class MockReceiptResponse {

    private String receiptNo;
    private Long paymentId;
    private String transactionReference;
    private String status;
    private Double amount;
    private String currency;
    private Double feeAmount;
    private Double netAmount;
    private String paymentMethod;
    private String methodDetailMasked;
    private String payerName;
    private String description;
    private Long invoiceId;
    private String invoiceNumber;
    private String orgId;
    private String payerRole;
    private String idempotencyKey;
    private Timestamp paidAt;
    private Timestamp createdAt;
}
