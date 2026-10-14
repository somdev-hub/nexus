package com.nexus.pms.service.implementations;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexus.pms.annotation.LogActivity;
import com.nexus.pms.model.entities.MockPayment;
import com.nexus.pms.model.enums.MockPaymentMethod;
import com.nexus.pms.model.enums.MockPaymentStatus;
import com.nexus.pms.payload.MockPayInitiateRequest;
import com.nexus.pms.payload.MockPaymentResponse;
import com.nexus.pms.payload.MockReceiptResponse;
import com.nexus.pms.repository.MockPaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockPaymentService {

    private final MockPaymentRepository mockPaymentRepository;

    @Transactional
    @LogActivity("Initiate Mock Payment")
    public MockPaymentResponse initiate(MockPayInitiateRequest req, String headerKey) {
        String key = headerKey != null && !headerKey.isBlank() ? headerKey.trim()
                : (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank() ? req.getIdempotencyKey().trim()
                        : UUID.randomUUID().toString());

        // Idempotent replay
        var existing = mockPaymentRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            var r = toResponse(existing.get());
            r.setIsIdempotentRetry(true);
            r.setSuccess(r.getStatus().equals("COMPLETED"));
            return r;
        }

        if (req.getAmount() == null || req.getAmount() <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0");
        if (req.getPaymentMethod() == null)
            throw new IllegalArgumentException("Payment method is required");

        MockPayment p = new MockPayment();
        p.setIdempotencyKey(key);
        p.setTransactionReference("NEXUS-PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        p.setAmount(req.getAmount());
        p.setCurrency(req.getCurrency() != null ? req.getCurrency() : "INR");
        p.setDescription(req.getDescription() != null ? req.getDescription() : "Supply chain mock payment");
        p.setPaymentMethod(req.getPaymentMethod());
        p.setMethodDetailMasked(mask(req.getPaymentMethod(), req.getMethodDetail()));
        p.setPayerName(req.getPayerName());
        p.setPayerVpa(req.getPaymentMethod() == MockPaymentMethod.UPI ? req.getMethodDetail() : null);
        p.setInvoiceId(req.getInvoiceId());
        p.setInvoiceNumber(req.getInvoiceNumber());
        p.setPurchaseOrderId(req.getPurchaseOrderId());
        p.setOrgId(req.getOrgId());
        p.setPayerRole(req.getPayerRole());
        p.setStatus(MockPaymentStatus.PROCESSING);
        p = mockPaymentRepository.save(p);

        // Deterministic demo outcome: ...0002 / fail@mock => FAILED, 999999 => stuck PROCESSING
        boolean forceFail = isForceFail(req);
        boolean stuck = req.getAmount() != null && Math.abs(req.getAmount() - 999999.0) < 0.001;
        Long pid = p.getPaymentId();

        if (!stuck) {
            CompletableFuture.delayedExecutor(4, TimeUnit.SECONDS).execute(() -> settle(pid, forceFail));
        } else {
            log.info("Mock payment {} left in PROCESSING (demo stuck amount)", pid);
        }

        var r = toResponse(mockPaymentRepository.findById(pid).orElse(p));
        r.setIsIdempotentRetry(false);
        r.setSuccess(true);
        return r;
    }

    @Transactional
    public void settle(Long paymentId, boolean forceFail) {
        try {
            var opt = mockPaymentRepository.findById(paymentId);
            if (opt.isEmpty())
                return;
            MockPayment p = opt.get();
            if (p.getStatus() != MockPaymentStatus.PROCESSING && p.getStatus() != MockPaymentStatus.PENDING)
                return;
            if (forceFail) {
                p.setStatus(MockPaymentStatus.FAILED);
                p.setFailureCode("MOCK_DECLINED");
                p.setFailureReason("Mock gateway declined (test card ...0002 / fail@mock)");
            } else {
                p.setStatus(MockPaymentStatus.COMPLETED);
                p.setReceiptNo("RCT-" + System.currentTimeMillis() + "-" + String.format("%04d", (int) (Math.random() * 9999)));
                p.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            }
            mockPaymentRepository.save(p);
            log.info("Mock payment {} settled -> {}", paymentId, p.getStatus());
        } catch (Exception e) {
            log.error("Mock settle failed for {}", paymentId, e);
        }
    }

    @Transactional(readOnly = true)
    public MockPaymentResponse get(Long id) {
        var p = mockPaymentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Mock payment not found: " + id));
        var r = toResponse(p);
        r.setSuccess("COMPLETED".equals(r.getStatus()));
        return r;
    }

    @Transactional(readOnly = true)
    public Page<MockPaymentResponse> list(String orgId, String status, Long invoiceId, Pageable pageable) {
        Page<MockPayment> page;
        if (invoiceId != null) {
            var list = mockPaymentRepository.findByInvoiceIdOrderByPaymentIdDesc(invoiceId);
            var mapped = list.stream().map(this::toResponse).toList();
            return new org.springframework.data.domain.PageImpl<>(mapped, pageable, mapped.size());
        }
        if (orgId != null && status != null) {
            page = mockPaymentRepository.findByOrgIdAndStatus(orgId, MockPaymentStatus.valueOf(status), pageable);
        } else if (orgId != null) {
            page = mockPaymentRepository.findByOrgId(orgId, pageable);
        } else if (status != null) {
            page = mockPaymentRepository.findByStatus(MockPaymentStatus.valueOf(status), pageable);
        } else {
            page = mockPaymentRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MockReceiptResponse receipt(Long id) {
        var p = mockPaymentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Mock payment not found: " + id));
        if (p.getStatus() != MockPaymentStatus.COMPLETED)
            throw new IllegalStateException("Receipt available only after COMPLETED (current: " + p.getStatus() + ")");
        return MockReceiptResponse.builder()
                .receiptNo(p.getReceiptNo())
                .paymentId(p.getPaymentId())
                .transactionReference(p.getTransactionReference())
                .status(p.getStatus().name())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .feeAmount(0.0)
                .netAmount(p.getAmount())
                .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                .methodDetailMasked(p.getMethodDetailMasked())
                .payerName(p.getPayerName())
                .description(p.getDescription())
                .invoiceId(p.getInvoiceId())
                .invoiceNumber(p.getInvoiceNumber())
                .orgId(p.getOrgId())
                .payerRole(p.getPayerRole())
                .idempotencyKey(p.getIdempotencyKey())
                .paidAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private boolean isForceFail(MockPayInitiateRequest req) {
        if (req.getMethodDetail() == null)
            return false;
        String d = req.getMethodDetail().trim();
        return d.endsWith("0002") || d.equalsIgnoreCase("fail@mock");
    }

    private String mask(MockPaymentMethod method, String detail) {
        if (detail == null || detail.isBlank())
            return method != null ? method.name() : null;
        String d = detail.trim();
        if (method == MockPaymentMethod.CARD) {
            String digits = d.replaceAll("\\D", "");
            String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : "****";
            return "CARD **** " + last4;
        }
        if (method == MockPaymentMethod.UPI)
            return "UPI " + d;
        if (method == MockPaymentMethod.BANK_TRANSFER)
            return d.length() > 40 ? d.substring(0, 40) + "…" : d;
        return method.name() + " " + (d.length() > 24 ? d.substring(0, 24) + "…" : d);
    }

    private MockPaymentResponse toResponse(MockPayment p) {
        return MockPaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .idempotencyKey(p.getIdempotencyKey())
                .transactionReference(p.getTransactionReference())
                .receiptNo(p.getReceiptNo())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                .methodDetailMasked(p.getMethodDetailMasked())
                .invoiceId(p.getInvoiceId())
                .invoiceNumber(p.getInvoiceNumber())
                .orgId(p.getOrgId())
                .payerRole(p.getPayerRole())
                .failureCode(p.getFailureCode())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
