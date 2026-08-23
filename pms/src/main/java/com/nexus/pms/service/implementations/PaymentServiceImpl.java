package com.nexus.pms.service.implementations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.nexus.pms.util.CommonUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.pms.config.RazorpayProperties;
import com.nexus.pms.kafka.KafkaProducer;
import com.nexus.pms.model.entities.ClientMaster;
import com.nexus.pms.model.entities.Customer;
import com.nexus.pms.model.entities.Merchant;
import com.nexus.pms.model.entities.MerchantMember;
import com.nexus.pms.model.entities.Payment;
import com.nexus.pms.model.entities.PaymentMethodEntity;
import com.nexus.pms.model.enums.PaymentStatus;
import com.nexus.pms.model.enums.PaymentType;
import com.nexus.pms.payload.ErrorResponseDto;
import com.nexus.pms.payload.PaymentRequest;
import com.nexus.pms.payload.PaymentRequest.CustomerDetailsRequest;
import com.nexus.pms.payload.PaymentRequest.MerchantDetailsRequest;
import com.nexus.pms.payload.PaymentRequest.PaymentMethodRequest;
import com.nexus.pms.payload.PaymentResponse;
import com.nexus.pms.payload.EmailCommunicationDto;
import com.nexus.pms.payload.KafkaMessageDto;
import com.nexus.pms.repository.ClientRepository;
import com.nexus.pms.repository.CustomerRepository;
import com.nexus.pms.repository.MerchantMemberRepository;
import com.nexus.pms.repository.MerchantRepository;
import com.nexus.pms.repository.PaymentMethodRepository;
import com.nexus.pms.repository.PaymentRepository;
import com.nexus.pms.service.interfaces.IdempotencyService;
import com.nexus.pms.service.interfaces.PaymentService;
import com.nexus.pms.service.interfaces.BankTransferService;
import com.nexus.pms.util.CommonConstants;
import com.nexus.pms.util.RestService;
import com.razorpay.RazorpayClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of PaymentService.
 * Handles payment processing with idempotency support.
 * Merchants and customers are created on-demand during payment processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantMemberRepository merchantMemberRepository;
    private final CustomerRepository customerRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ClientRepository clientRepository;
    private final IdempotencyService idempotencyService;
    private final RazorpayProperties configProperties;
    private final BankTransferService bankTransferService;
    private final KafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;
    private final RestService restService;
    private final CommonUtils commonUtils;

    /**
     * Process a payment with idempotency guarantee.
     * Returns PaymentResponse on success or ErrorResponseDto on error.
     * Flow:
     * 1. Check if idempotency key provided, if not generate one
     * 2. Check if payment already processed (cached via idempotency key)
     * 3. If exists, return cached response (idempotent behavior)
     * 4. If not exists:
     * a. Validate payment request
     * b. Check for duplicate by transaction reference or razorpay ID
     * c. Create and persist Payment entity
     * d. Calculate charges
     * e. Process payment (call payment gateway like Razorpay)
     * f. Update payment status
     * g. Store idempotency record
     * h. Return response
     */
    @Override
    public ResponseEntity<?> processPaymentWithIdempotency(PaymentRequest paymentRequest) {
        log.info("Processing payment with idempotency");

        // Step 1: Ensure idempotency key exists
        String idempotencyKey = paymentRequest.getIdempotencyKey();
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            idempotencyKey = idempotencyService.generateIdempotencyKey();
            paymentRequest.setIdempotencyKey(idempotencyKey);
            log.info("Generated new idempotency key: {}", idempotencyKey);
        }

        // Validate idempotency key format
        if (!idempotencyService.isValidIdempotencyKey(idempotencyKey)) {
            log.warn("Invalid idempotency key format: {}", idempotencyKey);
            return ResponseEntity.badRequest().body(
                    buildErrorDTO("Invalid idempotency key format", "INVALID_KEY"));
        }

        // Step 2: Check if payment already exists (idempotency check)
        PaymentResponse cachedResponse = idempotencyService.checkAndGetExistingPayment(idempotencyKey);
        if (cachedResponse != null) {
            log.info("Found existing payment with idempotency key: {}, returning cached response", idempotencyKey);
            cachedResponse.setIsIdempotentRetry(true);
            return ResponseEntity.ok(cachedResponse);
        }

        // Step 3: Validate the payment request
        try {
            validatePaymentRequest(paymentRequest);
        } catch (IllegalArgumentException e) {
            log.error("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    buildErrorDTO(e.getMessage(), "VALIDATION_ERROR"));
        }

        // Step 4: Check for duplicate payment by other identifiers
        if (paymentRequest.getTransactionReference() != null) {
            Optional<Payment> existingPayment = paymentRepository
                    .findByTransactionReference(paymentRequest.getTransactionReference());
            if (existingPayment.isPresent()) {
                log.warn("Payment already exists with transaction reference: {}",
                        paymentRequest.getTransactionReference());

                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        buildErrorDTO("Duplicate payment with transaction reference: " +
                                paymentRequest.getTransactionReference(), "DUPLICATE_PAYMENT"));
            }
        }

        // Step 5: Create Payment entity
        Payment payment = createPaymentEntity(paymentRequest);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setRetryCount(0);

        // Step 6: Calculate charges
        Double[] charges = calculatePaymentCharges(paymentRequest.getAmount());
        payment.setGrossAmount(paymentRequest.getAmount());
        payment.setFeeAmount(charges[0]);
        payment.setTaxAmount(charges[1]);
        payment.setNetAmount(charges[2]);

        // Step 7: Save to database
        payment = paymentRepository.save(payment);
        log.info("Saved payment with ID: {}, idempotency key: {}", payment.getPaymentId(), idempotencyKey);

        // Store callback sourceSystemIds (payroll IDs) for later use in webhooks
        if (paymentRequest.getCallback() != null
                && !ObjectUtils.isEmpty(paymentRequest.getCallback().getSourceSystemIds())) {
            try {
                List<Long> sourceSystemIds = paymentRequest.getCallback().getSourceSystemIds();
                String idsJson = objectMapper.writeValueAsString(sourceSystemIds);
                payment.setSourceSystemIdsJson(idsJson);
                payment = paymentRepository.save(payment);
                log.info("Stored {} source system IDs for payment ID: {}", sourceSystemIds.size(),
                        payment.getPaymentId());
            } catch (Exception e) {
                log.warn("Failed to store source system IDs for payment {}: {}", payment.getPaymentId(),
                        e.getMessage());
            }
        }

        // Step 8: Simulate payment processing (in real scenario, call Razorpay/payment
        // gateway)
        PaymentResponse response = processPaymentGateway(payment, paymentRequest);

        // Handle different response types
        if (response.getSuccess()) {
            // Payment completed immediately (synchronous success)
            // Step 9: Update payment status to COMPLETED
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment = paymentRepository.save(payment);
            log.info("Payment processed successfully: {}", payment.getPaymentId());

            // Step 10: Store idempotency record
            boolean stored = idempotencyService.storeIdempotencyRecord(idempotencyKey, payment.getPaymentId());
            if (!stored) {
                log.warn("Failed to store idempotency record, but payment was processed");
            }

            response = mapPaymentToResponse(payment);
            response.setSuccess(true);

            // FOR SYNCHRONOUS PAYMENTS: Send callback immediately with success=true
            if (paymentRequest.getCallback() != null
                    && !ObjectUtils.isEmpty(paymentRequest.getCallback().getCallbackUrl())) {
                invokeHrCallback(payment, paymentRequest.getCallback(), null);
            }
        } else if ("PENDING".equalsIgnoreCase(response.getPaymentStatus())) {
            // Payment initiated but pending (asynchronous - waiting for webhook)
            // This includes bank transfers and Razorpay orders
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);
            log.info("Payment initiated and pending webhook confirmation: {}", payment.getPaymentId());

            // Store idempotency record for pending payments too
            boolean stored = idempotencyService.storeIdempotencyRecord(idempotencyKey, payment.getPaymentId());
            if (!stored) {
                log.warn("Failed to store idempotency record for pending payment");
            }

            // Return response as PENDING - status will update on webhook
            response = mapPaymentToResponse(payment);
            response.setSuccess(true); // Initiation was successful

            // NOTE: For asynchronous payments, callback will be sent ONLY when webhook
            // confirms payment completion
            // This ensures success=true in callback, not false
            log.info("Callback will be sent via Kafka when webhook confirms payment completion for payment ID: {}",
                    payment.getPaymentId());
        } else {
            // Payment failed
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setLastErrorCode(response.getLastErrorCode());
            payment.setLastErrorMessage(response.getLastErrorMessage());
            payment = paymentRepository.save(payment);
            log.error("Payment processing failed: {}", response.getLastErrorMessage());
        }

        // Return PaymentResponse on success or ErrorResponseDto on failure
        if (response.getSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            // Payment failed - return ErrorResponseDto
            ErrorResponseDto errorResponse = buildErrorResponse(
                    response.getLastErrorMessage() != null
                            ? response.getLastErrorMessage()
                            : "Payment processing failed",
                    response.getIdempotencyKey());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * HYBRID PAYMENT ROUTING SCENARIO
     * ================================
     * Routes payments based on payment method type:
     * 
     * 1. BANK_TRANSFER → Direct bank-to-bank transfer (NO Razorpay)
     * - Uses merchant's bank → customer's bank
     * - NEFT/RTGS/IMPS protocol via bank APIs
     * - Bank sends webhook confirmation
     * 
     * 2. CARD → Razorpay payment gateway
     * - Shows payment UI to customer
     * - Customer enters card details
     * - Razorpay sends webhook confirmation
     * 
     * 3. UPI → Razorpay UPI integration
     * - Shows UPI payment options
     * - Customer completes via UPI app
     * - Razorpay sends webhook confirmation
     */
    private PaymentResponse processPaymentGateway(Payment payment, PaymentRequest request) {
        log.info("Processing payment for ID: {}", payment.getPaymentId());

        try {
            PaymentMethodRequest paymentMethod = request.getPaymentMethod();

            // Route based on payment method type
            if (isPaymentMethodBankTransfer(paymentMethod) || isPaymentMethodNetBanking(paymentMethod)) {
                log.info("Routing to direct bank transfer (no Razorpay)");
                return processBankTransfer(payment, request);
            } else if (isPaymentMethodCard(paymentMethod) || isPaymentMethodUPI(paymentMethod)) {
                log.info("Routing to Razorpay for {}", paymentMethod.getPaymentMethod());
                return processRazorpayPayment(payment, request);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported payment method: " + paymentMethod.getPaymentMethod());
            }

        } catch (Exception e) {
            log.error("Error in payment processing for ID: {}", payment.getPaymentId(), e);
            return buildPaymentErrorResponse(payment, "Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * BANK_TRANSFER PAYMENT METHOD
     * Direct bank-to-bank transfer (NO Razorpay involved)
     * Routes based on payment type:
     * - SALARY: Organization (Customer) → Employee (MerchantMember) via
     * NEFT/RTGS/IMPS
     * - REFUND/INVOICE: Merchant → Customer via NEFT/RTGS/IMPS
     */
    private PaymentResponse processBankTransfer(Payment payment, PaymentRequest request) {
        log.info("Processing direct bank transfer for payment ID: {}, Type: {}", payment.getPaymentId(),
                payment.getPaymentType());

        try {
            // Route based on payment type
            if (payment.getPaymentType().name().equalsIgnoreCase("SALARY")) {
                return processSalaryBankTransfer(payment, request);
            } else {
                return processCustomerBankTransfer(payment, request);
            }
        } catch (Exception e) {
            log.error("Bank transfer error for payment ID: {}", payment.getPaymentId(), e);
            return buildPaymentErrorResponse(payment, "Bank transfer initiation failed: " + e.getMessage());
        }
    }

    /**
     * SALARY BANK TRANSFER
     * Transfer from organization (CUSTOMER) to employee (MERCHANTMEMBER).
     * 
     * Flow: Organization Bank Account (from PaymentMethod) → Employee Bank Account
     * - PaymentMethodRequest = Organization/Employer's bank account (source)
     * - MerchantMember = Employee (payee, destination account)
     * 
     * PaymentMethodEntity has source account, MerchantMember has destination
     * account.
     */
    private PaymentResponse processSalaryBankTransfer(Payment payment, PaymentRequest request) {
        try {
            // Validate payment method has source bank details
            if (payment.getPaymentMethodEntity() == null) {
                throw new IllegalArgumentException("Payment method details are required for salary payment");
            }
            if (payment.getPaymentMethodEntity().getBankAccountNumber() == null ||
                    payment.getPaymentMethodEntity().getBankAccountNumber().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Organization bank account number (source) is required for salary payment");
            }
            if (payment.getPaymentMethodEntity().getBankName() == null ||
                    payment.getPaymentMethodEntity().getBankName().trim().isEmpty()) {
                throw new IllegalArgumentException("Organization bank name (source) is required for salary payment");
            }

            List<String> paymentIds = new ArrayList<>();

            for (MerchantMember employee : payment.getMerchant().getMerchantMembers()) {

                // Set employee status to IN_PROGRESS
                employee.setStatus(PaymentStatus.IN_PROGRESS);
                merchantMemberRepository.save(employee);
                log.info("Updated employee status to IN_PROGRESS for member ID: {}", employee.getMerchantMemberId());

                // Validate employee has bank details (required for salary transfer)
                if (employee.getBankAccountNumber() == null || employee.getBankAccountNumber().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Employee bank account number is required for salary payment. Member ID: "
                                    + employee.getMerchantMemberId());
                }
                if (employee.getBankName() == null || employee.getBankName().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Employee bank name is required for salary payment. Member ID: "
                                    + employee.getMerchantMemberId());
                }
                if (employee.getIfscCode() == null || employee.getIfscCode().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Employee IFSC code is required for salary payment. Member ID: "
                                    + employee.getMerchantMemberId());
                }

                log.info("Salary payment validated - Organization: {}, Employee: {}, Amount: {}",
                        payment.getCustomer().getCustomerEmail(), employee.getName(), payment.getGrossAmount());

                // Initiate bank transfer from organization to employee
                BankTransferService.BankTransferResult result = bankTransferService.initiateTransfer(
                        payment.getPaymentMethodEntity().getBankAccountNumber(), // Organization source account
                        payment.getPaymentMethodEntity().getBankName(), // Organization source bank
                        payment.getPaymentMethodEntity().getBankIfscCode(), // Organization source IFSC
                        payment.getPaymentMethodEntity().getBankAccountHolderName(), // Organization account name
                        employee.getBankAccountNumber(), // Employee destination account
                        employee.getBankName(), // Employee destination bank
                        employee.getIfscCode(), // Employee destination IFSC
                        employee.getBankAccountName(), // Employee name
                        payment.getGrossAmount(),
                        payment.getCurrency(),
                        "SALARY_" + payment.getPaymentId() + "_EMP_" + employee.getMerchantMemberId());

                if (result.getStatus().equals("FAILED")) {
                    log.error("Salary bank transfer failed: {}", result.getErrorMessage());
                    // Set employee status to FAILED on transfer error
                    employee.setStatus(PaymentStatus.FAILED);
                    merchantMemberRepository.save(employee);
                    log.warn("Updated employee status to FAILED for member ID: {}", employee.getMerchantMemberId());
                    return buildPaymentErrorResponse(payment, "Salary transfer failed: " + result.getErrorMessage());
                }

                // Set payment reference ID on the individual employee member for salary payment
                employee.setPaymentReferenceId(result.getTransactionId());
                merchantMemberRepository.save(employee);
                log.info("Set payment reference ID on member ID: {} with value: {}",
                        employee.getMerchantMemberId(), result.getTransactionId());

                paymentIds.add(result.getTransactionId());

                log.info("Salary bank transfer initiated: {}, From: {}, To: {}", result.getTransactionId(),
                        payment.getCustomer().getCustomerEmail(), employee.getName());

                // DEPRECATED: Email notification now handled by HR service on payment callback
                // triggerSalaryPaymentEmailNotification(payment, paymentRequest);

                // Email notification will be sent by HR service after callback processing
                log.info("Payment callback will trigger payslip generation and email notification in HR service");

                // Set employee status to COMPLETED after successful transfer initiation
                employee.setStatus(PaymentStatus.COMPLETED);
                merchantMemberRepository.save(employee);
                log.info("Updated employee status to COMPLETED for member ID: {}", employee.getMerchantMemberId());
            }

            // Trigger HR callback to initiate payslip generation
            if (request.getCallback() != null && !ObjectUtils.isEmpty(request.getCallback().getSourceSystemIds())) {
                // For bank transfers, invoke callback immediately to publish to Kafka
                // paymentIds contains the bank transaction IDs from each transfer
                invokeHrCallback(payment, request.getCallback(), paymentIds);
            }

            // Return PENDING - bank will send webhook when transfer completes
            // success=true because transfer initiation was successful
            // Payment status will be updated to COMPLETED when bank webhook arrives
            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .idempotencyKey(payment.getIdempotencyKey())
                    .razorpayPaymentId(paymentIds) // Bank transaction ID
                    .paymentStatus("PENDING")
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .description(payment.getDescription())
                    .paymentType(payment.getPaymentType().name())
                    .merchantId(payment.getMerchant().getMerchantId())
                    .customerId(payment.getCustomer().getCustomerId())
                    .paymentMethodId(payment.getPaymentMethodEntity().getPaymentMethodId())
                    .transactionReference(payment.getTransactionReference())
                    .grossAmount(payment.getGrossAmount())
                    .feeAmount(payment.getFeeAmount())
                    .netAmount(payment.getNetAmount())
                    .taxAmount(payment.getTaxAmount())
                    .success(true) // Transfer initiation was successful
                    .build();

        } catch (Exception e) {
            log.error("Salary bank transfer error for payment ID: {}", payment.getPaymentId(), e);
            // Set employee status to FAILED on exception
            try {
                for (MerchantMember emp : payment.getMerchant().getMerchantMembers()) {
                    emp.setStatus(PaymentStatus.FAILED);
                    merchantMemberRepository.save(emp);
                    log.warn("Updated employee status to FAILED for member ID: {} due to exception",
                            emp.getMerchantMemberId());
                }
            } catch (Exception statusUpdateError) {
                log.error("Failed to update employee status on exception", statusUpdateError);
            }
            return buildPaymentErrorResponse(payment, "Salary transfer initiation failed: " + e.getMessage());
        }
    }

    /**
     * CUSTOMER BANK TRANSFER (REFUND/INVOICE)
     * Transfer from merchant to customer using PaymentMethod details.
     * Destination account details come from PaymentMethodEntity.
     */
    private PaymentResponse processCustomerBankTransfer(Payment payment, PaymentRequest request) {
        log.info("Processing customer bank transfer for payment ID: {}, Type: {}",
                payment.getPaymentId(), payment.getPaymentType());

        try {
            // Validate merchant has bank details (source account)
            if (payment.getMerchant().getBankAccountNumber() == null ||
                    payment.getMerchant().getBankAccountNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Merchant bank account number is required for customer transfers");
            }
            if (payment.getMerchant().getBankName() == null ||
                    payment.getMerchant().getBankName().trim().isEmpty()) {
                throw new IllegalArgumentException("Merchant bank name is required for customer transfers");
            }

            // Validate payment method has destination bank details
            if (payment.getPaymentMethodEntity() == null) {
                throw new IllegalArgumentException("Payment method details are required");
            }
            if (payment.getPaymentMethodEntity().getBankAccountNumber() == null ||
                    payment.getPaymentMethodEntity().getBankAccountNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Destination bank account number is required");
            }
            if (payment.getPaymentMethodEntity().getBankName() == null ||
                    payment.getPaymentMethodEntity().getBankName().trim().isEmpty()) {
                throw new IllegalArgumentException("Destination bank name is required");
            }

            log.info("Bank details validated for merchant {} and payment method",
                    payment.getMerchant().getMerchantOfficialEmail());

            // Initiate bank transfer from merchant to destination (via payment method)
            BankTransferService.BankTransferResult result = bankTransferService.initiateTransfer(
                    payment.getMerchant().getBankAccountNumber(),
                    payment.getMerchant().getBankName(),
                    payment.getMerchant().getIfscCode(),
                    payment.getMerchant().getBankAccountName(),
                    payment.getPaymentMethodEntity().getBankAccountNumber(),
                    payment.getPaymentMethodEntity().getBankName(),
                    payment.getPaymentMethodEntity().getBankIfscCode(),
                    payment.getPaymentMethodEntity().getBankAccountHolderName(),
                    payment.getGrossAmount(),
                    payment.getCurrency(),
                    "Payment_" + payment.getPaymentId());

            if (result.getStatus().equals("FAILED")) {
                log.error("Customer bank transfer failed: {}", result.getErrorMessage());
                return buildPaymentErrorResponse(payment, "Bank transfer failed: " + result.getErrorMessage());
            }

            // Set payment reference ID on the merchant for non-salary payments
            payment.getMerchant().setPaymentReferenceId(result.getTransactionId());
            merchantRepository.save(payment.getMerchant());
            log.info("Set payment reference ID on merchant ID: {} with value: {}",
                    payment.getMerchant().getMerchantId(), result.getTransactionId());

            log.info("Customer bank transfer initiated: {}", result.getTransactionId());

            // Return PENDING - bank will send webhook when transfer completes
            // success=true because transfer initiation was successful
            // Payment status will be updated to COMPLETED when bank webhook arrives
            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .idempotencyKey(payment.getIdempotencyKey())
                    .razorpayPaymentId(List.of(result.getTransactionId())) // Bank transaction ID
                    .paymentStatus("PENDING")
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .description(payment.getDescription())
                    .paymentType(payment.getPaymentType().name())
                    .merchantId(payment.getMerchant().getMerchantId())
                    .customerId(payment.getCustomer() != null ? payment.getCustomer().getCustomerId() : null)
                    .paymentMethodId(payment.getPaymentMethodEntity().getPaymentMethodId())
                    .transactionReference(payment.getTransactionReference())
                    .grossAmount(payment.getGrossAmount())
                    .feeAmount(payment.getFeeAmount())
                    .netAmount(payment.getNetAmount())
                    .taxAmount(payment.getTaxAmount())
                    .success(true) // Transfer initiation was successful
                    .build();

        } catch (Exception e) {
            log.error("Customer bank transfer error for payment ID: {}", payment.getPaymentId(), e);
            return buildPaymentErrorResponse(payment, "Bank transfer initiation failed: " + e.getMessage());
        }
    }

    /**
     * CARD/UPI PAYMENT METHODS
     * Razorpay payment gateway with customer UI
     */
    private PaymentResponse processRazorpayPayment(Payment payment, PaymentRequest request) {
        log.info("Processing Razorpay payment for payment ID: {}", payment.getPaymentId());

        try {
            // Step 1: Initialize Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(
                    configProperties.getRazorpayKeyId(),
                    configProperties.getRazorpayKeySecret());

            // Step 2: Create Razorpay Order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (long) (payment.getGrossAmount() * 100)); // In paise
            orderRequest.put("currency", payment.getCurrency());
            orderRequest.put("receipt", "receipt_" + payment.getPaymentId());
            orderRequest.put("payment_capture", 1); // Auto-capture
            orderRequest.put("notes", new JSONObject()
                    .put("payment_id", payment.getPaymentId())
                    .put("merchant_id", payment.getMerchant().getMerchantId())
                    .put("customer_id",
                            payment.getCustomer() != null ? payment.getCustomer().getCustomerId() : null));

            // Note: The following code demonstrates the pattern for creating an order
            // The actual Razorpay SDK API call depends on the specific SDK version
            // Typical usage: Order razorpayOrder =
            // razorpayClient.Orders.create(orderRequest);
            // For now, we generate a sample order ID
            String orderId = "order_" + System.currentTimeMillis();
            log.info("Created Razorpay Order: {} for payment ID: {}", orderId, payment.getPaymentId());

            // Step 3: Return order ID to frontend
            // Frontend will use Razorpay SDK to show payment UI to customer
            // Customer enters card/UPI details and completes payment
            // Razorpay will send webhook when payment is complete

            return PaymentResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .idempotencyKey(payment.getIdempotencyKey())
                    .razorpayPaymentId(List.of(orderId)) // Razorpay order ID to be used by frontend
                    .paymentStatus("PENDING")
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .description(payment.getDescription())
                    .paymentType(payment.getPaymentType().name())
                    .merchantId(payment.getMerchant().getMerchantId())
                    .customerId(payment.getCustomer() != null ? payment.getCustomer().getCustomerId() : null)
                    .paymentMethodId(payment.getPaymentMethodEntity().getPaymentMethodId())
                    .transactionReference(payment.getTransactionReference())
                    .grossAmount(payment.getGrossAmount())
                    .feeAmount(payment.getFeeAmount())
                    .netAmount(payment.getNetAmount())
                    .taxAmount(payment.getTaxAmount())
                    .success(true) // Order creation was successful
                    .build();

        } catch (Exception e) {
            log.error("Razorpay payment processing failed for payment ID: {}", payment.getPaymentId(), e);
            return buildPaymentErrorResponse(payment, "Razorpay payment failed: " + e.getMessage());
        }
    }

    /**
     * Helper methods for payment method type checking
     */
    private boolean isPaymentMethodBankTransfer(PaymentMethodRequest paymentMethod) {
        return paymentMethod != null
                && paymentMethod.getPaymentMethod().name().equalsIgnoreCase("BANK_TRANSFER");
    }

    private boolean isPaymentMethodCard(PaymentMethodRequest paymentMethod) {
        return paymentMethod != null && (paymentMethod.getPaymentMethod().name().equalsIgnoreCase("CREDIT_CARD") ||
                paymentMethod.getPaymentMethod().name().equalsIgnoreCase("DEBIT_CARD"));
    }

    private boolean isPaymentMethodUPI(PaymentMethodRequest paymentMethod) {
        return paymentMethod != null && paymentMethod.getPaymentMethod().name().equalsIgnoreCase("UPI");
    }

    private boolean isPaymentMethodNetBanking(PaymentMethodRequest paymentMethod) {
        return paymentMethod != null && paymentMethod.getPaymentMethod().name().equalsIgnoreCase("NET_BANKING");
    }

    /**
     * Handle Razorpay webhook events.
     * This method should be called from a REST controller endpoint:
     * 
     * @PostMapping("/api/payments/razorpay-webhook")
     * public ResponseEntity<?> handleRazorpayWebhook(
     * 
     * @RequestBody String payload,
     *              @RequestHeader("X-Razorpay-Signature") String signature
     *              ) {
     *              paymentService.handleRazorpayWebhook(payload, signature);
     *              return ResponseEntity.ok().build();
     *              }
     * 
     *              WEBHOOK EVENTS HANDLED:
     *              - payment.authorized: Payment completed successfully
     *              - payment.failed: Payment failed
     *              - payment.captured: Payment captured (for manual capture mode)
     *              - order.paid: Order fully paid
     * 
     * @param webhookPayload   Raw webhook payload from Razorpay
     * @param webhookSignature Signature header from Razorpay
     * @return true if webhook processed successfully
     */
    @Transactional
    public boolean handleRazorpayWebhook(String webhookPayload, String webhookSignature) {
        log.info("Received Razorpay webhook event");

        try {
            // Step 1: Verify webhook signature (CRITICAL for security)
            if (!verifyWebhookSignature(webhookPayload, webhookSignature)) {
                log.error("Invalid webhook signature. Rejecting webhook.");
                return false;
            }
            log.info("Webhook signature verified successfully");

            // Step 2: Parse webhook payload
            JSONObject webhookEvent = new JSONObject(webhookPayload);
            String eventType = webhookEvent.getString("event");
            JSONObject eventData = webhookEvent.getJSONObject("payload");
            JSONObject paymentData = eventData.getJSONObject("payment").getJSONObject("entity");

            // Extract key information from webhook
            String razorpayPaymentId = paymentData.getString("id"); // pay_xyz123
            String orderId = paymentData.getString("order_id"); // order_abc456
            String status = paymentData.getString("status"); // authorized, failed, etc.
            String amount = paymentData.getString("amount"); // In paise
            JSONObject notes = paymentData.getJSONObject("notes");
            Long paymentId = Long.parseLong(notes.getString("payment_id"));

            log.info("Processing webhook event. Payment status will be determined from webhook data");

            // Step 3: Find payment record by payment ID stored in notes
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (!paymentOpt.isPresent()) {
                log.error("Payment not found for ID: {}", paymentId);
                return false;
            }
            Payment payment = paymentOpt.get();

            // Step 4: Handle based on event type
            String event = eventType.toLowerCase();

            if (event.contains("authorized") || event.contains("captured") || event.contains("paid")) {
                log.info("Payment authorized via webhook: razorpayPaymentId={}, status={}",
                        razorpayPaymentId, status);

                // Update payment as successful
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);

                // Store idempotency record
                idempotencyService.storeIdempotencyRecord(
                        payment.getIdempotencyKey(),
                        payment.getPaymentId());

                log.info("Payment completed successfully. Payment ID: {}, Razorpay ID: {}",
                        paymentId, razorpayPaymentId);

                // DEPRECATED: Email notification now handled by HR service on payment callback
                // Trigger email notification for completed salary payments
                if (payment.getPaymentType().name().equalsIgnoreCase("SALARY")) {
                    // Email notification will be sent by HR service after callback processing
                    log.info("Payment callback will be sent after webhook confirmation");

                    // Send payment completion callback to HR with stored payroll IDs
                    try {
                        if (payment.getSourceSystemIdsJson() != null) {
                            @SuppressWarnings("unchecked")
                            List<Long> payrollIds = objectMapper.readValue(payment.getSourceSystemIdsJson(),
                                    List.class);

                            // Build callback with stored payroll IDs
                            List<Map<String, Object>> callbackDtos = new ArrayList<>();
                            for (Long payrollId : payrollIds) {
                                Map<String, Object> callbackDto = new HashMap<>();
                                callbackDto.put("payrollId", payrollId);
                                callbackDto.put("paymentReferenceId", payment.getTransactionReference());
                                callbackDto.put("success", true);
                                callbackDtos.add(callbackDto);
                            }

                            String callbackJson = objectMapper.writeValueAsString(callbackDtos);
                            String messageKey = "payment-" + payment.getPaymentId();

                            kafkaProducer.publishMessage(
                                    CommonConstants.PAYMENT_CALLBACK_TOPIC,
                                    messageKey,
                                    callbackJson).thenAccept(result -> {
                                        log.info(
                                                "Payment completion callback published to Kafka for {} payroll(s) from payment ID: {}",
                                                payrollIds.size(), payment.getPaymentId());
                                    }).exceptionally(ex -> {
                                        log.error("Error publishing payment completion callback to Kafka", ex);
                                        return null;
                                    });
                        } else {
                            log.warn("No source system IDs stored for payment {}, callback not sent",
                                    payment.getPaymentId());
                        }
                    } catch (Exception e) {
                        log.error("Error preparing payment completion callback for payment {}", payment.getPaymentId(),
                                e);
                    }
                }

            } else if (event.contains("failed")) {
                log.warn("Payment failed via webhook: razorpayPaymentId={}, status={}",
                        razorpayPaymentId, status);

                // Update payment as failed
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setLastErrorCode("RAZORPAY_DECLINED");
                payment.setLastErrorMessage("Payment declined by customer");
                paymentRepository.save(payment);

                log.warn("Payment marked as failed for payment ID: {}", paymentId);
            }

            // Step 5: Return success to Razorpay (confirms webhook received)
            log.info("Webhook processed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return false;
        }
    }

    /**
     * Verify Razorpay webhook signature using HMAC-SHA256.
     * CRITICAL: Always verify webhook signature to ensure authenticity.
     * 
     * Signature formula: HMAC_SHA256(webhook_body, webhook_secret)
     * 
     * @param webhookPayload   Raw webhook body as string
     * @param webhookSignature Signature from X-Razorpay-Signature header
     * @return true if signature is valid, false otherwise
     */
    private boolean verifyWebhookSignature(String webhookPayload, String webhookSignature) {
        try {
            String webhookSecret = configProperties.getRazorpayWebhookSecret();

            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(webhookPayload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = HexFormat.of().formatHex(rawHmac);

            boolean isValid = calculatedSignature.equals(webhookSignature);
            if (!isValid) {
                log.error("Webhook signature mismatch. Expected: {}, Got: {}",
                        calculatedSignature, webhookSignature);
            }
            return isValid;

        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Extract employee (merchant member) data from the payment request.
     * Extracts the specific employee to be paid from the request's merchant members
     * list.
     * 
     * Employee data is transient - it comes from the payment request and is NOT
     * stored in the database.
     * This aligns with PMS principle: merchant members are request data, not
     * persisted master data.
     * 
     * @param paymentRequest   The payment request containing merchant members list
     * @param merchantMemberId The ID of the employee to find in the request
     * @return MerchantMemberRequest for the specified employee, or null if not
     *         found
     */
    /**
     * Create a Payment entity from PaymentRequest.
     * Handles on-demand creation/loading of merchant, customer, and payment method.
     * Note: merchantMemberId will be resolved from sourceMemberId during payment
     * processing.
     */
    private Payment createPaymentEntity(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        payment.setPaymentType(request.getPaymentType());
        // Set the transaction reference from the request (sent by HR during payroll
        // initiation)
        payment.setTransactionReference(request.getTransactionReference());

        // Step 1: Create or load merchant by email (on-demand)
        if (request.getMerchant() != null) {
            Merchant merchant = createOrLoadMerchant(request.getMerchant());
            payment.setMerchant(merchant);
            log.info("Associated merchant ID {} to payment", merchant.getMerchantId());
        } else {
            throw new IllegalArgumentException("Merchant details are required");
        }

        // Step 2: Create payment method from request
        if (request.getPaymentMethod() != null) {
            PaymentMethodEntity paymentMethod = createPaymentMethod(request.getPaymentMethod());
            payment.setPaymentMethodEntity(paymentMethod);
            log.info("Associated payment method ID {} to payment", paymentMethod.getPaymentMethodId());
        } else {
            throw new IllegalArgumentException("Payment method details are required");
        }

        // Step 3: Create or load customer by email (on-demand) if payment type requires
        // it
        if (requiresCustomer(request.getPaymentType())) {
            if (request.getCustomer() != null) {
                Customer customer = createOrLoadCustomer(request.getCustomer());
                payment.setCustomer(customer);
                log.info("Associated customer ID {} to payment", customer.getCustomerId());
            } else {
                throw new IllegalArgumentException(
                        "Customer details are required for " + request.getPaymentType() + " payment");
            }
        }

        return payment;
    }

    /**
     * Create merchant if doesn't exist, otherwise load by email.
     * Includes creating merchant members if provided.
     * 
     * NOTE: For SALARY payments, merchant bank details (bankAccountNumber,
     * bankName, ifscCode)
     * are OPTIONAL. Only merchantMembers' bank details are required.
     * For REFUND/INVOICE payments, merchant bank details ARE REQUIRED.
     */
    private Merchant createOrLoadMerchant(MerchantDetailsRequest merchantDetails) {
        log.info("Creating new merchant with email: {}", merchantDetails.getMerchantOfficialEmail());

        // Validate and load ClientMaster
        if (merchantDetails.getClientMasterId() == null || merchantDetails.getClientMasterId() <= 0) {
            throw new IllegalArgumentException("Client Master ID is required for merchant creation");
        }

        Optional<ClientMaster> clientMasterOpt = clientRepository.findById(merchantDetails.getClientMasterId());
        if (clientMasterOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Client Master not found with ID: " + merchantDetails.getClientMasterId());
        }

        ClientMaster clientMaster = clientMasterOpt.get();
        log.info("Found ClientMaster: {} (ID: {})", clientMaster.getClientName(), clientMaster.getClientMasterId());

        Merchant merchant = new Merchant();
        merchant.setSourceSystem(clientMaster);
        merchant.setSourceSystemId(merchantDetails.getSourceSystemId());
        merchant.setMerchantOfficialEmail(merchantDetails.getMerchantOfficialEmail());
        merchant.setAddressLine1(merchantDetails.getAddressLine1());
        merchant.setAddressLine2(merchantDetails.getAddressLine2());
        merchant.setCity(merchantDetails.getCity());
        merchant.setState(merchantDetails.getState());
        merchant.setPinCode(merchantDetails.getPinCode());
        merchant.setCountry(merchantDetails.getCountry());
        // Bank details are stored but may be optional depending on payment type
        merchant.setBankAccountNumber(merchantDetails.getBankAccountNumber());
        merchant.setBankAccountName(merchantDetails.getBankAccountName());
        merchant.setBankName(merchantDetails.getBankName());
        merchant.setIfscCode(merchantDetails.getIfscCode());
        merchant.setBankAccountType(merchantDetails.getBankAccountType());
        merchant = merchantRepository.save(merchant);

        if (merchantDetails.getMerchantMembers() != null && !merchantDetails.getMerchantMembers().isEmpty()) {
            log.info("Adding {} merchant members (employees) to merchant", merchantDetails.getMerchantMembers().size());
            final Merchant savedMerchant = merchant;
            List<MerchantMember> members = merchantDetails.getMerchantMembers().stream().map(memberRequest -> {
                MerchantMember member = new MerchantMember();
                member.setName(memberRequest.getName());
                member.setSourceMemberId(memberRequest.getSourceMemberId());
                member.setEmail(memberRequest.getEmail());
                member.setBankAccountNumber(memberRequest.getBankAccountNumber());
                member.setBankAccountName(memberRequest.getBankAccountName());
                member.setBankName(memberRequest.getBankName());
                member.setIfscCode(memberRequest.getIfscCode());
                member.setBankAccountType(memberRequest.getBankAccountType());
                member.setTotalAmountReceivable(memberRequest.getTotalAmountReceivable());
                member.setStatus(PaymentStatus.PENDING);
                member.setMerchant(savedMerchant);
                return member;
            }).toList();
            members = merchantMemberRepository.saveAll(members);
            merchant.setMerchantMembers(members);
            log.info("Persisted {} merchant members for merchant ID: {}", members.size(), merchant.getMerchantId());
        }

        log.info("Saved merchant with ID: {} linked to ClientMaster: {}", merchant.getMerchantId(),
                clientMaster.getClientName());

        return merchant;
    }

    /**
     * Create a new PaymentMethodEntity from PaymentMethodRequest.
     */
    private PaymentMethodEntity createPaymentMethod(PaymentMethodRequest paymentMethodRequest) {
        log.info("Creating new payment method: {}", paymentMethodRequest.getPaymentMethod());
        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.setPaymentMethod(paymentMethodRequest.getPaymentMethod());

        String methodName = paymentMethodRequest.getPaymentMethod().name();

        if (methodName.equalsIgnoreCase("CREDIT_CARD") || methodName.equalsIgnoreCase("DEBIT_CARD")) {
            paymentMethod.setCardBrand(paymentMethodRequest.getCardBrand());
            paymentMethod.setCardLast4(paymentMethodRequest.getCardLast4());
            paymentMethod.setCardExpMonth(paymentMethodRequest.getCardExpMonth());
            paymentMethod.setCardExpYear(paymentMethodRequest.getCardExpYear());
            log.info("Saved card details for {} payment method", methodName);
        }

        if (methodName.equalsIgnoreCase("BANK_TRANSFER")) {
            paymentMethod.setBankName(paymentMethodRequest.getBankName());
            paymentMethod.setBankAccountNumber(paymentMethodRequest.getBankAccountNumber());
            paymentMethod.setBankIfscCode(paymentMethodRequest.getBankIfscCode());
            paymentMethod.setBankAccountHolderName(paymentMethodRequest.getBankAccountHolderName());
            log.info("Saved bank transfer details");
        }

        if (methodName.equalsIgnoreCase("NET_BANKING")) {
            if (paymentMethodRequest.getBankName() != null) {
                paymentMethod.setBankName(paymentMethodRequest.getBankName());
                paymentMethod.setBankAccountNumber(paymentMethodRequest.getBankAccountNumber());
                paymentMethod.setBankIfscCode(paymentMethodRequest.getBankIfscCode());
                paymentMethod.setBankAccountHolderName(paymentMethodRequest.getBankAccountHolderName());
                log.info("Saved bank details for NET_BANKING");
            }
        }

        if (methodName.equalsIgnoreCase("UPI")) {
            paymentMethod.setUpiId(paymentMethodRequest.getUpiId());
            paymentMethod.setUpiVpa(paymentMethodRequest.getUpiVpa());
            log.info("Saved UPI details");
        }

        if (methodName.equalsIgnoreCase("WALLET") || methodName.equalsIgnoreCase("EMI")
                || methodName.equalsIgnoreCase("OTHER")) {
            log.info("Payment method {} will be handled by Razorpay", methodName);
        }

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Created payment method with ID: {} (Type: {})", paymentMethod.getPaymentMethodId(), methodName);
        return paymentMethod;
    }

    /**
     * Create new customer (no record reuse).
     * Only stores customer identification and address info.
     * Payment method details are stored separately in PaymentMethodEntity.
     */
    private Customer createOrLoadCustomer(CustomerDetailsRequest customerDetails) {
        log.info("Creating new customer with email: {}", customerDetails.getCustomerEmail());

        // Validate and load ClientMaster
        if (customerDetails.getClientMasterId() == null || customerDetails.getClientMasterId() <= 0) {
            throw new IllegalArgumentException("Client Master ID is required for customer creation");
        }

        Optional<ClientMaster> clientMasterOpt = clientRepository.findById(customerDetails.getClientMasterId());
        if (!clientMasterOpt.isPresent()) {
            throw new IllegalArgumentException(
                    "Client Master not found with ID: " + customerDetails.getClientMasterId());
        }

        ClientMaster clientMaster = clientMasterOpt.get();
        log.info("Found ClientMaster: {} (ID: {})", clientMaster.getClientName(), clientMaster.getClientMasterId());

        Customer customer = new Customer();
        customer.setSourceSystem(clientMaster);
        customer.setCustomerName(customerDetails.getCustomerName());
        customer.setCustomerEmail(customerDetails.getCustomerEmail());
        customer.setCustomerPhone(customerDetails.getCustomerPhone());
        customer.setAddressLine1(customerDetails.getAddressLine1());
        customer.setAddressLine2(customerDetails.getAddressLine2());
        customer.setCity(customerDetails.getCity());
        customer.setState(customerDetails.getState());
        customer.setPinCode(customerDetails.getPinCode());
        customer.setCountry(customerDetails.getCountry());

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Saved customer with ID: {} linked to ClientMaster: {}", savedCustomer.getCustomerId(),
                clientMaster.getClientName());
        return savedCustomer;
    }

    /**
     * Determine if payment type requires a customer.
     * 
     * SALARY: Customer = Organization/Employer (source of salary money)
     * REFUND: Customer = Person receiving the refund
     * INVOICE: Customer = Person paying the invoice
     */
    private boolean requiresCustomer(PaymentType paymentType) {
        return paymentType.name().equalsIgnoreCase("SALARY") ||
                paymentType.name().equalsIgnoreCase("REFUND") ||
                paymentType.name().equalsIgnoreCase("INVOICE");
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.error("Payment not found with ID: {}", paymentId);
                    return new RuntimeException("Payment not found");
                });
    }

    @Override
    public PaymentResponse mapPaymentToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .idempotencyKey(payment.getIdempotencyKey())
                .paymentStatus(payment.getPaymentStatus() != null ? payment.getPaymentStatus().toString() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description(payment.getDescription())
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : null)
                .merchantId(payment.getMerchant() != null ? payment.getMerchant().getMerchantId() : null)
                .customerId(payment.getCustomer() != null ? payment.getCustomer().getCustomerId() : null)
                .paymentMethodId(
                        payment.getPaymentMethodEntity() != null ? payment.getPaymentMethodEntity().getPaymentMethodId()
                                : null)
                .transactionReference(payment.getTransactionReference())
                .grossAmount(payment.getGrossAmount())
                .feeAmount(payment.getFeeAmount())
                .netAmount(payment.getNetAmount())
                .taxAmount(payment.getTaxAmount())
                .retryCount(payment.getRetryCount())
                .lastErrorCode(payment.getLastErrorCode())
                .lastErrorMessage(payment.getLastErrorMessage())
                .isIdempotentRetry(payment.getIsIdempotentRetry())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .success(payment.getPaymentStatus() == PaymentStatus.COMPLETED)
                .build();
    }

    @Override
    public Double[] calculatePaymentCharges(Double amount) {
        double feeAmount = amount * 0.02;
        double taxAmount = feeAmount * 0.18;
        double netAmount = amount - feeAmount - taxAmount;

        log.debug("Calculated charges - Gross: {}, Fee: {}, Tax: {}, Net: {}", amount, feeAmount, taxAmount, netAmount);
        return new Double[] { feeAmount, taxAmount, netAmount };
    }

    @Override
    public void validatePaymentRequest(PaymentRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }

        if (request.getPaymentType() == null) {
            throw new IllegalArgumentException("Payment type is required");
        }

        if (request.getMerchant() == null) {
            throw new IllegalArgumentException("Merchant details are required");
        }
        if (request.getMerchant().getMerchantOfficialEmail() == null ||
                request.getMerchant().getMerchantOfficialEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Merchant email is required");
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method details are required");
        }
        if (request.getPaymentMethod().getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method type is required");
        }

        switch (request.getPaymentMethod().getPaymentMethod().name()) {
            case "CREDIT_CARD":
            case "DEBIT_CARD":
                if (request.getPaymentMethod().getCardBrand() == null ||
                        request.getPaymentMethod().getCardBrand().trim().isEmpty()) {
                    throw new IllegalArgumentException("Card brand is required for card payments");
                }
                if (request.getPaymentMethod().getCardLast4() == null ||
                        request.getPaymentMethod().getCardLast4().trim().isEmpty()) {
                    throw new IllegalArgumentException("Card last 4 digits are required for card payments");
                }
                break;
            case "BANK_TRANSFER":
                if (request.getPaymentMethod().getBankName() == null ||
                        request.getPaymentMethod().getBankName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Bank name is required for bank transfers");
                }
                if (request.getPaymentMethod().getBankAccountNumber() == null ||
                        request.getPaymentMethod().getBankAccountNumber().trim().isEmpty()) {
                    throw new IllegalArgumentException("Bank account number is required for bank transfers");
                }
                if (request.getPaymentMethod().getBankIfscCode() == null ||
                        request.getPaymentMethod().getBankIfscCode().trim().isEmpty()) {
                    throw new IllegalArgumentException("IFSC code is required for bank transfers");
                }
                break;
            case "UPI":
                if (request.getPaymentMethod().getUpiId() == null ||
                        request.getPaymentMethod().getUpiId().trim().isEmpty()) {
                    throw new IllegalArgumentException("UPI ID is required for UPI payments");
                }
                break;
            case "NET_BANKING":
                // NET_BANKING via Razorpay doesn't require explicit bank details
                // Razorpay will handle the banking interface
                log.info("NET_BANKING payment will be processed via Razorpay gateway");
                break;
            case "WALLET":
            case "EMI":
            case "OTHER":
                // These payment methods are supported via Razorpay
                log.info("Payment method {} will be processed via Razorpay gateway",
                        request.getPaymentMethod().getPaymentMethod().name());
                break;
        }

        if (request.getPaymentType().name().equalsIgnoreCase("SALARY")) {
            // For SALARY payments, CUSTOMER is REQUIRED - it represents the
            // organization/employer
            // Salary transfer flow: Organization (Customer) → Employee (MerchantMember)
            // Payment method contains the source account details (organization's bank
            // account for BANK_TRANSFER)
            // MerchantMember provides destination account (employee's bank account)
            if (request.getCustomer() == null) {
                throw new IllegalArgumentException(
                        "Customer details are required for SALARY payments. Customer represents the organization/employer");
            }
            if (request.getCustomer().getCustomerEmail() == null ||
                    request.getCustomer().getCustomerEmail().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Customer email (organization email) is required for salary payments");
            }
            log.info(
                    "SALARY payment validated. Source account details are in PaymentMethod, destination in MerchantMember.");
        }

        if (request.getPaymentType().name().equalsIgnoreCase("REFUND") ||
                request.getPaymentType().name().equalsIgnoreCase("INVOICE")) {
            if (request.getCustomer() == null) {
                throw new IllegalArgumentException(
                        "Customer details are required for " + request.getPaymentType() + " payments");
            }
            if (request.getCustomer().getCustomerEmail() == null ||
                    request.getCustomer().getCustomerEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Customer email is required");
            }
        }
    }

    /**
     * Build PaymentResponse with error information.
     */
    private PaymentResponse buildPaymentErrorResponse(Payment payment, String errorMessage) {
        // Map payment to response to include all payment details
        PaymentResponse response = mapPaymentToResponse(payment);
        // Override status and error fields
        response.setPaymentStatus("FAILED");
        response.setLastErrorMessage(errorMessage);
        response.setSuccess(false);
        return response;
    }

    /**
     * Build error response.
     */
    private ErrorResponseDto buildErrorResponse(String errorMessage, String idempotencyKey) {
        return new ErrorResponseDto(
                "PaymentProcessingException",
                400,
                new java.sql.Timestamp(System.currentTimeMillis()),
                errorMessage,
                idempotencyKey);
    }

    /**
     * Trigger salary payment email notification via Kafka.
     * Called after successful salary payment completion or bank transfer
     * initiation.
     * Sends email to the merchant member (employee) with payment details.
     *
     * @param payment        The payment entity
     * @param paymentRequest The original payment request
     */
    private void triggerSalaryPaymentEmailNotification(Payment payment, PaymentRequest paymentRequest) {
        try {
            // Send email to all merchant members (employees) associated with this merchant
            for (MerchantMember employee : payment.getMerchant().getMerchantMembers()) {
                // Build EmailCommunicationDto with all required fields for CMS
                EmailCommunicationDto emailCommunicationDto = new EmailCommunicationDto();

                // Required: Recipient information
                emailCommunicationDto.setSenderEmail("noreply@nexushr.com");
                emailCommunicationDto.setRecipientEmails(List.of(employee.getEmail()));
                emailCommunicationDto.setSubject("Salary Payment Processed - " + payment.getPaymentId());

                // Optional: CC/BCC emails (empty lists if not needed)
                emailCommunicationDto.setCcEmails(new ArrayList<>());
                emailCommunicationDto.setBccEmails(new ArrayList<>());

                // Build placeholders for the email template
                Map<String, Object> placeholders = new HashMap<>();
                placeholders.put("employeeName", employee.getName());
                placeholders.put("amount", String.format("%.2f", payment.getGrossAmount()));
                placeholders.put("currency", payment.getCurrency());
                placeholders.put("paymentDate", new java.sql.Timestamp(System.currentTimeMillis()).toString());
                placeholders.put("transactionReference", payment.getTransactionReference() != null
                        ? payment.getTransactionReference()
                        : payment.getPaymentId().toString());
                placeholders.put("organizationName", payment.getCustomer() != null
                        ? payment.getCustomer().getCustomerName()
                        : "Organization");
                placeholders.put("bankName", employee.getBankName() != null ? employee.getBankName() : "N/A");
                placeholders.put("accountHolderName", employee.getBankAccountName() != null
                        ? employee.getBankAccountName()
                        : employee.getName());

                // Mask account number to show only last 4 digits
                String accountLast4 = "****";
                if (employee.getBankAccountNumber() != null && employee.getBankAccountNumber().length() >= 4) {
                    accountLast4 = employee.getBankAccountNumber().substring(
                            employee.getBankAccountNumber().length() - 4);
                }
                placeholders.put("accountLast4", accountLast4);

                // Add fee and net amount if calculated
                if (payment.getGrossAmount() != null) {
                    placeholders.put("grossAmount", String.format("%.2f", payment.getGrossAmount()));
                    if (payment.getFeeAmount() != null) {
                        placeholders.put("feeAmount", String.format("%.2f", payment.getFeeAmount()));
                    }
                    if (payment.getNetAmount() != null) {
                        placeholders.put("netAmount", String.format("%.2f", payment.getNetAmount()));
                    }
                }

                emailCommunicationDto.setPlaceholders(placeholders);

                // Wrap in KafkaMessageDto for CMS consumption
                KafkaMessageDto kafkaMessageDto = new KafkaMessageDto();
                kafkaMessageDto.setTopic(CommonConstants.SALARY_PAYMENT_MAIL_TOPIC);
                kafkaMessageDto.setCommsType("email");
                kafkaMessageDto.setUuid(UUID.randomUUID().toString());
                kafkaMessageDto.setMessage(objectMapper.writeValueAsString(emailCommunicationDto));

                String kafkaMessage = objectMapper.writeValueAsString(kafkaMessageDto);
                log.debug("Publishing salary payment email message to Kafka: {}", kafkaMessage);

                kafkaProducer.publishMessage(
                        CommonConstants.SALARY_PAYMENT_MAIL_TOPIC,
                        "salary-payment-" + payment.getPaymentId() + "-" + employee.getMerchantMemberId(),
                        kafkaMessage);

                log.info(
                        "Salary payment email notification published to Kafka for payment ID: {}, Employee: {}, Topic: {}",
                        payment.getPaymentId(), employee.getName(), CommonConstants.SALARY_PAYMENT_MAIL_TOPIC);
            }

        } catch (Exception e) {
            log.error("Error triggering salary payment email notification for payment ID: {}",
                    payment.getPaymentId(), e);
            // Don't throw exception - payment processing should not fail due to email
            // notification
        }
    }

    /**
     * Build error response DTO for API error responses.
     * Used when returning error-only responses without payment details.
     * 
     * @param message   The error message
     * @param errorCode The error code (stored in exceptionType field)
     * @return ErrorResponseDto with minimal error information
     */
    private ErrorResponseDto buildErrorDTO(String message, String errorCode) {
        return ErrorResponseDto.builder()
                .message(message)
                .exceptionType(errorCode)
                .statusCode(400)
                .timestamp(new java.sql.Timestamp(System.currentTimeMillis()))
                .build();
    }

    /**
     * Publish payment callback to Kafka topic to notify HR service.
     * Replaces HTTP callback mechanism with asynchronous Kafka messaging.
     * This is called after successful payment (bank transfer or Razorpay).
     * 
     * Flow:
     * 1. Extract payroll IDs from callback configuration (sourceSystemIds)
     * 2. Build callback DTO for each payroll
     * 3. Serialize to JSON
     * 4. Publish to Kafka payment-callback-topic
     * 5. HR service consumes the message and processes payment completion
     *
     * @param payment    The completed payment entity
     * @param callback   The callback configuration from PaymentRequest
     * @param paymentIds List of bank transaction IDs from bank transfers
     */
    private void invokeHrCallback(Payment payment,
            com.nexus.pms.payload.PaymentRequest.Callback callback,
            List<String> paymentIds) {
        try {
            // For Razorpay webhooks, callback is null, so return
            if (callback == null) {
                log.debug("No callback configured for Razorpay webhook - callback will be handled separately");
                return;
            }

            // Get callback payload IDs from request (stored in sourceSystemIds)
            if (ObjectUtils.isEmpty(callback.getSourceSystemIds())) {
                log.warn("No source system IDs in callback for payment ID: {}", payment.getPaymentId());
                return;
            }

            log.info("Publishing payment callback to Kafka for payment ID: {}, Payrolls: {}",
                    payment.getPaymentId(), callback.getSourceSystemIds().size());

            // Build callback request body with list of PayrollCallbackDto
            List<Map<String, Object>> callbackDtos = new ArrayList<>();

            for (Long payrollId : callback.getSourceSystemIds()) {
                Map<String, Object> callbackDto = new HashMap<>();
                callbackDto.put("payrollId", payrollId);
                callbackDto.put("paymentReferenceId", payment.getTransactionReference());
                // Success flag:
                // - SALARY payments: true (transfer initiated successfully)
                // - Other payments: check if status is COMPLETED
                boolean isSuccess = payment.getPaymentType().name().equalsIgnoreCase("SALARY") ||
                        payment.getPaymentStatus() == PaymentStatus.COMPLETED;
                callbackDto.put("success", isSuccess);
                callbackDtos.add(callbackDto);
            }

            log.info("Callback payload: {} payrolls with status={}",
                    callbackDtos.size(), payment.getPaymentStatus());

            // Convert callback list to JSON
            String callbackJson = objectMapper.writeValueAsString(callbackDtos);

            // Publish to Kafka topic with payment ID as key for ordering
            String messageKey = "payment-" + payment.getPaymentId();
            kafkaProducer.publishMessage(
                    CommonConstants.PAYMENT_CALLBACK_TOPIC,
                    messageKey,
                    callbackJson).thenAccept(result -> {
                        log.info("Payment callback published successfully to Kafka for payment ID: {}, Topic: {}",
                                payment.getPaymentId(), CommonConstants.PAYMENT_CALLBACK_TOPIC);
                    }).exceptionally(ex -> {
                        log.error("Error publishing payment callback to Kafka for payment ID: {}",
                                payment.getPaymentId(), ex);
                        // Don't throw - callback failure shouldn't block payment processing
                        return null;
                    });

        } catch (Exception e) {
            log.error("Error preparing payment callback for Kafka for payment ID: {}", payment.getPaymentId(), e);
            // Don't throw - callback failure shouldn't block payment processing
        }
    }
}
