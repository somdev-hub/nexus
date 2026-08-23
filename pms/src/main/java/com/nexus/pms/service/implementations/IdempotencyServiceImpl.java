package com.nexus.pms.service.implementations;

import com.nexus.pms.model.entities.IdempotencyRecord;
import com.nexus.pms.model.entities.Payment;
import com.nexus.pms.payload.PaymentResponse;
import com.nexus.pms.repository.IdempotencyRepository;
import com.nexus.pms.service.interfaces.IdempotencyService;
import com.nexus.pms.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of IdempotencyService for handling idempotent payment
 * processing.
 * Ensures that duplicate payment requests result in the same response without
 * double-charging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectProvider<PaymentService> paymentServiceProvider;

    /**
     * Check if an idempotency key already exists and retrieve cached payment
     * response.
     *
     * @param idempotencyKey The idempotency key
     * @return PaymentResponse if found, null otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse checkAndGetExistingPayment(String idempotencyKey) {
        log.debug("Checking for existing payment with idempotency key: {}", idempotencyKey);

        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .map(record -> {
                    log.info("Found existing payment for idempotency key: {}", idempotencyKey);
                    // Retrieve the payment details and build response
                    Payment payment = paymentServiceProvider.getObject().getPaymentById(record.getPaymentId());
                    return paymentServiceProvider.getObject().mapPaymentToResponse(payment);
                })
                .orElse(null);
    }

    /**
     * Store idempotency record after successful payment.
     * This prevents the same payment from being processed multiple times.
     *
     * @param idempotencyKey The unique idempotency key
     * @param paymentId      The payment ID to associate with this key
     * @return true if successfully stored
     */
    @Override
    public boolean storeIdempotencyRecord(String idempotencyKey, Long paymentId) {
        try {
            log.debug("Storing idempotency record - key: {}, paymentId: {}", idempotencyKey, paymentId);

            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .paymentId(paymentId)
                    .build();

            idempotencyRepository.save(record);
            log.info("Successfully stored idempotency record for key: {}", idempotencyKey);
            return true;
        } catch (Exception e) {
            log.error("Failed to store idempotency record for key: {}", idempotencyKey, e);
            // Don't fail the payment just because idempotency storage failed
            // The payment was successful, but we couldn't cache the idempotency key
            return false;
        }
    }

    /**
     * Validate idempotency key format.
     * Should be non-empty and preferably a valid UUID format.
     *
     * @param idempotencyKey The key to validate
     * @return true if valid
     */
    @Override
    public boolean isValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            log.warn("Invalid idempotency key: null or empty");
            return false;
        }

        if (idempotencyKey.length() < 1 || idempotencyKey.length() > 255) {
            log.warn("Invalid idempotency key length: {}", idempotencyKey.length());
            return false;
        }

        // Try to parse as UUID - valid format but not mandatory
        try {
            UUID.fromString(idempotencyKey);
            return true;
        } catch (IllegalArgumentException e) {
            // Not a UUID, but still valid if it matches a reasonable pattern
            // Allow alphanumeric with hyphens
            return idempotencyKey.matches("^[a-zA-Z0-9_-]+$");
        }
    }

    /**
     * Generate a new idempotency key using UUID.
     *
     * @return A unique idempotency key
     */
    @Override
    public String generateIdempotencyKey() {
        String key = UUID.randomUUID().toString();
        log.debug("Generated idempotency key: {}", key);
        return key;
    }
}
