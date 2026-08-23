package com.nexus.pms.controller;

import com.nexus.pms.annotation.LogActivity;
import com.nexus.pms.payload.ErrorResponseDto;
import com.nexus.pms.payload.PaymentRequest;
import com.nexus.pms.payload.PaymentResponse;
import com.nexus.pms.service.interfaces.IdempotencyService;
import com.nexus.pms.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST Controller for payment operations.
 * Unified endpoint for processing all payment types (salary, refund, invoice,
 * supply, etc.)
 * with idempotent payment processing.
 *
 * ENDPOINT: POST /api/payments
 * Handles all payment types based on paymentType field in request.
 *
 * For salary payments, set paymentType to SALARY and include merchantMemberId.
 * For other payments, set appropriate paymentType and include relevant IDs.
 *
 * SALARY PAYMENT EXAMPLE:
 * ========================
 *
 * Scenario: A merchant wants to pay salary to multiple employees in one batch.
 *
 * Flow 1: FIRST REQUEST (New Payment)
 * ------------------------------------
 * POST /api/payments
 * Header: Idempotency-Key: "550e8400-e29b-41d4-a716-446655440000"
 * Body: {
 * "amount": 50000,
 * "currency": "INR",
 * "description": "Monthly Salary - March 2026",
 * "paymentType": "SALARY",
 * "merchantId": 1,
 * "merchantMemberId": 101,
 * "paymentMethodId": 1,
 * "transactionReference": "SALARY_MARCH_2026_EMP101"
 * }
 *
 * Step 1: Check if Idempotency-Key header exists
 * Step 2: Check if payment with this key already exists in IdempotencyRecord
 * table
 * Step 3: Payment not found → Process new payment
 * Step 4: Validate payment details (amount > 0, merchant exists, etc.)
 * Step 5: Create Payment entity in database
 * Step 6: Calculate charges (2% fee + 18% tax)
 * Step 7: Call payment gateway (Razorpay)
 * Step 8: Update payment status to COMPLETED
 * Step 9: Store idempotency record linking the key to payment ID
 * Step 10: Return PaymentResponse with status 201
 *
 * Response (201 Created):
 * {
 * "paymentId": 1001,
 * "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
 * "paymentStatus": "COMPLETED",
 * "amount": 50000,
 * "currency": "INR",
 * "merchantMemberId": 101,
 * "razorpayPaymentId": "pay_1704067200000",
 * "grossAmount": 50000,
 * "netAmount": 48960,
 * "feeAmount": 1000,
 * "taxAmount": 40,
 * "success": true,
 * "createdAt": "2026-04-04T10:30:00.000Z"
 * }
 *
 * ---
 *
 * Flow 2: DUPLICATE REQUEST (Idempotent Response)
 * -----------------------------------------------
 * Same request sent again (e.g., due to network timeout or retry logic):
 *
 * POST /api/payments
 * Header: Idempotency-Key: "550e8400-e29b-41d4-a716-446655440000" <-- SAME KEY
 * Body: { ... same as above ... }
 *
 * Step 1: Check Idempotency-Key header
 * Step 2: Find record in IdempotencyRecord table with this key ✓ FOUND
 * Step 3: Retrieve associated payment ID (1001)
 * Step 4: Load Payment entity from database
 * Step 5: Convert to PaymentResponse
 * Step 6: Return SAME PaymentResponse with status 200 (not 201)
 * Step 7: Set isIdempotentRetry = true
 *
 * Response (200 OK):
 * {
 * "paymentId": 1001,
 * "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
 * "paymentStatus": "COMPLETED",
 * "amount": 50000,
 * "currency": "INR",
 * "merchantMemberId": 101,
 * "razorpayPaymentId": "pay_1704067200000",
 * "netAmount": 48960,
 * "isIdempotentRetry": true, <-- Flag indicates this is cached
 * "success": true
 * }
 *
 * IMPORTANT:
 * - Payment NOT processed again (employee not paid twice)
 * - No new Razorpay transaction created
 * - Database shows only 1 payment record
 * - IdempotencyRecord links the key to payment ID 1001
 * - Client gets consistent response
 *
 * ---
 *
 * Flow 3: DIFFERENT KEY (New Payment)
 * -----------------------------------
 * Same merchant wants to pay another employee:
 *
 * POST /api/payments
 * Header: Idempotency-Key: "660e8400-e29b-41d4-a716-446655440111" <-- DIFFERENT
 * KEY
 * Body: {
 * "amount": 45000,
 * "merchantMemberId": 102, <-- Different employee
 * ...
 * }
 *
 * Step 1: Check Idempotency-Key
 * Step 2: NOT found in IdempotencyRecord
 * Step 3: Process as new payment
 * Step 4-10: Same as Flow 1
 * Result: New payment created (Payment ID: 1002)
 *
 * ---
 *
 * BENEFITS:
 * =========
 * 1. Prevents double payment if client retries on network timeout
 * 2. Same response for retry - client thinks success happened
 * 3. Only one Razorpay transaction per idempotency key
 * 4. Different batch payments (different keys) processed independently
 * 5. No race condition - database constraint ensures idempotency key uniqueness
 * 6. Audit trail - can track which idempotency key led to which payment
 *
 * ---
 *
 * EDGE CASES HANDLED:
 * ===================
 * 1. Missing Idempotency-Key → Generate one automatically
 * 2. Invalid Idempotency-Key format → Reject with 400 Bad Request
 * 3. Duplicate by transaction reference → Detect and return existing payment
 * 4. Payment validation fails → Return 400 with error message
 * 5. Payment gateway fails → Return 500, don't store idempotency record
 * 6. Idempotency storage fails (after payment success) → Log warning, return
 * success anyway
 */
