package com.nexus.pms.service.interfaces;

import com.nexus.pms.payload.PaymentResponse;

/**
 * Service interface for idempotency handling.
 * Ensures duplicate payment requests are detected and handled properly.
 */
public interface IdempotencyService {

    /**
     * Check if an idempotency key already exists in the system.
     * Used to prevent duplicate payment processing.
     *
     * @param idempotencyKey The idempotency key to check
     * @return PaymentResponse if exists, null if not found
     */
    PaymentResponse checkAndGetExistingPayment(String idempotencyKey);

    /**
     * Store the idempotency key with the payment result.
     * Should be called after successful payment processing.
     *
     * @param idempotencyKey The idempotency key
     * @param paymentId      The payment ID associated with this key
     * @return true if stored successfully
     */
    boolean storeIdempotencyRecord(String idempotencyKey, Long paymentId);

    /**
     * Check if idempotency key is valid format.
     * Should be a non-empty UUID or similar unique identifier.
     *
     * @param idempotencyKey The idempotency key to validate
     * @return true if valid format, false otherwise
     */
    boolean isValidIdempotencyKey(String idempotencyKey);

    /**
     * Generate a new idempotency key.
     * Useful for clients that don't provide one.
     *
     * @return A generated idempotency key (UUID)
     */
    String generateIdempotencyKey();
}
