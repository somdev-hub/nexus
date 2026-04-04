package com.nexus.pms.service.interfaces;

import com.nexus.pms.payload.InitiatePaymentRequest;
import com.nexus.pms.payload.PaymentResponse;
import com.nexus.pms.model.enums.PaymentStatus;

import java.util.List;

/**
 * Service interface for payment operations.
 * Defines all payment-related business logic methods.
 */
public interface PaymentService {

    /**
     * Initiate a new payment transaction.
     * Handles both supply chain (customer) and salary (merchant member) payments.
     *
     * @param request InitiatePaymentRequest containing payment details
     * @return PaymentResponse with payment details and status
     * @throws IllegalArgumentException  if request validation fails
     * @throws ResourceNotFoundException if merchant/customer/member not found
     * @throws IdempotencyException      if idempotency key already exists
     * @throws PaymentException          if payment initiation fails
     */
    PaymentResponse initiatePayment(InitiatePaymentRequest request);

    /**
     * Retrieve payment details by payment ID.
     *
     * @param paymentId The payment ID
     * @return PaymentResponse with full payment details
     * @throws ResourceNotFoundException if payment not found
     */
    PaymentResponse getPaymentDetails(Long paymentId);

    /**
     * Retrieve payment by idempotency key (for deduplication).
     *
     * @param idempotencyKey The idempotency key used in payment initiation
     * @return PaymentResponse if exists, null otherwise
     */
    PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey);

    /**
     * Retrieve all payments for a merchant with optional filtering.
     *
     * @param merchantId The merchant ID
     * @return List of PaymentResponse for the merchant
     */
    List<PaymentResponse> getPaymentsByMerchant(Long merchantId);

    /**
     * Retrieve all payments for a customer.
     *
     * @param customerId The customer ID
     * @return List of PaymentResponse for the customer
     */
    List<PaymentResponse> getPaymentsByCustomer(Long customerId);

    /**
     * Retrieve all payments for a merchant member (employee).
     *
     * @param memberId The merchant member ID
     * @return List of PaymentResponse for the member
     */
    List<PaymentResponse> getPaymentsByMerchantMember(Long memberId);

    /**
     * Update payment status after Razorpay callback.
     *
     * @param paymentId         The payment ID
     * @param status            The new payment status
     * @param razorpayPaymentId The Razorpay payment ID from webhook
     * @return Updated PaymentResponse
     */
    PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status, String razorpayPaymentId);

    /**
     * Handle payment failure and update retry count.
     *
     * @param paymentId    The payment ID
     * @param errorCode    The error code from Razorpay
     * @param errorMessage The error message
     * @return Updated PaymentResponse
     */
    PaymentResponse handlePaymentFailure(Long paymentId, String errorCode, String errorMessage);

    /**
     * Cancel a pending payment.
     *
     * @param paymentId The payment ID
     * @param reason    The cancellation reason
     * @return Updated PaymentResponse with CANCELLED status
     */
    PaymentResponse cancelPayment(Long paymentId, String reason);

    /**
     * Get payments by status for analytics/reporting.
     *
     * @param status The payment status to filter by
     * @return List of PaymentResponse with the given status
     */
    List<PaymentResponse> getPaymentsByStatus(PaymentStatus status);

    /**
     * Process retry for failed payments.
     *
     * @param paymentId The payment ID to retry
     * @return PaymentResponse with updated status
     */
    PaymentResponse retryPayment(Long paymentId);
}
