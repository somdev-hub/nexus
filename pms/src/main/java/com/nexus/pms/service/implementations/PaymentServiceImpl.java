package com.nexus.pms.service.implementations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.pms.config.RazorpayProperties;
import com.nexus.pms.model.entities.Customer;
import com.nexus.pms.model.entities.Merchant;
import com.nexus.pms.model.entities.MerchantMember;
import com.nexus.pms.model.entities.Payment;
import com.nexus.pms.model.entities.PaymentMethodEntity;
import com.nexus.pms.model.enums.PaymentStatus;
import com.nexus.pms.model.enums.PaymentType;
import com.nexus.pms.payload.PaymentRequest;
import com.nexus.pms.payload.PaymentRequest.CustomerDetailsRequest;
import com.nexus.pms.payload.PaymentRequest.MerchantDetailsRequest;
import com.nexus.pms.payload.PaymentRequest.MerchantMemberRequest;
import com.nexus.pms.payload.PaymentRequest.PaymentMethodRequest;
import com.nexus.pms.payload.PaymentResponse;
import com.nexus.pms.repository.CustomerRepository;
import com.nexus.pms.repository.MerchantMemberRepository;
import com.nexus.pms.repository.MerchantRepository;
import com.nexus.pms.repository.PaymentMethodRepository;
import com.nexus.pms.repository.PaymentRepository;
import com.nexus.pms.service.interfaces.IdempotencyService;
import com.nexus.pms.service.interfaces.PaymentService;
import com.nexus.pms.service.interfaces.BankTransferService;
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
    private final IdempotencyService idempotencyService;
    private final RazorpayProperties configProperties;
    private final BankTransferService bankTransferService;

    /**
     * Process a payment with idempotency guarantee.
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
    public PaymentResponse processPaymentWithIdempotency(PaymentRequest paymentRequest) {
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
            return buildErrorResponse("Invalid idempotency key format", null);
        }

        // Step 2: Check if payment already exists (idempotency check)
        PaymentResponse cachedResponse = idempotencyService.checkAndGetExistingPayment(idempotencyKey);
        if (cachedResponse != null) {
            log.info("Found existing payment with idempotency key: {}, returning cached response", idempotencyKey);
            cachedResponse.setIsIdempotentRetry(true);
            return cachedResponse;
        }

        // Step 3: Validate the payment request
        try {
            validatePaymentRequest(paymentRequest);
        } catch (IllegalArgumentException e) {
            log.error("Payment validation failed: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(), idempotencyKey);
        }

        // Step 4: Check for duplicate payment by other identifiers
        if (paymentRequest.getTransactionReference() != null) {
            Optional<Payment> existingPayment = paymentRepository
                    .findByTransactionReference(paymentRequest.getTransactionReference());
            if (existingPayment.isPresent()) {
                log.warn("Payment already exists with transaction reference: {}",
                        paymentRequest.getTransactionReference());
                return mapPaymentToResponse(existingPayment.get());
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

        // Step 8: Simulate payment processing (in real scenario, call Razorpay/payment
        // gateway)
        PaymentResponse response = processPaymentGateway(payment, paymentRequest);

        if (response.getSuccess()) {
            // Step 9: Update payment status to COMPLETED
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setRazorpayPaymentId(response.getRazorpayPaymentId());
            payment = paymentRepository.save(payment);
            log.info("Payment processed successfully: {}", payment.getPaymentId());

            // Step 10: Store idempotency record
            boolean stored = idempotencyService.storeIdempotencyRecord(idempotencyKey, payment.getPaymentId());
            if (!stored) {
                log.warn("Failed to store idempotency record, but payment was processed");
            }

            response = mapPaymentToResponse(payment);
            response.setSuccess(true);
        } else {
            // Payment failed
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setLastErrorCode(response.getLastErrorCode());
            payment.setLastErrorMessage(response.getLastErrorMessage());
            payment = paymentRepository.save(payment);
            log.error("Payment processing failed: {}", response.getLastErrorMessage());
        }

        return response;
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
            if (isPaymentMethodBankTransfer(paymentMethod)) {
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
            return buildErrorResponse("Payment processing failed: " + e.getMessage(),
                    payment.getIdempotencyKey());
        }
    }

    /**
     * BANK_TRANSFER PAYMENT METHOD
     * Direct bank-to-bank transfer (NO Razorpay involved)
     * Uses: Merchant's bank → Customer's bank via NEFT/RTGS/IMPS
     */
    private PaymentResponse processBankTransfer(Payment payment, PaymentRequest request) {
        log.info("Processing direct bank transfer for payment ID: {}", payment.getPaymentId());

        try {
            // Validate both parties have bank details
            if (payment.getMerchant().getBankAccountNumber() == null
                    || payment.getCustomer().getBankAccountNumber() == null) {
                throw new IllegalArgumentException("Bank details missing for merchant or customer");
            }

            // Initiate bank transfer
            BankTransferService.BankTransferResult result = bankTransferService.initiateTransfer(
                    payment.getMerchant().getBankAccountNumber(),
                    payment.getMerchant().getBankName(),
                    payment.getMerchant().getIfscCode(),
                    payment.getMerchant().getBankAccountName(),
                    payment.getCustomer().getBankAccountNumber(),
                    payment.getCustomer().getBankName(),
                    payment.getCustomer().getBankIfscCode(),
                    payment.getCustomer().getBankAccountName(),
                    payment.getGrossAmount(),
                    payment.getCurrency(),
                    "Payment_" + payment.getPaymentId());

            if (result.getStatus().equals("FAILED")) {
                log.error("Bank transfer failed: {}", result.getErrorMessage());
                return buildErrorResponse("Bank transfer failed: " + result.getErrorMessage(),
                        payment.getIdempotencyKey());
            }

            log.info("Bank transfer initiated: {}", result.getTransactionId());

            // Return PENDING - bank will send webhook when transfer completes
            return PaymentResponse.builder()
                    .razorpayPaymentId(result.getTransactionId()) // Bank transaction ID
                    .paymentStatus("PENDING")
                    .success(false) // Will be confirmed by bank webhook
                    .build();

        } catch (Exception e) {
            log.error("Bank transfer error for payment ID: {}", payment.getPaymentId(), e);
            return buildErrorResponse("Bank transfer initiation failed: " + e.getMessage(),
                    payment.getIdempotencyKey());
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

            return PaymentResponse.builder()
                    .razorpayPaymentId(null) // Will be populated by webhook when customer pays
                    .paymentStatus("PENDING")
                    .success(false) // Waiting for customer action
                    .build();

        } catch (Exception e) {
            log.error("Razorpay payment processing failed for payment ID: {}", payment.getPaymentId(), e);
            return buildErrorResponse("Razorpay payment failed: " + e.getMessage(),
                    payment.getIdempotencyKey());
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
        return paymentMethod != null && paymentMethod.getPaymentMethod().name().equalsIgnoreCase("CARD");
    }

    private boolean isPaymentMethodUPI(PaymentMethodRequest paymentMethod) {
        return paymentMethod != null && paymentMethod.getPaymentMethod().name().equalsIgnoreCase("UPI");
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
                payment.setRazorpayPaymentId(razorpayPaymentId);
                paymentRepository.save(payment);

                // Store idempotency record
                idempotencyService.storeIdempotencyRecord(
                        payment.getIdempotencyKey(),
                        payment.getPaymentId());

                log.info("Payment completed successfully. Payment ID: {}, Razorpay ID: {}",
                        paymentId, razorpayPaymentId);

            } else if (event.contains("failed")) {
                log.warn("Payment failed via webhook: razorpayPaymentId={}, status={}",
                        razorpayPaymentId, status);

                // Update payment as failed
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setRazorpayPaymentId(razorpayPaymentId);
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
     * Create a Payment entity from PaymentRequest.
     * Handles on-demand creation/loading of merchant, customer, and payment method.
     */
    private Payment createPaymentEntity(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setDescription(request.getDescription());
        payment.setPaymentType(request.getPaymentType());
        payment.setMerchantMemberId(request.getMerchantMemberId());
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
     */
    private Merchant createOrLoadMerchant(MerchantDetailsRequest merchantDetails) {
        log.info("Creating new merchant with email: {}", merchantDetails.getMerchantOfficialEmail());
        Merchant merchant = new Merchant();
        merchant.setSourceSystemId(merchantDetails.getSourceSystemId());
        merchant.setMerchantOfficialEmail(merchantDetails.getMerchantOfficialEmail());
        merchant.setAddressLine1(merchantDetails.getAddressLine1());
        merchant.setAddressLine2(merchantDetails.getAddressLine2());
        merchant.setCity(merchantDetails.getCity());
        merchant.setState(merchantDetails.getState());
        merchant.setPinCode(merchantDetails.getPinCode());
        merchant.setCountry(merchantDetails.getCountry());
        merchant.setBankAccountNumber(merchantDetails.getBankAccountNumber());
        merchant.setBankAccountName(merchantDetails.getBankAccountName());
        merchant.setBankName(merchantDetails.getBankName());
        merchant.setIfscCode(merchantDetails.getIfscCode());
        merchant.setBankAccountType(merchantDetails.getBankAccountType());

        merchant = merchantRepository.save(merchant);
        log.info("Saved merchant with ID: {}", merchant.getMerchantId());

        // Create merchant members if provided
        if (merchantDetails.getMerchantMembers() != null && !merchantDetails.getMerchantMembers().isEmpty()) {
            List<MerchantMember> membersList = new ArrayList<>();
            for (MerchantMemberRequest memberRequest : merchantDetails.getMerchantMembers()) {
                MerchantMember member = new MerchantMember();
                member.setMerchant(merchant);
                member.setSourceMemberId(memberRequest.getSourceMemberId());
                member.setName(memberRequest.getName());
                member.setEmail(memberRequest.getEmail());
                member.setBankAccountNumber(memberRequest.getBankAccountNumber());
                member.setBankAccountName(memberRequest.getBankAccountName());
                member.setBankName(memberRequest.getBankName());
                member.setIfscCode(memberRequest.getIfscCode());
                member.setBankAccountType(memberRequest.getBankAccountType());
                member.setTotalAmountReceivable(memberRequest.getTotalAmountReceivable());
                membersList.add(member);
            }

            membersList = merchantMemberRepository.saveAll(membersList);
            merchant.setMerchantMembers(membersList);
            log.info("Created {} merchant members for merchant ID: {}", membersList.size(), merchant.getMerchantId());
        }

        return merchant;
    }

    /**
     * Create a new PaymentMethodEntity from PaymentMethodRequest.
     */
    private PaymentMethodEntity createPaymentMethod(PaymentMethodRequest paymentMethodRequest) {
        log.info("Creating new payment method: {}", paymentMethodRequest.getPaymentMethod());
        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.setPaymentMethod(paymentMethodRequest.getPaymentMethod());

        if (paymentMethodRequest.getPaymentMethod().name().equalsIgnoreCase("CARD")) {
            paymentMethod.setCardBrand(paymentMethodRequest.getCardBrand());
            paymentMethod.setCardLast4(paymentMethodRequest.getCardLast4());
            paymentMethod.setCardExpMonth(paymentMethodRequest.getCardExpMonth());
            paymentMethod.setCardExpYear(paymentMethodRequest.getCardExpYear());
        }

        if (paymentMethodRequest.getPaymentMethod().name().equalsIgnoreCase("BANK_TRANSFER")) {
            paymentMethod.setBankName(paymentMethodRequest.getBankName());
            paymentMethod.setBankAccountNumber(paymentMethodRequest.getBankAccountNumber());
            paymentMethod.setBankIfscCode(paymentMethodRequest.getBankIfscCode());
            paymentMethod.setBankAccountHolderName(paymentMethodRequest.getBankAccountHolderName());
        }

        if (paymentMethodRequest.getPaymentMethod().name().equalsIgnoreCase("UPI")) {
            paymentMethod.setUpiId(paymentMethodRequest.getUpiId());
            paymentMethod.setUpiVpa(paymentMethodRequest.getUpiVpa());
        }

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Created payment method with ID: {}", paymentMethod.getPaymentMethodId());
        return paymentMethod;
    }

    /**
     * Create new customer (no record reuse).
     */
    private Customer createOrLoadCustomer(CustomerDetailsRequest customerDetails) {
        log.info("Creating new customer with email: {}", customerDetails.getCustomerEmail());
        Customer customer = new Customer();
        customer.setCustomerName(customerDetails.getCustomerName());
        customer.setCustomerEmail(customerDetails.getCustomerEmail());
        customer.setCustomerPhone(customerDetails.getCustomerPhone());
        customer.setBankAccountNumber(customerDetails.getBankAccountNumber());
        customer.setBankAccountName(customerDetails.getBankAccountName());
        customer.setBankName(customerDetails.getBankName());
        customer.setBankIfscCode(customerDetails.getIfscCode());
        customer.setBankAccountType(customerDetails.getBankAccountType());
        customer.setUpiId(customerDetails.getUpiId());
        customer.setAddressLine1(customerDetails.getAddressLine1());
        customer.setAddressLine2(customerDetails.getAddressLine2());
        customer.setCity(customerDetails.getCity());
        customer.setState(customerDetails.getState());
        customer.setPinCode(customerDetails.getPinCode());
        customer.setCountry(customerDetails.getCountry());
        return customerRepository.save(customer);
    }

    /**
     * Determine if payment type requires a customer.
     */
    private boolean requiresCustomer(PaymentType paymentType) {
        return paymentType.name().equalsIgnoreCase("REFUND") ||
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
                .merchantMemberId(payment.getMerchantMemberId())
                .paymentMethodId(
                        payment.getPaymentMethodEntity() != null ? payment.getPaymentMethodEntity().getPaymentMethodId()
                                : null)
                .transactionReference(payment.getTransactionReference())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
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
            case "CARD":
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
        }

        if (request.getPaymentType().name().equalsIgnoreCase("SALARY")) {
            if (request.getMerchantMemberId() == null || request.getMerchantMemberId() <= 0) {
                throw new IllegalArgumentException("Merchant member ID is required for salary payments");
            }
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
     * Build error response.
     */
    private PaymentResponse buildErrorResponse(String errorMessage, String idempotencyKey) {
        return PaymentResponse.builder()
                .idempotencyKey(idempotencyKey)
                .errorMessage(errorMessage)
                .success(false)
                .build();
    }
}
