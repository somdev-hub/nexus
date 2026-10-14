package com.nexus.pms.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.pms.annotation.LogActivity;
import com.nexus.pms.payload.MockPayInitiateRequest;
import com.nexus.pms.service.implementations.MockPaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/pms/mock-payments")
@RequiredArgsConstructor
@Slf4j
public class MockPaymentController {

    private final MockPaymentService mockPaymentService;

    @LogActivity("Initiate Mock Payment")
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(
            @Valid @RequestBody MockPayInitiateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            var res = mockPaymentService.initiate(request, idempotencyKey);
            boolean replay = Boolean.TRUE.equals(res.getIsIdempotentRetry());
            return ResponseEntity.status(replay ? HttpStatus.OK : HttpStatus.CREATED).body(res);
        } catch (IllegalArgumentException e) {
            log.warn("Mock payment validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @LogActivity("Get Mock Payment")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mockPaymentService.get(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @LogActivity("List Mock Payments")
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long invoiceId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(mockPaymentService.list(orgId, status, invoiceId, pageable));
    }

    @LogActivity("Get Mock Receipt")
    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> receipt(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mockPaymentService.receipt(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
