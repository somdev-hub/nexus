package com.nexus.pms.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.nexus.pms.model.enums.PaymentType;
import com.nexus.pms.model.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * DTO for payment request payload.
 * Maps directly to Payment entity structure with nested merchant, customer, and
 * payment method DTOs.
 *
 * ARCHITECTURE:
 * =============
 * Every payment initiation must include:
 * - Complete merchant details with merchant members
 * - Complete payment method details
 * - Payment details (amount, type, description, etc.)
 * - Customer details (optional - required for REFUND/INVOICE only)
 *
 * No record reuse. Every payment request creates fresh records.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequest {

    /**
     * Unique idempotency key for ensuring idempotent requests.
     * If not provided, a new one will be generated.
     */
    private String idempotencyKey;

    /**
     * Merchant details - Required for all payments.
     * Includes merchant members for salary/distribution payments.
     */
    @Valid
    @NotNull(message = "Merchant details are required")
    private MerchantDetailsRequest merchant;

    /**
     * Payment method details - Required for all payments.
     */
    @Valid
    @NotNull(message = "Payment method details are required")
    private PaymentMethodRequest paymentMethod;

    /**
     * Customer details - Required for REFUND and INVOICE payment types.
     */
    @Valid
    private CustomerDetailsRequest customer;

    /**
     * Payment amount in the specified currency.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "9999999.99", message = "Amount cannot exceed maximum limit")
    private Double amount;

    /**
     * Currency code (e.g., INR, USD).
     */
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currency;

    /**
     * Payment description.
     */
    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 500, message = "Description must be between 5 and 500 characters")
    private String description;

    /**
     * Payment type (SALARY, REFUND, INVOICE, SUPPLY, OTHER).
     */
    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    /**
     * Transaction reference from the client.
     */
    private String transactionReference;

    /**
     * Additional metadata for the payment.
     */
    private String metadata;

    /**
     * Nested DTO: Merchant details (maps to t_merchants table with members).
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MerchantDetailsRequest {
        /**
         * Client Master ID - Required.
         * Links merchant to the parent client entity.
         */
        @NotNull(message = "Client Master ID is required")
        private Long clientMasterId;

        /**
         * Source system reference.
         */
        private Long sourceSystemId;

        /**
         * Merchant official email.
         */
        @NotBlank(message = "Merchant email is required")
        @Email(message = "Merchant email must be valid")
        private String merchantOfficialEmail;

        /**
         * Address line 1.
         */
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        private String addressLine1;

        /**
         * Address line 2.
         */
        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        private String addressLine2;

        /**
         * City.
         */
        @Size(max = 100, message = "City must not exceed 100 characters")
        private String city;

        /**
         * State.
         */
        @Size(max = 100, message = "State must not exceed 100 characters")
        private String state;

        /**
         * Pin code.
         */
        @Size(max = 20, message = "Pin code must not exceed 20 characters")
        private String pinCode;

        /**
         * Country.
         */
        @Size(max = 100, message = "Country must not exceed 100 characters")
        private String country;

        /**
         * Bank account number (will be encrypted).
         */
        @Size(max = 255, message = "Bank account number must not exceed 255 characters")
        private String bankAccountNumber;

        /**
         * Bank account holder name.
         */
        @Size(max = 255, message = "Bank account name must not exceed 255 characters")
        private String bankAccountName;

        /**
         * Bank name.
         */
        @Size(max = 100, message = "Bank name must not exceed 100 characters")
        private String bankName;

        /**
         * Bank IFSC code.
         */
        @Size(max = 20, message = "IFSC code must not exceed 20 characters")
        private String ifscCode;

        /**
         * Bank account type (SAVINGS, CURRENT, etc.).
         */
        @Size(max = 50, message = "Bank account type must not exceed 50 characters")
        private String bankAccountType;

        /**
         * Merchant members (employees, beneficiaries, etc.).
         */
        @Valid
        private List<MerchantMemberRequest> merchantMembers;
    }

    /**
     * Nested DTO: Merchant Member details.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MerchantMemberRequest {
        /**
         * Source member ID.
         */
        private Long sourceMemberId;

        /**
         * Member name.
         */
        @NotBlank(message = "Member name is required")
        @Size(min = 2, max = 255, message = "Member name must be between 2 and 255 characters")
        private String name;

        /**
         * Member email.
         */
        @NotBlank(message = "Member email is required")
        @Email(message = "Member email must be valid")
        private String email;

        /**
         * Bank account number.
         */
        @Size(max = 255, message = "Bank account number must not exceed 255 characters")
        private String bankAccountNumber;

        /**
         * Bank account holder name.
         */
        @Size(max = 255, message = "Bank account name must not exceed 255 characters")
        private String bankAccountName;

        /**
         * Bank name.
         */
        @Size(max = 100, message = "Bank name must not exceed 100 characters")
        private String bankName;

        /**
         * Bank IFSC code.
         */
        @Size(max = 20, message = "IFSC code must not exceed 20 characters")
        private String ifscCode;

        /**
         * Bank account type (SAVINGS, CURRENT, etc.).
         */
        @Size(max = 50, message = "Bank account type must not exceed 50 characters")
        private String bankAccountType;

        /**
         * Total amount receivable.
         */
        @DecimalMin(value = "0.00", message = "Total amount receivable must be >= 0")
        private Double totalAmountReceivable;

        /**
         * Whether member is eligible for payment.
         */
        private Boolean isEligibleForPayment;
    }

    /**
     * Nested DTO: Payment Method details.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PaymentMethodRequest {
        /**
         * Payment method type (CARD, BANK_TRANSFER, UPI, WALLET, etc.).
         */
        @NotNull(message = "Payment method is required")
        private PaymentMethod paymentMethod;

        /**
         * Card brand (VISA, MASTERCARD, AMEX, etc.).
         */
        @Size(max = 50, message = "Card brand must not exceed 50 characters")
        private String cardBrand;

        /**
         * Last 4 digits of card.
         */
        @Size(max = 4, message = "Card last 4 must be 4 characters")
        private String cardLast4;

        /**
         * Card expiry month (MM).
         */
        @Size(max = 2, message = "Card expiry month must be 2 characters")
        private String cardExpMonth;

        /**
         * Card expiry year (YYYY).
         */
        @Size(max = 4, message = "Card expiry year must be 4 characters")
        private String cardExpYear;

        /**
         * Bank name.
         */
        @Size(max = 100, message = "Bank name must not exceed 100 characters")
        private String bankName;

        /**
         * Bank account number.
         */
        @Size(max = 255, message = "Bank account number must not exceed 255 characters")
        private String bankAccountNumber;

        /**
         * Bank IFSC code.
         */
        @Size(max = 20, message = "IFSC code must not exceed 20 characters")
        private String bankIfscCode;

        /**
         * Bank account holder name.
         */
        @Size(max = 255, message = "Bank account holder name must not exceed 255 characters")
        private String bankAccountHolderName;

        /**
         * UPI ID.
         */
        @Size(max = 100, message = "UPI ID must not exceed 100 characters")
        private String upiId;

        /**
         * UPI VPA (Virtual Payment Address).
         */
        @Size(max = 100, message = "UPI VPA must not exceed 100 characters")
        private String upiVpa;
    }

    /**
     * Nested DTO: Customer details (maps to t_customers table).
     * Contains only customer identification and address information.
     * Payment method details are stored separately in PaymentMethodRequest.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CustomerDetailsRequest {
        /**
         * Client Master ID - Required.
         * Links customer to the parent client entity.
         */
        @NotNull(message = "Client Master ID is required")
        private Long clientMasterId;

        /**
         * Customer name.
         */
        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 255, message = "Customer name must be between 2 and 255 characters")
        private String customerName;

        /**
         * Customer email.
         */
        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email must be valid")
        private String customerEmail;

        /**
         * Customer phone number.
         */
        @Size(max = 20, message = "Customer phone must not exceed 20 characters")
        private String customerPhone;

        /**
         * Address line 1.
         */
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        private String addressLine1;

        /**
         * Address line 2.
         */
        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        private String addressLine2;

        /**
         * City.
         */
        @Size(max = 100, message = "City must not exceed 100 characters")
        private String city;

        /**
         * State.
         */
        @Size(max = 100, message = "State must not exceed 100 characters")
        private String state;

        /**
         * Pin code.
         */
        @Size(max = 20, message = "Pin code must not exceed 20 characters")
        private String pinCode;

        /**
         * Country.
         */
        @Size(max = 100, message = "Country must not exceed 100 characters")
        private String country;
    }
}
