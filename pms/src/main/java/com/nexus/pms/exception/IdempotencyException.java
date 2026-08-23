package com.nexus.pms.exception;

/**
 * Exception thrown when idempotency check fails or duplicate payment is
 * detected.
 */
public class IdempotencyException extends PaymentException {

    private String idempotencyKey;
    private String existingTransactionId;

    public IdempotencyException(String message, String idempotencyKey) {
        super(message, "IDEMPOTENCY_VIOLATION");
        this.idempotencyKey = idempotencyKey;
    }

    public IdempotencyException(String message, String idempotencyKey, String existingTransactionId) {
        super(message, "IDEMPOTENCY_VIOLATION");
        this.idempotencyKey = idempotencyKey;
        this.existingTransactionId = existingTransactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExistingTransactionId() {
        return existingTransactionId;
    }
}
