package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO for payment response payload.
 * Contains the result of a payment operation including status and details.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {

    /**
     * Payment ID.
     */
    private Long paymentId;

    /**
     * Idempotency key used for this payment.
     */
    private String idempotencyKey;

    /**
     * Payment status (e.g., PENDING, COMPLETED, FAILED, REFUNDED).
     */
    private String paymentStatus;

    /**
     * The amount that was charged.
     */
    private Double amount;

    /**
     * Currency of the payment.
     */
    private String currency;

    /**
     * Description of the payment.
     */
    private String description;

    /**
     * Payment type.
     */
    private String paymentType;

    /**
     * Merchant ID.
     */
    private Long merchantId;

    /**
     * Customer ID.
     */
    private Long customerId;

    /**
     * Merchant member ID.
     */
    private Long merchantMemberId;

    /**
     * Payment method ID.
     */
    private Long paymentMethodId;

    /**
     * Transaction reference.
     */
    private String transactionReference;

    /**
     * Razorpay payment ID.
     */
    private String razorpayPaymentId;

    /**
     * Gross amount (before fees/taxes).
     */
    private Double grossAmount;

    /**
     * Fee amount charged.
     */
    private Double feeAmount;

    /**
     * Net amount received.
     */
    private Double netAmount;

    /**
     * Tax amount.
     */
    private Double taxAmount;

    /**
     * Number of retry attempts.
     */
    private Integer retryCount;

    /**
     * Last error code (if payment failed).
     */
    private String lastErrorCode;

    /**
     * Last error message (if payment failed).
     */
    private String lastErrorMessage;

    /**
     * Whether this was a retry due to idempotency.
     */
    private Boolean isIdempotentRetry;

    /**
     * Payment creation timestamp.
     */
    private Timestamp createdAt;

    /**
     * Payment last update timestamp.
     */
    private Timestamp updatedAt;

    /**
     * Error message if the operation failed.
     */
    private String errorMessage;

    /**
     * Success flag.
     */
    private Boolean success;
}