@RestController
@RequestMapping("/pms/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    /**
     * Process a payment (salary, refund, invoice, etc.).
     * Unified endpoint supporting all payment types with idempotent processing.
     *
     * Supported payment types:
     * - SALARY: Payment to employee (requires merchantMemberId)
     * - REFUND: Customer refund
     * - INVOICE: Invoice payment from customer
     * - SUPPLY: Supplier payment
     * - OTHER: Other payment types
     *
     * @param paymentRequest The payment request details
     * @param idempotencyKey The Idempotency-Key header (optional, will be generated
     *                       if missing)
     * @return PaymentResponse with payment details
     */
    @LogActivity("Process Payment")
    @PostMapping("/initiate")
    public ResponseEntity<?> processPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Determine if salary payment for better logging
        boolean isSalaryPayment = paymentRequest.getPaymentType() != null &&
                paymentRequest.getPaymentType().name().equalsIgnoreCase("SALARY");

        // Log payment request
        if (isSalaryPayment) {
            log.info("Received salary payment request - Merchant: {}, Amount: {}",
                    paymentRequest.getMerchant() != null ? paymentRequest.getMerchant().getMerchantOfficialEmail()
                            : "N/A",
                    paymentRequest.getAmount());
        } else {
            log.info("Received payment request - Type: {}, Amount: {}",
                    paymentRequest.getPaymentType(),
                    paymentRequest.getAmount());
        }

        // Set idempotency key from header
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            paymentRequest.setIdempotencyKey(idempotencyKey);
            log.debug("Using idempotency key from header: {}", idempotencyKey);
        }

        // Process payment with idempotency
        // Service now returns ResponseEntity<?> with appropriate status codes and
        // response bodies
        // No casting needed - just return directly from service
        ResponseEntity<?> response = paymentService.processPaymentWithIdempotency(paymentRequest);

        // Log response details
        if (response.getStatusCode().is2xxSuccessful()) {
            Object body = response.getBody();
            if (body instanceof PaymentResponse) {
                PaymentResponse paymentResponse = (PaymentResponse) body;
                if (isSalaryPayment) {
                    log.info("Salary payment processed successfully - Payment ID: {}",
                            paymentResponse.getPaymentId());
                } else {
                    log.info("Payment processed successfully - Payment ID: {}, Type: {}",
                            paymentResponse.getPaymentId(),
                            paymentRequest.getPaymentType());
                }
            }
        } else if (response.getStatusCode().is4xxClientError()) {
            Object body = response.getBody();
            String errorMsg = body instanceof ErrorResponseDto
                    ? ((ErrorResponseDto) body).getMessage()
                    : "Unknown error";
            log.error("Payment processing failed: {}", errorMsg);
        }

        // Return response as-is from service
        return response;
    }

    /**
     * Retrieve payment details by ID.
     *
     * @param paymentId The payment ID
     * @return PaymentResponse with payment details
     */
    @LogActivity("Get Payment Details")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        log.info("Fetching payment details - Payment ID: {}", paymentId);

        try {
            PaymentResponse response = paymentService.mapPaymentToResponse(
                    paymentService.getPaymentById(paymentId));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Payment not found - Payment ID: {}", paymentId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validate idempotency key format.
     * Useful for clients to validate before making request.
     *
     * @param idempotencyKey The idempotency key to validate
     * @return true if valid, false otherwise
     */
    @LogActivity("Validate Idempotency Key")
    @GetMapping("/validate/idempotency-key")
    public ResponseEntity<Boolean> validateIdempotencyKey(
            @RequestParam String idempotencyKey) {

        log.debug("Validating idempotency key: {}", idempotencyKey);
        boolean isValid = idempotencyService.isValidIdempotencyKey(idempotencyKey);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Generate a new idempotency key.
     * Useful for clients that don't want to generate their own.
     *
     * @return A new idempotency key (UUID format)
     */
    @LogActivity("Generate Idempotency Key")
    @PostMapping("/generate/idempotency-key")
    public ResponseEntity<String> generateIdempotencyKey() {
        String key = idempotencyService.generateIdempotencyKey();
        log.debug("Generated new idempotency key: {}", key);
        return ResponseEntity.ok(key);
    }
}
