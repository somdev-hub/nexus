package com.nexus.pms.service.interfaces;

import com.nexus.pms.model.entities.Payment;
import com.nexus.pms.payload.PaymentRequest;
import com.nexus.pms.payload.PaymentResponse;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for payment processing.
 * Handles creating, processing, and managing payments.
 */
public interface PaymentService {

    /**
     * Process a payment with idempotency.
     * If the idempotency key already exists, returns the cached response.
     * Otherwise, processes the payment and stores the idempotency key.
     * 
     * Returns ResponseEntity to allow service layer to control:
     * - HTTP status codes
     * - Response data structure (ErrorResponseDto, PaymentResponse, etc.)
     * - Response headers
     *
     * @param paymentRequest The payment request details
     * @return ResponseEntity with PaymentResponse on success or ErrorResponseDto on
     *         error
     */
    ResponseEntity<?> processPaymentWithIdempotency(PaymentRequest paymentRequest);

    /**
     * Get a payment by ID.
     *
     * @param paymentId The payment ID
     * @return Payment entity if found
     * @throws RuntimeException if payment not found
     */
    Payment getPaymentById(Long paymentId);

    /**
     * Map Payment entity to PaymentResponse DTO.
     *
     * @param payment The payment entity
     * @return PaymentResponse DTO
     */
    PaymentResponse mapPaymentToResponse(Payment payment);

    /**
     * Calculate payment fees and net amount.
     *
     * @param amount The gross amount
     * @return Array with [feeAmount, taxAmount, netAmount]
     */
    Double[] calculatePaymentCharges(Double amount);

    /**
     * Validate payment details.
     *
     * @param request The payment request
     * @throws IllegalArgumentException if validation fails
     */
    void validatePaymentRequest(PaymentRequest request);
}
