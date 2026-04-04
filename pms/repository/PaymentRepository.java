package com.nexus.pms.repository;

import com.nexus.pms.model.entities.Payment;
import com.nexus.pms.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Provides CRUD operations and custom queries for payment management.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by idempotency key to prevent duplicate payments.
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find payment by transaction reference.
     */
    Optional<Payment> findByTransactionReference(String transactionReference);

    /**
     * Find payment by Razorpay payment ID.
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Get all payments for a merchant.
     */
    @Query("SELECT p FROM Payment p WHERE p.merchant.merchantId = :merchantId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Payment> findAllByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * Get all payments for a customer.
     */
    @Query("SELECT p FROM Payment p WHERE p.customer.customerId = :customerId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Payment> findAllByCustomerId(@Param("customerId") Long customerId);

    /**
     * Get all payments for a merchant member (employee).
     */
    @Query("SELECT p FROM Payment p WHERE p.merchantMember.merchantMemberId = :memberId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Payment> findAllByMerchantMemberId(@Param("memberId") Long memberId);

    /**
     * Get payments by status.
     */
    List<Payment> findByPaymentStatus(PaymentStatus status);

    /**
     * Get failed payments for retry.
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentStatus IN ('INITIATED', 'PENDING', 'FAILED') " +
            "AND p.retryCount < 3 AND p.isActive = true ORDER BY p.createdAt ASC")
    List<Payment> findPaymentsForRetry();

    /**
     * Get recently created payments.
     */
    @Query(value = "SELECT p FROM Payment p WHERE p.createdAt >= CURRENT_TIMESTAMP - INTERVAL :minutes MINUTE ORDER BY p.createdAt DESC")
    List<Payment> findRecentPayments(@Param("minutes") Integer minutes);
}
