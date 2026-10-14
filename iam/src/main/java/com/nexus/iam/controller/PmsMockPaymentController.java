package com.nexus.iam.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.nexus.iam.annotation.LogActivity;
import com.nexus.iam.utils.CommonUtils;
import com.nexus.iam.utils.RestService;
import com.nexus.iam.utils.WebConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/iam/pms/mock-payments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PmsMockPaymentController {

    private final RestService restService;
    private final WebConstants webConstants;
    private final CommonUtils commonUtils;

    @LogActivity("Initiate Mock Payment via IAM")
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        if (idempotencyKey != null)
            headers.put("Idempotency-Key", idempotencyKey);
        return restService.iamRestCall(webConstants.getPmsMockPaymentsUrl() + "/initiate", body, headers,
                HttpMethod.POST, null);
    }

    @LogActivity("Get Mock Payment via IAM")
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        return restService.iamRestCall(webConstants.getPmsMockPaymentsUrl() + "/" + id, null, headers,
                HttpMethod.GET, null);
    }

    @LogActivity("List Mock Payments via IAM")
    @GetMapping
    public ResponseEntity<?> list(@RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long invoiceId,
            @PageableDefault(size = 20) Pageable pageable) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(webConstants.getPmsMockPaymentsUrl())
                .queryParam("page", pageable.getPageNumber())
                .queryParam("size", pageable.getPageSize());
        if (orgId != null)
            b.queryParam("orgId", orgId);
        if (status != null)
            b.queryParam("status", status);
        if (invoiceId != null)
            b.queryParam("invoiceId", invoiceId);
        return restService.iamRestCall(b.toUriString(), null, headers, HttpMethod.GET, null);
    }

    @LogActivity("Get Mock Receipt via IAM")
    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> receipt(@PathVariable Long id,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Organization-ID", required = false) String orgIdHeader) {
        Map<String, String> headers = commonUtils.buildJsonHeaders(authToken);
        if (orgIdHeader != null)
            headers.put("X-Organization-ID", orgIdHeader);
        return restService.iamRestCall(webConstants.getPmsMockPaymentsUrl() + "/" + id + "/receipt", null, headers,
                HttpMethod.GET, null);
    }
}
