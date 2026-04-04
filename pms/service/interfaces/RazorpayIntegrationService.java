package com.nexus.pms.service.interfaces;

import com.nexus.pms.payload.PaymentResponse;
import java.util.Map;

/**
 * Service interface for Razorpay API integration.
 * Handles all communication with Razorpay payment gateway.
 */
public interface RazorpayIntegrationService {

    /**
     * Create a payment order in Razorpay.
     * This initiates the payment with Razorpay gateway.
     *
     * @param amount   The payment amount in paisa (1 rupee = 100 paisa)
     * @param currency The currency code (INR)
     * @param receipt  The receipt ID for reference
     * @param metadata Additional metadata to attach to order
     * @return Razorpay order ID
     * @throws RazorpayException if Razorpay API call fails
     */
    String createOrder(Long amount, String currency, String receipt, Map<String, String> metadata);

    /**
     * Fetch payment details from Razorpay by payment ID.
     * Used for reconciliation and status verification.
     *
     * @param razorpayPaymentId The Razorpay payment ID
     * @return Payment details from Razorpay
     * @throws RazorpayException if payment not found or API error
     */
    Map<String, Object> fetchPaymentDetails(String razorpayPaymentId);

    /**
     * Fetch order details from Razorpay.
     *
     * @param razorpayOrderId The Razorpay order ID
     * @return Order details from Razorpay
     * @throws RazorpayException if order not found or API error
     */
    Map<String, Object> fetchOrderDetails(String razorpayOrderId);

    /**
     * Validate webhook signature from Razorpay.
     * Ensures the webhook is authentic and hasn't been tampered with.
     *
     * @param payload       The webhook payload
     * @param signature     The signature from webhook header
     * @param webhookSecret The webhook secret key
     * @return true if signature is valid, false otherwise
     */
    boolean validateWebhookSignature(String payload, String signature, String webhookSecret);

    /**
     * Get settlement details for a payment.
     * Used for reconciliation between Razorpay and our system.
     *
     * @param razorpayPaymentId The Razorpay payment ID
     * @return Settlement details if available
     */
    Map<String, Object> getSettlementDetails(String razorpayPaymentId);

    /**
     * Verify payment amount and currency with Razorpay.
     * Ensures no amount tampering between order creation and payment capture.
     *
     * @param razorpayPaymentId The Razorpay payment ID
     * @param expectedAmount    Expected amount in paisa
     * @param expectedCurrency  Expected currency code
     * @return true if amounts match, false otherwise
     * @throws RazorpayException if payment not found
     */
    boolean verifyPaymentAmount(String razorpayPaymentId, Long expectedAmount, String expectedCurrency);

    /**
     * Check if Razorpay API is accessible.
     *
     * @return true if API is healthy, false otherwise
     */
    boolean isHealthy();
}
