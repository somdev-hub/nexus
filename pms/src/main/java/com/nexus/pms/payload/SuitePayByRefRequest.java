package com.nexus.pms.payload;

import com.nexus.pms.model.enums.MockPaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SuitePayByRefRequest {

    @NotBlank(message = "Payment reference is required")
    private String ref;

    @NotNull(message = "Payment method is required")
    private MockPaymentMethod paymentMethod;

    /** UPI id / card number / bank name / wallet id (full value ok — only masked copy is stored). */
    private String methodDetail;

    // Bank-transfer structured fields (validated server-side when method is BANK_TRANSFER)
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankHolderName;

    // Card structured fields (validated server-side when method is CARD)
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
    private String cardHolderName;

    private String payerName;
}
