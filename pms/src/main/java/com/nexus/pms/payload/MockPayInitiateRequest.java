package com.nexus.pms.payload;

import com.nexus.pms.model.enums.MockPaymentMethod;

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
public class MockPayInitiateRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private Double amount;

    private String currency;

    private String description;

    @NotNull(message = "Payment method is required")
    private MockPaymentMethod paymentMethod;

    /** Card number / VPA / bank label / wallet id (full value ok — only masked copy is stored). */
    private String methodDetail;

    private String payerName;

    // Structured bank-transfer fields (optional; validated when method is BANK_TRANSFER)
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankHolderName;

    // Structured card fields (optional; validated when method is CARD)
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String cardHolderName;

    // Business linkage
    private Long invoiceId;
    private String invoiceNumber;
    private Long purchaseOrderId;
    private String orgId;
    private String payerRole;

    private String idempotencyKey;
}
