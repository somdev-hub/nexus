package com.nexus.pms.payload;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SuiteIntentCreateRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private Double amount;

    private String currency;
    private String description;

    private Long invoiceId;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String orgId;
    private String payerRole;

    // Logistics settlement linkage (core LQ-* quote ref).
    private String quoteRef;
    private Long partnershipId;
    private String intentKind;

    /** Optional client key; header Idempotency-Key wins. Deterministic per invoice recommended. */
    private String idempotencyKey;
}
