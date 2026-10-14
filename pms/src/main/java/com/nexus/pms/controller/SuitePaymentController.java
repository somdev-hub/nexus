package com.nexus.pms.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.pms.annotation.LogActivity;
import com.nexus.pms.payload.SuiteIntentCreateRequest;
import com.nexus.pms.payload.SuitePayByRefRequest;
import com.nexus.pms.service.implementations.SuitePaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/pms/suite-payments")
@RequiredArgsConstructor
@Slf4j
public class SuitePaymentController {

    private final SuitePaymentService suiteService;

    @LogActivity("Create Suite Payment Intent")
    @PostMapping("/intents")
    public ResponseEntity<?> createIntent(
            @Valid @RequestBody SuiteIntentCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            var res = suiteService.createIntent(request, idempotencyKey);
            boolean replay = Boolean.TRUE.equals(res.getIsIdempotentRetry());
            return ResponseEntity.status(replay ? HttpStatus.OK : HttpStatus.CREATED).body(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @LogActivity("Get Suite Intent By Ref")
    @GetMapping("/intents/by-ref")
    public ResponseEntity<?> getByRef(@RequestParam String ref) {
        try {
            return ResponseEntity.ok(suiteService.getByRef(ref));
        } catch (IllegalArgumentException e) {
            String m = e.getMessage();
            HttpStatus st = "Invalid payment reference".equals(m) || "Payment session not found".equals(m)
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(st).body(java.util.Map.of("error", m));
        }
    }

    @LogActivity("Pay Suite Intent By Ref")
    @PostMapping("/intents/pay-by-ref")
    public ResponseEntity<?> payByRef(@Valid @RequestBody SuitePayByRefRequest request) {
        try {
            var res = suiteService.payByRef(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @LogActivity("Cancel Suite Intent")
    @PostMapping("/intents/by-ref/cancel")
    public ResponseEntity<?> cancel(@RequestBody java.util.Map<String, String> body) {
        try {
            return ResponseEntity.ok(suiteService.cancelByRef(body.get("ref")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @LogActivity("List Suite Intents")
    @GetMapping("/intents")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(suiteService.list(orgId, status, pageable));
    }
}
