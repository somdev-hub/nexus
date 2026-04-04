package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nexus.pms.model.enums.PaymentStatus;
import java.sql.Timestamp;
import java.util.Map;

/**
 * DTO for payment response.
 * Contains payment details and status information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String transactionReference;
    private String idempotencyKey;
    private String razorpayPaymentId;

    private Long merchantId;
    private Long customerId;
    private Long merchantMemberId;

    private Double amount;
    private String currency;
    private String description;
    private String paymentType;
    private String paymentMethod;

    private PaymentStatus paymentStatus;
    private String statusMessage;

    private Double grossAmount;
    private Double feeAmount;
    private Double netAmount;
    private Double taxAmount;

    private Boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String errorCode;
    private String errorMessage;

    private Map<String, Object> metadata;
}
