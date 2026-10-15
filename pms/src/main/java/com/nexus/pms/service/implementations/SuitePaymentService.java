package com.nexus.pms.service.implementations;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.pms.annotation.LogActivity;
import com.nexus.pms.model.entities.MockPayment;
import com.nexus.pms.model.entities.SuitePayment;
import com.nexus.pms.model.enums.MockPaymentMethod;
import com.nexus.pms.model.enums.MockPaymentStatus;
import com.nexus.pms.model.enums.SuitePaymentStatus;
import com.nexus.pms.payload.MockPayInitiateRequest;
import com.nexus.pms.payload.MockPaymentResponse;
import com.nexus.pms.payload.SuiteIntentCreateRequest;
import com.nexus.pms.payload.SuiteIntentResponse;
import com.nexus.pms.payload.SuitePayByRefRequest;
import com.nexus.pms.repository.MockPaymentRepository;
import com.nexus.pms.repository.SuitePaymentRepository;
import com.nexus.pms.util.SuiteRefCrypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuitePaymentService {

    private final SuitePaymentRepository suiteRepo;
    private final MockPaymentRepository mockRepo;
    private final MockPaymentService mockService;
    private final SuiteRefCrypto crypto;

    @Value("${suite.payments.ttl-minutes:30}")
    private long ttlMinutes;

    // ── Create intent (idempotent) ──────────────────────────────
    @Transactional
    @LogActivity("Create Suite Payment Intent")
    public SuiteIntentResponse createIntent(SuiteIntentCreateRequest req, String headerKey) {
        if (req.getAmount() == null || req.getAmount() <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");

        String base = headerKey != null && !headerKey.isBlank() ? headerKey.trim()
                : (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()
                        ? req.getIdempotencyKey().trim()
                        : defaultKey(req));

        // A fresh Pay after a CANCELLED/EXPIRED session must open a NEW
        // session — replaying the dead one would trap the user on a
        // "session cancelled" screen. Roll the key forward (-r2, -r3…)
        // until we land on a live intent or a free key. Live states
        // (INITIATED/PROCESSING/FAILED/COMPLETED) still replay.
        String key = base;
        for (int attempt = 1; attempt <= 10; attempt++) {
            var existing = suiteRepo.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                var current = refresh(existing.get());
                if (current.getStatus() == SuitePaymentStatus.CANCELLED
                        || current.getStatus() == SuitePaymentStatus.EXPIRED) {
                    key = base + "-r" + (attempt + 1);
                    continue;
                }
                var r = toResponse(current);
                r.setIsIdempotentRetry(true);
                return r;
            }
            break;
        }

        SuitePayment s = new SuitePayment();
        s.setIntentRef(UUID.randomUUID().toString());
        s.setIdempotencyKey(key);
        s.setAmount(req.getAmount());
        s.setCurrency(req.getCurrency() != null ? req.getCurrency() : "INR");
        s.setDescription(req.getDescription());
        s.setInvoiceId(req.getInvoiceId());
        s.setInvoiceNumber(req.getInvoiceNumber());
        s.setPurchaseOrderId(req.getPurchaseOrderId());
        s.setOrgId(req.getOrgId());
        s.setPayerRole(req.getPayerRole());
        s.setQuoteRef(req.getQuoteRef());
        s.setPartnershipId(req.getPartnershipId());
        s.setIntentKind(req.getQuoteRef() != null ? "LOGISTICS_SETTLEMENT" : "INVOICE");
        s.setStatus(SuitePaymentStatus.INITIATED);
        s.setExpiresAt(new Timestamp(System.currentTimeMillis() + ttlMinutes * 60 * 1000L));
        try {
            s = suiteRepo.save(s);
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // Lost a race with an identical concurrent create — return the winner.
            var winner = suiteRepo.findByIdempotencyKey(key).orElseThrow(() -> dup);
            var r = toResponse(refresh(winner));
            r.setIsIdempotentRetry(true);
            return r;
        }
        var r = toResponse(s);
        r.setIsIdempotentRetry(false);
        return r;
    }

    // ── Load by encrypted ref ───────────────────────────────────
    @Transactional
    public SuiteIntentResponse getByRef(String ref) {
        SuitePayment s = resolve(ref);
        return toResponse(refresh(s));
    }

    // ── Pay by encrypted ref (idempotent) ───────────────────────
    @Transactional
    @LogActivity("Pay Suite Intent")
    public SuiteIntentResponse payByRef(SuitePayByRefRequest req) {
        SuitePayment s = resolve(req.getRef());
        s = refresh(s);

        // Replay safe: terminal intents return their recorded outcome.
        if (s.getStatus() == SuitePaymentStatus.COMPLETED
                || s.getStatus() == SuitePaymentStatus.CANCELLED
                || s.getStatus() == SuitePaymentStatus.EXPIRED) {
            var r = toResponse(s);
            r.setIsIdempotentRetry(true);
            return r;
        }

        validatePayInput(req);

        // If a previous attempt is still settling, re-attach instead of double-charging.
        if (s.getMockPaymentId() != null) {
            var linked = mockRepo.findById(s.getMockPaymentId()).orElse(null);
            if (linked != null && (linked.getStatus() == MockPaymentStatus.PROCESSING
                    || linked.getStatus() == MockPaymentStatus.PENDING)) {
                s.setStatus(SuitePaymentStatus.PROCESSING);
                s = suiteRepo.save(s);
                var r = toResponse(s);
                r.setIsIdempotentRetry(true);
                return r;
            }
            if (linked != null && linked.getStatus() == MockPaymentStatus.COMPLETED) {
                return toResponse(syncFromMock(s, linked));
            }
            // Previous attempt FAILED → fall through and retry with a fresh mock key.
        }

        // Deterministic mock key per intent+attempt keeps retries single-flight.
        String mockKey = "suite-intent-" + s.getIntentId() + "-"
                + (s.getMockPaymentId() == null ? "a1" : "r" + System.currentTimeMillis());

        MockPayInitiateRequest m = MockPayInitiateRequest.builder()
                .amount(s.getAmount())
                .currency(s.getCurrency())
                .description(s.getDescription() != null ? s.getDescription()
                        : ("Suite pay invoice " + s.getInvoiceNumber()))
                .paymentMethod(req.getPaymentMethod())
                .methodDetail(displayDetail(req))
                .payerName(req.getPayerName())
                .invoiceId(s.getInvoiceId())
                .invoiceNumber(s.getInvoiceNumber())
                .purchaseOrderId(s.getPurchaseOrderId())
                .orgId(s.getOrgId())
                .payerRole(s.getPayerRole())
                .bankAccountNumber(req.getBankAccountNumber())
                .bankIfsc(req.getBankIfsc())
                .bankHolderName(req.getBankHolderName())
                .cardNumber(req.getCardNumber())
                .cardExpiry(req.getCardExpiry())
                .cardCvv(req.getCardCvv())
                .cardHolderName(req.getCardHolderName())
                .idempotencyKey(mockKey)
                .build();

        MockPaymentResponse mp = mockService.initiate(m, mockKey);
        s.setMockPaymentId(mp.getPaymentId());
        s.setStatus(SuitePaymentStatus.PROCESSING);
        s = suiteRepo.save(s);
        var r = toResponse(s);
        r.setIsIdempotentRetry(false);
        return r;
    }

    @Transactional
    public SuiteIntentResponse cancelByRef(String ref) {
        SuitePayment s = resolve(ref);
        s = refresh(s);
        if (s.getStatus() == SuitePaymentStatus.INITIATED || s.getStatus() == SuitePaymentStatus.FAILED) {
            s.setStatus(SuitePaymentStatus.CANCELLED);
            s = suiteRepo.save(s);
        }
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public Page<SuiteIntentResponse> list(String orgId, String status, Pageable pageable) {
        Page<SuitePayment> page;
        if (orgId != null && status != null)
            page = suiteRepo.findByOrgIdAndStatus(orgId, SuitePaymentStatus.valueOf(status), pageable);
        else if (orgId != null)
            page = suiteRepo.findByOrgId(orgId, pageable);
        else
            page = suiteRepo.findAll(pageable);
        return page.map(s -> toResponse(s));
    }

    // ── helpers ─────────────────────────────────────────────────
    private SuitePayment resolve(String ref) {
        if (ref == null || ref.isBlank())
            throw new IllegalArgumentException("Payment reference is required");
        String raw;
        try {
            // URL-decoded by Spring already; tolerate double encoding from manual links.
            raw = crypto.decrypt(ref.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment reference");
        }
        return suiteRepo.findByIntentRef(raw)
                .orElseThrow(() -> new IllegalArgumentException("Payment session not found"));
    }

    /** Lazy expiry + pull latest mock outcome into the intent. */
    private SuitePayment refresh(SuitePayment s) {
        if (s.getStatus() == SuitePaymentStatus.INITIATED
                && s.getExpiresAt() != null
                && s.getExpiresAt().before(new Timestamp(System.currentTimeMillis()))) {
            s.setStatus(SuitePaymentStatus.EXPIRED);
            return suiteRepo.save(s);
        }
        if (s.getMockPaymentId() != null
                && (s.getStatus() == SuitePaymentStatus.PROCESSING
                        || s.getStatus() == SuitePaymentStatus.INITIATED)) {
            var mp = mockRepo.findById(s.getMockPaymentId()).orElse(null);
            if (mp != null)
                return syncFromMock(s, mp);
        }
        return s;
    }

    private SuitePayment syncFromMock(SuitePayment s, MockPayment mp) {
        if (mp.getStatus() == MockPaymentStatus.COMPLETED) {
            s.setStatus(SuitePaymentStatus.COMPLETED);
            s.setCompletedAt(mp.getCompletedAt());
            s.setFailureReason(null);
        } else if (mp.getStatus() == MockPaymentStatus.FAILED) {
            s.setStatus(SuitePaymentStatus.FAILED);
            s.setFailureReason(mp.getFailureReason());
        } else {
            s.setStatus(SuitePaymentStatus.PROCESSING);
        }
        return suiteRepo.save(s);
    }

    private void validatePayInput(SuitePayByRefRequest req) {
        if (req.getPaymentMethod() == null)
            throw new IllegalArgumentException("Payment method is required");
        MockPaymentMethod m = req.getPaymentMethod();
        switch (m) {
            case UPI -> {
                String v = req.getMethodDetail();
                if (v == null || !v.trim().matches("^[\\w.\\-]{2,}@[a-zA-Z]{2,}$"))
                    throw new IllegalArgumentException("Enter a valid UPI ID (e.g. name@mock)");
            }
            case CARD -> {
                if (req.getCardHolderName() == null || req.getCardHolderName().trim().length() < 2)
                    throw new IllegalArgumentException("Card holder name is required");
                String digits = req.getCardNumber() == null ? "" : req.getCardNumber().replaceAll("\\D", "");
                if (digits.length() < 13 || digits.length() > 19 || !luhn(digits))
                    throw new IllegalArgumentException("Enter a valid card number");
                if (req.getCardExpiry() == null || !req.getCardExpiry().matches("^(0[1-9]|1[0-2])/\\d{2}$"))
                    throw new IllegalArgumentException("Expiry must be MM/YY");
                if (!isFutureExpiry(req.getCardExpiry()))
                    throw new IllegalArgumentException("Card is expired");
                if (req.getCardCvv() == null || !req.getCardCvv().matches("^\\d{3,4}$"))
                    throw new IllegalArgumentException("CVV must be 3–4 digits");
            }
            case BANK_TRANSFER -> {
                if (req.getBankHolderName() == null || req.getBankHolderName().trim().length() < 2)
                    throw new IllegalArgumentException("Account holder name is required");
                if (req.getBankAccountNumber() == null
                        || !req.getBankAccountNumber().replaceAll("\\s", "").matches("^\\d{9,18}$"))
                    throw new IllegalArgumentException("Account number must be 9–18 digits");
                if (req.getBankIfsc() == null
                        || !req.getBankIfsc().trim().toUpperCase().matches("^[A-Z]{4}0[A-Z0-9]{6}$"))
                    throw new IllegalArgumentException("Enter a valid IFSC (e.g. HDFC0001234)");
            }
            case NET_BANKING -> {
                if (req.getMethodDetail() == null || req.getMethodDetail().trim().length() < 2)
                    throw new IllegalArgumentException("Select your bank");
            }
            case WALLET -> {
                if (req.getMethodDetail() == null || req.getMethodDetail().trim().length() < 2)
                    throw new IllegalArgumentException("Enter wallet ID / linked phone");
            }
        }
    }

    private String displayDetail(SuitePayByRefRequest req) {
        return switch (req.getPaymentMethod()) {
            case UPI -> req.getMethodDetail();
            case CARD -> req.getCardNumber();
            case BANK_TRANSFER ->
                req.getBankHolderName() + " · " + maskAcct(req.getBankAccountNumber());
            case NET_BANKING, WALLET -> req.getMethodDetail();
        };
    }

    private String maskAcct(String a) {
        if (a == null)
            return "****";
        String d = a.replaceAll("\\s", "");
        return "****" + (d.length() >= 4 ? d.substring(d.length() - 4) : d);
    }

    private boolean luhn(String d) {
        int sum = 0;
        boolean alt = false;
        for (int i = d.length() - 1; i >= 0; i--) {
            int n = d.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9)
                    n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private boolean isFutureExpiry(String mmYY) {
        try {
            int mm = Integer.parseInt(mmYY.substring(0, 2));
            int yy = 2000 + Integer.parseInt(mmYY.substring(3, 5));
            java.time.YearMonth exp = java.time.YearMonth.of(yy, mm);
            return !exp.isBefore(java.time.YearMonth.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String defaultKey(SuiteIntentCreateRequest req) {
        if (req.getQuoteRef() != null)
            return "suite-quote-" + req.getQuoteRef();
        if (req.getInvoiceId() != null)
            return "suite-inv-" + req.getInvoiceId();
        return "suite-" + UUID.randomUUID();
    }

    private SuiteIntentResponse toResponse(SuitePayment s) {
        MockPaymentResponse mp = null;
        try {
            if (s.getMockPaymentId() != null)
                mp = mockRepo.findById(s.getMockPaymentId()).map(m -> MockPaymentResponse.builder()
                        .paymentId(m.getPaymentId())
                        .idempotencyKey(m.getIdempotencyKey())
                        .transactionReference(m.getTransactionReference())
                        .receiptNo(m.getReceiptNo())
                        .status(m.getStatus() != null ? m.getStatus().name() : null)
                        .amount(m.getAmount())
                        .currency(m.getCurrency())
                        .paymentMethod(m.getPaymentMethod() != null ? m.getPaymentMethod().name() : null)
                        .methodDetailMasked(m.getMethodDetailMasked())
                        .failureReason(m.getFailureReason())
                        .completedAt(m.getCompletedAt())
                        .build()).orElse(null);
        } catch (Exception e) {
            log.debug("mock lookup failed for intent {}", s.getIntentId());
        }
        return SuiteIntentResponse.builder()
                .intentId(s.getIntentId())
                .ref(crypto.encrypt(s.getIntentRef()))
                .idempotencyKey(s.getIdempotencyKey())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .invoiceId(s.getInvoiceId())
                .invoiceNumber(s.getInvoiceNumber())
                .purchaseOrderId(s.getPurchaseOrderId())
                .orgId(s.getOrgId())
                .payerRole(s.getPayerRole())
                .quoteRef(s.getQuoteRef())
                .partnershipId(s.getPartnershipId())
                .intentKind(s.getIntentKind())
                .amount(s.getAmount())
                .currency(s.getCurrency())
                .description(s.getDescription())
                .mockPaymentId(s.getMockPaymentId())
                .mockPayment(mp)
                .failureReason(s.getFailureReason() != null ? s.getFailureReason()
                        : (mp != null ? mp.getFailureReason() : null))
                .expiresAt(s.getExpiresAt())
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }
}
