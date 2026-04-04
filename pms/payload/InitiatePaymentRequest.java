package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for initiating a payment request.
 * Supports both customer and merchant member payment flows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    // Required fields
    private Long merchantId;
    private Double amount;
    private String currency;
    private String idempotencyKey;

    // Optional: For general payments (supply chain)
    private Long customerId;

    // Optional: For salary payments
    private Long merchantMemberId;

    // Payment metadata
    private String description;
    private String paymentType; // SALARY, SUPPLY_CHAIN, GENERAL
    private String paymentMethod; // BANK_TRANSFER, UPI, WALLET
    private String externalReferenceId; // Link to HR payroll, invoice, etc.

    // Additional metadata
    private Map<String, Object> metadata;

    /**
     * Validates that the request has either customerId or merchantMemberId
     */
    public void validate() {
        if (merchantId == null || merchantId <= 0) {
            throw new IllegalArgumentException("Merchant ID is required and must be positive");
        }

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required for deduplication");
        }

        // Either customer or merchant member must be provided
        if ((customerId == null || customerId <= 0) && (merchantMemberId == null || merchantMemberId <= 0)) {
            throw new IllegalArgumentException("Either customerId or merchantMemberId must be provided");
        }

        // Cannot provide both
        if ((customerId != null && customerId > 0) && (merchantMemberId != null && merchantMemberId > 0)) {
            throw new IllegalArgumentException("Cannot provide both customerId and merchantMemberId. Use either one.");
        }
    }
}
