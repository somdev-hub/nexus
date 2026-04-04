package com.nexus.pms.repository;

import com.nexus.pms.model.entities.PaymentRetries;
import com.nexus.pms.model.enums.RetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PaymentRetries entity.
 * Tracks payment retry attempts for failed transactions.
 */
@Repository
public interface PaymentRetriesRepository extends JpaRepository<PaymentRetries, Long> {

    /**
     * Get all retries for a payment.
     */
    @Query("SELECT pr FROM PaymentRetries pr WHERE pr.payment.paymentId = :paymentId ORDER BY pr.createdAt DESC")
    List<PaymentRetries> findByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * Get last retry status for a payment.
     */
    @Query(value = "SELECT pr FROM PaymentRetries pr WHERE pr.payment.paymentId = :paymentId ORDER BY pr.createdAt DESC LIMIT 1")
    PaymentRetries findLastRetryForPayment(@Param("paymentId") Long paymentId);

    /**
     * Get count of retries for a payment.
     */
    @Query("SELECT COUNT(pr) FROM PaymentRetries pr WHERE pr.payment.paymentId = :paymentId")
    int countByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * Find failed retries to analyze error patterns.
     */
    @Query("SELECT pr FROM PaymentRetries pr WHERE pr.retryStatus = :status ORDER BY pr.createdAt DESC")
    List<PaymentRetries> findByStatus(@Param("status") RetryStatus status);
}
