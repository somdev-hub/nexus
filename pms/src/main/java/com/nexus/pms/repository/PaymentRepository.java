package com.nexus.pms.repository;

import com.nexus.pms.model.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Provides database operations for payment records.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find a payment by idempotency key.
     * Used to check if a payment with the same idempotency key already exists.
     *
     * @param idempotencyKey The idempotency key
     * @return Payment if found, empty otherwise
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find a payment by transaction reference.
     *
     * @param transactionReference The transaction reference
     * @return Payment if found, empty otherwise
     */
    Optional<Payment> findByTransactionReference(String transactionReference);

    /**
     * Find a payment by Razorpay payment ID.
     *
     * @param razorpayPaymentId The Razorpay payment ID
     * @return Payment if found, empty otherwise
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
}
